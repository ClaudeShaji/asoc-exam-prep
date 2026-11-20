package com.asocexamprep.ui.morse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.asocexamprep.morse.Stage
import com.asocexamprep.morse.Step
import com.asocexamprep.morse.Trainer
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.morse.CopyMode
import com.asocexamprep.morse.CopyScore
import com.asocexamprep.morse.Drills
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.morse.Scoring
import com.asocexamprep.ui.screens.ScreenScaffold
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class Phase { READY, PLAYING, HEARD, SCORED }

/** Shared listen-type-check flow. [makeText] builds a new piece when the learner starts. */
private class CopyRound {
    var phase by mutableStateOf(Phase.READY)
    var sent by mutableStateOf("")
    var typed by mutableStateOf("")
    var score by mutableStateOf<CopyScore?>(null)
    var startedAt by mutableLongStateOf(0L)
    var job: Job? = null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val current = s.trainerStep
    // -1 follows the learner's current step; otherwise a pinned (earlier or just-passed) step.
    var pinned by rememberSaveable { mutableIntStateOf(-1) }
    var unlocked by remember { mutableStateOf<String?>(null) }
    val step = Trainer.step(if (pinned < 0) current else pinned)
    val data = c.content.pack.training

    fun onDone(accuracy: Float) {
        if (Trainer.passed(accuracy) && step.index == current && current < Trainer.steps.lastIndex) {
            pinned = step.index
            unlocked = Trainer.step(current + 1).let { n -> n.char?.let { ch -> "${n.label}: ${MorseCode.prosign[ch] ?: ch}" } ?: n.label }
            scope.launch { c.settings.setTrainerStep(current + 1) }
        }
    }

