package com.asocexamprep.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.content.Question
import com.asocexamprep.data.settings.Grade
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.exam.Exam
import com.asocexamprep.exam.ExamScore
import com.asocexamprep.ui.screens.GradeSwitch
import com.asocexamprep.ui.screens.ScreenScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun Back(onBack: () -> Unit) =
    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }

@Composable
internal fun NavCard(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Which paper: the full written exam, or one section of it. */
enum class PaperKind(val key: String, val sections: Set<String>?, val title: String) {
    FULL("full", null, "Mock exam"),
    THEORY("A", setOf("A"), "Radio Theory paper"),
    RULES("B", setOf("B"), "Rules & Regulations paper");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: FULL
    }
}

private fun paperLabel(examId: String) = when (examId.substringAfter(':', "full")) {
    "A" -> "Theory"
    "B" -> "Rules"
    else -> "Full"
}

/** The Exam tab: papers, the Morse test, practice by topic, mistakes to review, past papers. */
@Composable
fun ExamScreen(onOpen: (String) -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val grade = pack.grade(s.grade.name)
    val mocks by c.db.progress().exams().collectAsStateWithLifecycle(emptyList())
    val mistakes by c.db.progress().openMistakes().collectAsStateWithLifecycle(emptyList())
    val due = mistakes.count { it.dueAt <= System.currentTimeMillis() }
    val morse = grade?.let { pack.grade("GENERAL")?.morse }

    ScreenScaffold("Exam", "exam") {
        GradeSwitch(tagPrefix = "exam_grade")
        grade?.let {
            Text(
                "${pack.exam.sections.sumOf { s -> s.questions }} questions · ${it.minutes / 60}h${if (it.minutes % 60 > 0) " ${it.minutes % 60}m" else ""} · pass: ${it.minSectionPercent}% in each section and ${it.minAggregatePercent}% overall",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("exam_rules"),
            )
        }
        mocks.firstOrNull()?.let { m ->
            Text(
                "Last paper (${paperLabel(m.examId)}): ${mockPercent(m.sectionScores)}% · ${if (m.passed) "Pass" else "Fail"}",
                style = MaterialTheme.typography.titleMedium,
                color = if (m.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("exam_last"),
            )
        }
        NavCard(Icons.Outlined.Timer, "Mock exam", "Sections A and B, timed and scored like the real paper", "exam_mock") { onOpen("mock/full") }
        NavCard(Icons.Outlined.Bolt, "Radio Theory", "Section A paper · 50 questions", "exam_paper_A") { onOpen("mock/A") }
        NavCard(Icons.Outlined.Gavel, "Rules & Regulations", "Section B paper · 50 questions", "exam_paper_B") { onOpen("mock/B") }
        NavCard(
            Icons.Outlined.GraphicEq, "Morse code test",
            (morse?.let { "Receive and send at ${it.wpm} WPM" } ?: "Receive and send") + if (s.grade == Grade.RESTRICTED) " · General grade only" else "",
            "exam_morse",
        ) { onOpen("morse") }
        NavCard(Icons.Outlined.Quiz, "Practice by topic", "${Exam.pool(pack, s.grade.name).size} questions · hints from the study notes", "exam_practice") { onOpen("topics") }
        NavCard(
            Icons.Outlined.Replay, "Review mistakes",
            when {
                mistakes.isEmpty() -> "No mistakes to review"
                due == 0 -> "${mistakes.size} waiting · none due today"
                else -> "$due due now · ${mistakes.size} in total"
            },
            "exam_review",
        ) { onOpen("review") }

        if (mocks.isNotEmpty()) {
            Text("Past papers", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            val fmt = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()) }
            mocks.take(8).forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("exam_history_${m.id}")) {
                    Text(fmt.format(Instant.ofEpochMilli(m.startedAt)), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(paperLabel(m.examId) + " · " + m.grade.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${mockPercent(m.sectionScores)}%", Modifier.padding(end = 12.dp))
                    Text(if (m.passed) "Pass" else "Fail", color = if (m.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

private fun mockPercent(json: String): Int = runCatching {
    val m = kotlinx.serialization.json.Json.decodeFromString<Map<String, List<Int>>>(json)
    val right = m.values.sumOf { it[0] }; val total = m.values.sumOf { it[1] }
    if (total == 0) 0 else Math.round(100f * right / total)
}.getOrDefault(0)

/** Choose a section or topic to practise. */
@Composable
fun PracticeTopicsScreen(onPick: (String) -> Unit, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val pool = remember(s.grade) { Exam.pool(pack, s.grade.name) }
    val counts = pool.groupingBy { it.topic }.eachCount()
    ScreenScaffold("Practice", "practice_topics", navigationIcon = { Back(onBack) }) {
        pack.exam.sections.forEach { sec ->
            val n = pool.count { it.section == sec.id }
            if (n == 0) return@forEach
            NavCard(Icons.Outlined.Quiz, "Section ${sec.id} · ${sec.title}", "10 random questions from $n", "practice_${sec.id}") { onPick(sec.id) }
            pack.unitsFor(s.grade.name).filter { it.section == sec.id }.flatMap { it.topics }.filter { (counts[it.id] ?: 0) > 0 }.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(t.id) }.testTag("practice_${t.id}").padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.title, Modifier.weight(1f))
                    Text("${counts[t.id]}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

/** Answer options; [reveal] colours the right one green and a wrong pick red, and stops changes. */
@Composable
internal fun Options(q: Question, chosen: Int?, reveal: Boolean, tagPrefix: String = "opt_", onChoose: (Int) -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val green = if (dark) androidx.compose.ui.graphics.Color(0xFF81C784) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
    val greenFill = if (dark) androidx.compose.ui.graphics.Color(0xFF1E4620) else androidx.compose.ui.graphics.Color(0xFFDCEFDC)
    val red = if (dark) androidx.compose.ui.graphics.Color(0xFFEF9A9A) else androidx.compose.ui.graphics.Color(0xFFC62828)
    val redFill = if (dark) androidx.compose.ui.graphics.Color(0xFF5C1F1F) else androidx.compose.ui.graphics.Color(0xFFF9DEDC)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.forEachIndexed { i, text ->
            val isRight = reveal && i == q.answer
            val isWrong = reveal && i == chosen && i != q.answer
            val border = when {
                isRight -> green
                isWrong -> red
                chosen == i -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val fill = when {
                isRight -> greenFill
                isWrong -> redFill
                chosen == i -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            // Stays enabled (full-contrast text); once revealed, taps are ignored.
            Card(
                onClick = { if (!reveal) onChoose(i) },
                border = BorderStroke(if (chosen == i || isRight) 2.dp else 1.dp, border),
                colors = CardDefaults.cardColors(containerColor = fill),
                modifier = Modifier.fillMaxWidth().testTag("$tagPrefix$i").semantics {
                    selected = chosen == i
                    if (reveal) stateDescription = when { isRight -> "Correct answer"; isWrong -> "Your answer, wrong"; else -> "" }
                },
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${'A' + i}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (isRight) Text("✓", color = green, fontWeight = FontWeight.Bold)
                    if (isWrong) Text("✗", color = red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Hint: the passage from the study notes that explains this question (copied verbatim, with its
 * source), plus the question source's own note if it has one. [onOpen] reports the first opening.
 */
@Composable
internal fun HintPanel(q: Question, tag: String, onTopic: (String) -> Unit, startOpen: Boolean = false, onOpen: () -> Unit = {}) {
    val pack = LocalContext.current.container.content.pack
    var open by rememberSaveable(q.id, startOpen) { mutableStateOf(startOpen) }
    val hint = pack.hints[q.id]
    if (!open) {
        TextButton(onClick = { open = true; onOpen() }, modifier = Modifier.testTag(tag)) {
            Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp))
            Text("  Hint")
        }
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("${tag}_panel"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hint != null) {
                val src = pack.source(hint.source)?.title ?: hint.source
                Text("From $src · ${hint.heading}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("${tag}_source"))
                // Table rows are kept exactly as in the notes; show their cells side by side.
                val text = if (hint.text.trimStart().startsWith("|")) hint.text.trim().trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("   ·   ") else hint.text
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("${tag}_text"))
            }
            q.explanation?.let { Text("Note in the source: $it", style = MaterialTheme.typography.bodyMedium) }
            if (hint == null && q.explanation == null) {
                Text("No hint for this question.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("${tag}_none"))
            }
            TextButton(onClick = { onTopic(q.topic) }) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(16.dp))
                Text("  Notes: ${pack.topic(q.topic)?.title}")
            }
        }
    }
}

@Composable
private fun Stem(q: Question, tag: String = "quiz_stem", number: Int? = null) {
    Text(
        (number?.let { "$it. " } ?: "") + q.stem,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(tag).semantics { stateDescription = q.id },
    )
}

/** Where the question and its answer come from: the published bank, and any confirming source. */
@Composable
private fun SourceLine(q: Question) {
    val pack = LocalContext.current.container.content.pack
    val parts = buildList {
        add((pack.source(q.source)?.title ?: q.source) + (q.page?.let { ", p. $it" } ?: ""))
        if (q.alsoIn.any { it.startsWith("rpc-") } && q.source != "rpc-asoc-b") add("same answer in the RPC Ham Radio Club paper")
        q.verified.firstOrNull()?.let { add("answer matches the official US question pool (${it.substringAfter(':')})") }
    }
    Text(
        "Source: " + parts.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("quiz_source"),
    )
}

/**
 * Practice or review: one question at a time with the answer shown at once.
 * [key] is a topic id, a section ("A"/"B"), or "review" for due mistakes.
 */
@Composable
fun QuizScreen(key: String, onTopic: (String) -> Unit, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val scope = rememberCoroutineScope()
    val review = key == "review"
    var questions by remember { mutableStateOf<List<Question>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    LaunchedEffect(round, s.grade) {
        questions = if (review) c.exams.dueMistakes(pack) else Exam.practice(pack, s.grade.name, key)
    }
    var i by remember(questions) { mutableIntStateOf(0) }
    var chosen by remember(questions, i) { mutableStateOf<Int?>(null) }
    var right by remember(questions) { mutableIntStateOf(0) }
    var shownAt by remember(questions, i) { mutableLongStateOf(System.currentTimeMillis()) }
    val title = when {
        review -> "Review mistakes"
        key.length == 1 -> "Section $key"
        else -> pack.topic(key)?.title ?: "Practice"
    }

    ScreenScaffold(title, "quiz", navigationIcon = { Back(onBack) }) {
        val qs = questions ?: return@ScreenScaffold
        if (qs.isEmpty()) {
            Text(if (review) "Nothing due. Mistakes come back here when they're due." else "No questions for this topic yet.", modifier = Modifier.testTag("quiz_empty"))
            return@ScreenScaffold
        }
        if (i >= qs.size) {
            Text("$right / ${qs.size}", style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("quiz_score"))
            Text(
                if (right == qs.size) "All correct." else "Wrong answers go to Review mistakes and come back until you get them right twice.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { round++ }, modifier = Modifier.testTag("quiz_again")) { Text(if (review) "Check again" else "Another 10") }
                OutlinedButton(onClick = onBack, modifier = Modifier.testTag("quiz_done")) { Text("Done") }
            }
            return@ScreenScaffold
        }
        val q = qs[i]
        LinearProgressIndicator(progress = { i.toFloat() / qs.size }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
        Text("${i + 1} of ${qs.size} · ${pack.topic(q.topic)?.title ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("quiz_count"))
        Stem(q)
        Options(q, chosen, reveal = chosen != null) { pick ->
            if (chosen != null) return@Options
            chosen = pick
            if (pick == q.answer) right++
            scope.launch { c.exams.answer(q, pick, if (review) "review" else "practice", System.currentTimeMillis() - shownAt) }
        }
        // Before answering the hint is on request; afterwards it opens to explain the answer.
        HintPanel(q, "quiz_hint", onTopic, startOpen = chosen != null)
        chosen?.let { pick ->
            val ok = pick == q.answer
            Text(
                if (ok) "Correct" else "The answer is ${'A' + q.answer}: ${q.options[q.answer]}",
                style = MaterialTheme.typography.titleMedium,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("quiz_feedback"),
            )
            SourceLine(q)
            Button(
                onClick = {
                    if (i + 1 >= qs.size) scope.launch { c.game.quizResult(right.toFloat() / qs.size) }
                    i++
                },
                modifier = Modifier.fillMaxWidth().testTag("quiz_next"),
            ) { Text(if (i + 1 >= qs.size) "Finish" else "Next") }
        }
    }
}

/**
 * A timed paper on one scrollable page: the full written exam or one section. With live results on,
 * each pick is marked right or wrong at once; hints are available and counted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperScreen(kindKey: String, onTopic: (String) -> Unit, onMorse: () -> Unit, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val kind = PaperKind.of(kindKey)
    val scope = rememberCoroutineScope()
    var paper by remember { mutableStateOf<List<Question>?>(null) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    val flagged = remember { mutableStateMapOf<String, Boolean>() }
    val hinted = remember { mutableStateMapOf<String, Boolean>() }
    var startedAt by remember { mutableLongStateOf(0L) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ExamScore?>(null) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val grade = pack.grade(s.grade.name)
    val running = paper != null && result == null
    val questionCount = pack.exam.sections.filter { kind.sections == null || it.id in kind.sections }.sumOf { it.questions }
    val minutes = grade?.let { Exam.minutesFor(pack, it, questionCount) } ?: 0
    val live = s.liveResults

    fun submit() {
        val p = paper ?: return
        val g = grade ?: return
        val sc = Exam.score(pack, g, p, answers)
        result = sc
        scope.launch {
            val id = c.exams.saveMock(g.id, sc, startedAt, "asoc-in:${kind.key}")
            p.forEach { q -> c.exams.answer(q, answers[q.id] ?: -1, "mock", examId = id) }
            c.game.mockResult(sc.percent / 100f, sc.passed && kind == PaperKind.FULL)
        }
    }

    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
            if (secondsLeft == 0) submit()
        }
    }
    BackHandler(enabled = running) { confirmLeave = true }

    Column(Modifier.fillMaxSize().testTag("screen_mock")) {
        TopAppBar(title = { Text(kind.title) }, navigationIcon = { Back { if (running) confirmLeave = true else onBack() } })
        val g = grade ?: return@Column
        val p = paper
        val r = result
        when {
            p == null -> ScrollBody("mock_intro") {
                Text("${g.title} grade", style = MaterialTheme.typography.titleLarge)
                pack.exam.sections.filter { kind.sections == null || it.id in kind.sections }.forEach { sec ->
                    Text("Section ${sec.id} · ${sec.title}: ${sec.questions} questions")
                }
                Text("Time: $minutes minutes")
                Text("Pass: at least ${g.minSectionPercent}% in each section and ${g.minAggregatePercent}% overall.")
                Text("All questions are on one page. Turn on live results to see each answer marked right or wrong as you go.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        paper = Exam.mock(pack, g.id, sections = kind.sections)
                        answers.clear(); flagged.clear(); hinted.clear()
                        startedAt = System.currentTimeMillis(); secondsLeft = minutes * 60
                    },
                    modifier = Modifier.fillMaxWidth().testTag("mock_start"),
                ) { Text("Start") }
            }
            r != null -> ScrollBody("mock_results") {
                MockResult(r, p, answers, hinted.size, onTopic, onDone = onBack, onAgain = { paper = null; result = null })
                if (kind == PaperKind.FULL && g.morse != null) {
                    Button(onClick = onMorse, modifier = Modifier.fillMaxWidth().testTag("mock_morse")) { Text("Part II: Morse code test") }
                }
            }
            else -> {
                // Header that stays put: time, progress, live results switch, submit.
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(" %d:%02d:%02d".format(secondsLeft / 3600, secondsLeft / 60 % 60, secondsLeft % 60), Modifier.testTag("mock_timer"))
                            Text("   ${answers.size} / ${p.size} answered", Modifier.weight(1f).testTag("mock_answered"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { confirmSubmit = true }, modifier = Modifier.testTag("mock_submit")) { Text("Submit") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Live results", Modifier.weight(1f))
                            Switch(checked = live, onCheckedChange = { on -> scope.launch { c.settings.setLiveResults(on) } }, modifier = Modifier.testTag("live_toggle"))
                        }
                    }
                }
                LazyColumn(
                    Modifier.fillMaxSize().testTag("paper_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(p, key = { _, q -> q.id }) { n, q ->
                        val sectionStart = n == 0 || p[n - 1].section != q.section
                        Column(Modifier.testTag("mq_$n"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sectionStart) {
                                Text(
                                    "Section ${q.section} · ${pack.exam.sections.first { it.id == q.section }.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Stem(q, tag = "mq_stem_$n", number = n + 1)
                            val chosen = answers[q.id]
                            Options(q, chosen, reveal = live && chosen != null, tagPrefix = "mq_${n}_opt_") { answers[q.id] = it }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = flagged[q.id] == true,
                                    onClick = { flagged[q.id] = flagged[q.id] != true },
                                    label = { Text("Flag") },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, null, Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("mq_flag_$n"),
                                )
                            }
                            // Opened hints stay open when the question scrolls off screen and back.
                            HintPanel(q, "mq_hint_$n", onTopic, startOpen = hinted[q.id] == true, onOpen = { hinted[q.id] = true })
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (confirmSubmit) {
        val missing = (paper?.size ?: 0) - answers.size
        val flags = flagged.count { it.value }
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit the paper?") },
            text = {
                Text(
                    listOfNotNull(
                        if (missing > 0) "$missing questions are unanswered and will count as wrong." else "All questions are answered.",
                        if (flags > 0) "$flags flagged to check." else null,
                    ).joinToString(" "),
                )
            },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; submit() }, modifier = Modifier.testTag("mock_confirm")) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Keep going") } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the paper?") },
            text = { Text("Your answers will be lost.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onBack() }, modifier = Modifier.testTag("mock_leave")) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
        )
    }
}

@Composable
private fun MockResult(r: ExamScore, paper: List<Question>, answers: Map<String, Int>, hints: Int, onTopic: (String) -> Unit, onDone: () -> Unit, onAgain: () -> Unit) {
    val pack = LocalContext.current.container.content.pack
    Text(
        if (r.passed) "Pass" else "Not yet",
        style = MaterialTheme.typography.displaySmall,
        color = if (r.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("mock_result"),
    )
    Text("${r.correct} / ${r.total} · ${r.percent}% (need ${r.grade.minAggregatePercent}%)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("mock_total"))
    r.sections.forEach { sec ->
        val ok = 100 * sec.correct >= r.grade.minSectionPercent * sec.total
        Text(
            "Section ${sec.section}: ${sec.correct} / ${sec.total} · ${sec.percent}% (need ${r.grade.minSectionPercent}%)",
            color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("mock_section_${sec.section}"),
        )
    }
    Text(if (hints == 0) "No hints used" else "Hints used: $hints", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("mock_hints"))
    val wrong = paper.filter { answers[it.id] != it.answer }
    if (wrong.isNotEmpty()) {
        val weakTopics = wrong.groupingBy { it.topic }.eachCount().entries.sortedByDescending { it.value }.take(4)
        Text("Study next", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        weakTopics.forEach { (t, n) ->
            Row(Modifier.fillMaxWidth().clickable { onTopic(t) }.padding(vertical = 8.dp).testTag("mock_weak_$t")) {
                Text(pack.topic(t)?.title ?: t, Modifier.weight(1f))
                Text("$n wrong", color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Wrong answers (${wrong.size}) — added to Review mistakes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        wrong.take(30).forEach { q ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(q.stem, style = MaterialTheme.typography.bodyMedium)
                answers[q.id]?.let { Text("You: ${q.options[it]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    ?: Text("Not answered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Text("Answer: ${q.options[q.answer]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAgain, modifier = Modifier.testTag("mock_again")) { Text("New paper") }
        OutlinedButton(onClick = onDone, modifier = Modifier.testTag("mock_done")) { Text("Done") }
    }
}

/** Scrolling page body under a screen's own top bar. */
@Composable
internal fun ScrollBody(tag: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().testTag(tag).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}
