package com.asocexamprep.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
private fun Back(onBack: () -> Unit) =
    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }

@Composable
private fun NavCard(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
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

/** The Exam tab: mock exam, practice by topic, mistakes to review, past mocks. */
@Composable
fun ExamScreen(onOpen: (String) -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val grade = pack.grade(s.grade.name)
    val mocks by c.db.progress().exams().collectAsStateWithLifecycle(emptyList())
    val mistakes by c.db.progress().openMistakes().collectAsStateWithLifecycle(emptyList())
    val due = mistakes.count { it.dueAt <= System.currentTimeMillis() }

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
                "Last mock: ${mockPercent(m.sectionScores)}% · ${if (m.passed) "Pass" else "Fail"}",
                style = MaterialTheme.typography.titleMedium,
                color = if (m.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("exam_last"),
            )
        }
        NavCard(Icons.Outlined.Timer, "Mock exam", "Full timed paper, scored like the real one", "exam_mock") { onOpen("mock") }
        NavCard(Icons.Outlined.Quiz, "Practice by topic", "${Exam.pool(pack, s.grade.name).size} questions · answers explained", "exam_practice") { onOpen("topics") }
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
            Text("Past mocks", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            val fmt = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()) }
            mocks.take(8).forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("exam_history_${m.id}")) {
                    Text(fmt.format(Instant.ofEpochMilli(m.startedAt)), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(m.grade.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Answer options; [reveal] colours the right one and the learner's wrong pick. */
@Composable
private fun Options(q: Question, chosen: Int?, reveal: Boolean, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.forEachIndexed { i, text ->
            val isRight = reveal && i == q.answer
            val isWrong = reveal && i == chosen && i != q.answer
            val border = when {
                isRight -> MaterialTheme.colorScheme.primary
                isWrong -> MaterialTheme.colorScheme.error
                chosen == i -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Card(
                onClick = { onChoose(i) },
                enabled = !reveal,
                border = BorderStroke(if (chosen == i || isRight) 2.dp else 1.dp, border),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isRight -> MaterialTheme.colorScheme.primaryContainer
                        isWrong -> MaterialTheme.colorScheme.errorContainer
                        chosen == i -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    disabledContainerColor = when {
                        isRight -> MaterialTheme.colorScheme.primaryContainer
                        isWrong -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                ),
                modifier = Modifier.fillMaxWidth().testTag("opt_$i").semantics {
                    selected = chosen == i
                    if (reveal) stateDescription = when { isRight -> "Correct answer"; isWrong -> "Your answer, wrong"; else -> "" }
                },
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${'A' + i}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun Stem(q: Question) {
    Text(q.stem, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("quiz_stem").semantics { stateDescription = q.id })
}

/**
 * Practice or review: one question at a time with the answer and explanation shown at once.
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
        chosen?.let { pick ->
            val ok = pick == q.answer
            Text(
                if (ok) "Correct" else "The answer is ${'A' + q.answer}: ${q.options[q.answer]}",
                style = MaterialTheme.typography.titleMedium,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("quiz_feedback"),
            )
            q.explanation?.let { Text(it, modifier = Modifier.testTag("quiz_explanation")) }
            TextButton(onClick = { onTopic(q.topic) }, modifier = Modifier.testTag("quiz_notes")) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp))
                Text("  Notes: ${pack.topic(q.topic)?.title}")
            }
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

/** A full timed mock exam, scored by the grade's pass rules at the end. */
@Composable
fun MockScreen(onTopic: (String) -> Unit, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val pack = c.content.pack
    val scope = rememberCoroutineScope()
    var paper by remember { mutableStateOf<List<Question>?>(null) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    val flagged = remember { mutableStateMapOf<String, Boolean>() }
    var i by remember { mutableIntStateOf(0) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ExamScore?>(null) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val grade = pack.grade(s.grade.name)
    val running = paper != null && result == null

    fun submit() {
        val p = paper ?: return
        val g = grade ?: return
        val sc = Exam.score(pack, g, p, answers)
        result = sc
        scope.launch {
            val id = c.exams.saveMock(g.id, sc, startedAt)
            p.forEach { q -> answers[q.id]?.let { c.exams.answer(q, it, "mock", examId = id) } ?: c.exams.answer(q, -1, "mock", examId = id) }
            c.game.mockResult(sc.percent / 100f, sc.passed)
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

    ScreenScaffold("Mock exam", "mock", navigationIcon = { Back { if (running) confirmLeave = true else onBack() } }) {
        val g = grade ?: return@ScreenScaffold
        val p = paper
        val r = result
        when {
            p == null -> {
                Text("${g.title} grade", style = MaterialTheme.typography.titleLarge)
                pack.exam.sections.forEach { sec -> Text("Section ${sec.id} · ${sec.title}: ${sec.questions} questions") }
                Text("Time: ${g.minutes} minutes")
                Text("Pass: at least ${g.minSectionPercent}% in each section and ${g.minAggregatePercent}% overall.")
                Text("Answers are shown at the end. You can move between questions and flag ones to check.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        paper = Exam.mock(pack, g.id)
                        answers.clear(); flagged.clear(); i = 0
                        startedAt = System.currentTimeMillis(); secondsLeft = g.minutes * 60
                    },
                    modifier = Modifier.fillMaxWidth().testTag("mock_start"),
                ) { Text("Start") }
            }
            r != null -> MockResult(r, p, answers, onTopic, onDone = onBack, onAgain = { paper = null; result = null })
            else -> {
                val q = p[i]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1} / ${p.size} · Section ${q.section}", Modifier.weight(1f).testTag("mock_count"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" %d:%02d:%02d".format(secondsLeft / 3600, secondsLeft / 60 % 60, secondsLeft % 60), modifier = Modifier.testTag("mock_timer"))
                }
                Navigator(p, i, answers, flagged) { i = it }
                Stem(q)
                Options(q, answers[q.id], reveal = false) { answers[q.id] = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (i > 0) i-- }, enabled = i > 0, modifier = Modifier.testTag("mock_prev")) { Text("Back") }
                    FilterChip(
                        selected = flagged[q.id] == true,
                        onClick = { flagged[q.id] = flagged[q.id] != true },
                        label = { Text("Flag") },
                        leadingIcon = { Icon(Icons.Outlined.Flag, null, Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("mock_flag"),
                    )
                    if (i + 1 < p.size) Button(onClick = { i++ }, modifier = Modifier.testTag("mock_next")) { Text("Next") }
                }
                Text("${answers.size} of ${p.size} answered", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { confirmSubmit = true }, modifier = Modifier.fillMaxWidth().testTag("mock_submit")) { Text("Submit") }
            }
        }
    }

    if (confirmSubmit) {
        val missing = (paper?.size ?: 0) - answers.size
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit the paper?") },
            text = { Text(if (missing > 0) "$missing questions are unanswered and will count as wrong." else "All questions are answered.") },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; submit() }, modifier = Modifier.testTag("mock_confirm")) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Keep going") } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the mock exam?") },
            text = { Text("Your answers will be lost.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onBack() }, modifier = Modifier.testTag("mock_leave")) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
        )
    }
}

/** Numbered strip of all questions: filled = answered, amber ring = flagged. */
@Composable
private fun Navigator(paper: List<Question>, current: Int, answers: Map<String, Int>, flagged: Map<String, Boolean>, onGo: (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(current) { state.animateScrollToItem((current - 3).coerceAtLeast(0)) }
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().testTag("mock_nav")) {
        items(paper.size) { n ->
            val q = paper[n]
            val answered = answers.containsKey(q.id)
            Box(
                Modifier.size(34.dp)
                    .background(
                        when {
                            n == current -> MaterialTheme.colorScheme.primary
                            answered -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        CircleShape,
                    )
                    .then(if (flagged[q.id] == true) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape) else Modifier)
                    .clickable { onGo(n) }
                    .testTag("nav_$n"),
                contentAlignment = Alignment.Center,
            ) {
                Text("${n + 1}", style = MaterialTheme.typography.labelMedium, color = if (n == current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MockResult(r: ExamScore, paper: List<Question>, answers: Map<String, Int>, onTopic: (String) -> Unit, onDone: () -> Unit, onAgain: () -> Unit) {
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
    // Weakest topics first, so the learner knows what to study.
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