    ScreenScaffold("Learn", "learn", navigationIcon = { MorseBack(onBack) }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stage.entries.forEach { st ->
                val stageSteps = Trainer.steps.filter { it.stage == st }
                val open = stageSteps.first().index <= current
                FilterChip(
                    selected = step.stage == st,
                    enabled = open,
                    onClick = {
                        unlocked = null
                        pinned = if (current in stageSteps.first().index..stageSteps.last().index) -1 else stageSteps.last().index
                    },
                    label = { Text(st.label) },
                    modifier = Modifier.testTag("learn_stage_${st.name}"),
                )
            }
        }
        LinearProgressIndicator(
            progress = { current.toFloat() / Trainer.steps.lastIndex },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        Text(step.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("learn_step"))
        if (step.index < current && pinned >= 0 && unlocked == null) {
            Text("Review", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val timing = MorseTiming(s.charWpm, s.effectiveWpm)
        key(step.index) {
            if (step.stage == Stage.CHARACTERS) {
                CharDrill(step, timing, s.toneHz, ::onDone)
            } else if (data != null) {
                val round = remember { CopyRound() }
                DisposableEffect(Unit) { onDispose { round.job?.cancel() } }
                CopyFlow(
                    round = round,
                    tag = "learn",
                    start = { unlocked = null; Trainer.text(step, data) },
                    play = { text -> c.morsePlayer.play(text, timing, s.toneHz) },
                    onScored = { score ->
                        recordSession(c, "learn", timing, score, round.startedAt, step.index)
                        onDone(score.accuracy)
                    },
                    resultExtra = { score -> if (Trainer.passed(score.accuracy)) null else "90% unlocks the next step" },
                )
            }
        }
        unlocked?.let {
            Text("Unlocked: $it", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("learn_unlocked"))
            Button(onClick = { unlocked = null; pinned = -1 }, modifier = Modifier.testTag("learn_continue")) { Text("Continue") }
        }
    }
}

/** One character at a time: hear it, type it, see right or wrong at once. */
@Composable
private fun CharDrill(step: Step, timing: MorseTiming, toneHz: Int, onDone: (Float) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val ch = step.char!!
    var prompts by remember { mutableStateOf(emptyList<Char>()) }
    val answers = remember { mutableStateListOf<Char>() }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    var input by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(0L) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    val i = answers.size
    val running = prompts.isNotEmpty() && i < prompts.size
    val done = prompts.isNotEmpty() && i >= prompts.size

    fun play(x: Char) { job?.cancel(); job = scope.launch { c.morsePlayer.play("$x", timing, toneHz) } }

    fun answer(typed: Char) {
        if (!running || feedback != null) return
        val ok = typed.uppercaseChar() == prompts[i]
        feedback = ok
        scope.launch {
            delay(if (ok) 500 else 1200)
            answers += typed.uppercaseChar()
            feedback = null
            input = ""
            if (answers.size < prompts.size) play(prompts[answers.size]) else {
                val score = Scoring.score(prompts.joinToString(""), answers.joinToString(""))
                recordSession(c, "learn", timing, score, startedAt, step.index)
                onDone(score.accuracy)
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(MorseCode.prosign[ch] ?: "$ch", style = MaterialTheme.typography.displayMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("char_new"))
        MorsePattern("$ch", dot = 8.dp)
    }
    if (!running) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { job?.cancel(); job = scope.launch { c.morsePlayer.play("$ch $ch $ch", timing, toneHz) } }, modifier = Modifier.testTag("char_hear")) { Text("Hear it") }
            Button(
                onClick = {
                    prompts = Trainer.charPrompts(step); answers.clear(); feedback = null; input = ""
                    startedAt = System.currentTimeMillis()
                    play(prompts[0])
                },
                modifier = Modifier.testTag("char_start"),
            ) { Text(if (done) "Again" else "Start") }
        }
    }
    if (running) {
        Text("${i + 1} of ${prompts.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("char_count"))
        OutlinedTextField(
            value = input,
            onValueChange = { v ->
                if (feedback == null) {
                    val ch = v.lastOrNull { !it.isWhitespace() }
                    input = ch?.uppercase() ?: ""
                    ch?.let(::answer)
                }
            },
            label = { Text("Type what you hear") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth().testTag("char_input"),
        )
        OutlinedButton(onClick = { play(prompts[i]) }, modifier = Modifier.testTag("char_replay")) { Text("Replay") }
        when (feedback) {
            true -> Text("✓", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("char_feedback"))
            false -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("char_feedback")) {
                Text("It was ${MorseCode.prosign[prompts[i]] ?: prompts[i]}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                MorsePattern("${prompts[i]}", dot = 6.dp, color = MaterialTheme.colorScheme.error)
            }
            null -> {}
        }
    }
    if (done) {
        val right = prompts.indices.count { answers[it] == prompts[it] }
        Text("$right / ${prompts.size}", style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("char_score"))
        if (!Trainer.passed(right.toFloat() / prompts.size)) Text("9 right unlocks the next character", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CopyScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val round = remember { CopyRound() }
    var mode by rememberSaveable { mutableStateOf(CopyMode.LETTERS) }
    DisposableEffect(Unit) { onDispose { round.job?.cancel() } }
    val exam = c.content.pack.grade("GENERAL")?.morse
    val timing = if (mode == CopyMode.EXAM) MorseTiming(exam?.wpm ?: 8) else MorseTiming(s.charWpm, s.effectiveWpm)
    val qcodes = remember { c.content.pack.reference.qcodes.map { it.code } }

    ScreenScaffold("Copy practice", "copy", navigationIcon = { MorseBack(onBack) }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { round.job?.cancel(); round.phase = Phase.READY; round.typed = ""; mode = m },
                    label = { Text(m.label) },
                    modifier = Modifier.testTag("copy_mode_${m.name}"),
                )
            }
        }
        Text(
            if (mode == CopyMode.EXAM) "${exam?.wpm ?: 8} WPM plain text, about ${exam?.characters ?: 200} characters. Pass: ${exam?.maxErrors ?: 5} errors or fewer."
            else "${timing.charWpm}/${timing.effectiveWpm} WPM",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CopyFlow(
            round = round,
            tag = "copy",
            start = { Drills.text(mode, qcodes, Trainer.learnedChars(maxOf(s.trainerStep, 9))) },
            play = { text -> c.morsePlayer.play(text, timing, s.toneHz) },
            onScored = { score -> recordSession(c, "copy-${mode.name.lowercase()}", timing, score, round.startedAt) },
            resultExtra = { score ->
                if (mode == CopyMode.EXAM) (if (score.errors <= (exam?.maxErrors ?: 5)) "Pass" else "Fail") else null
            },
        )
    }
}

@Composable
private fun CopyFlow(
    round: CopyRound,
    tag: String,
    start: () -> String,
    play: suspend (String) -> Unit,
    onScored: suspend (CopyScore) -> Unit,
    resultExtra: (CopyScore) -> String?,
) {
    val scope = rememberCoroutineScope()
    fun listen(text: String) {
        round.job?.cancel()
        round.phase = Phase.PLAYING
        round.job = scope.launch {
            try { play(text) } finally { if (round.phase == Phase.PLAYING) round.phase = Phase.HEARD }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (round.phase) {
            Phase.READY, Phase.SCORED -> Button(
                onClick = {
                    round.sent = start(); round.typed = ""; round.score = null
                    round.startedAt = System.currentTimeMillis()
                    listen(round.sent)
                },
                modifier = Modifier.testTag("${tag}_start"),
            ) { Text(if (round.phase == Phase.READY) "Start" else "Next") }
            Phase.PLAYING -> OutlinedButton(onClick = { round.job?.cancel(); round.phase = Phase.HEARD }, modifier = Modifier.testTag("${tag}_stop")) { Text("Stop") }
            Phase.HEARD -> {
                Button(
                    onClick = {
                        val sc = Scoring.score(round.sent, round.typed)
                        round.score = sc
                        round.phase = Phase.SCORED
                        scope.launch { onScored(sc) }
                    },
                    modifier = Modifier.testTag("${tag}_check"),
                ) { Text("Check") }
                OutlinedButton(onClick = { listen(round.sent) }, modifier = Modifier.testTag("${tag}_replay")) { Text("Replay") }
            }
        }
    }
    if (round.phase != Phase.READY) {
        CopyField(round.typed, { round.typed = it }, enabled = round.phase != Phase.SCORED, tag = "${tag}_input")
    }
    if (round.phase == Phase.PLAYING) Text("Listening…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("${tag}_listening"))
    round.score?.let { ScoreView(it, MorseCode.clean(round.sent), tag, resultExtra(it)) }
}
