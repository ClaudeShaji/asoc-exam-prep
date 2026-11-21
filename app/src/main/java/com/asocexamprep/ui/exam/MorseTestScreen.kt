package com.asocexamprep.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.morse.CopyScore
import com.asocexamprep.morse.Drills
import com.asocexamprep.morse.KeyDecoder
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.morse.Scoring
import com.asocexamprep.ui.morse.CopyField
import com.asocexamprep.ui.morse.KeyArea
import com.asocexamprep.ui.morse.recordSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class MorsePart { INTRO, RECEIVE, SEND, DONE }

/** Part II of the General exam: receive and send plain text at the exam speed; both must pass. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseTestScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val req = c.content.pack.grade("GENERAL")?.morse ?: return
    val timing = MorseTiming(req.wpm)
    val scope = rememberCoroutineScope()
    var part by remember { mutableStateOf(MorsePart.INTRO) }
    var rxText by remember { mutableStateOf("") }
    var rxTyped by remember { mutableStateOf("") }
    var rxPlaying by remember { mutableStateOf(false) }
    var rxScore by remember { mutableStateOf<CopyScore?>(null) }
    var txText by remember { mutableStateOf("") }
    var txScore by remember { mutableStateOf<CopyScore?>(null) }
    var decoded by remember { mutableStateOf("") }
    val decoder = remember(txText) { KeyDecoder(timing.ditMs) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    fun passed(sc: CopyScore?) = sc != null && sc.errors <= req.maxErrors

    Column(Modifier.fillMaxSize().testTag("screen_morse_test")) {
        TopAppBar(title = { Text("Morse code test") }, navigationIcon = { Back(onBack) })
        ScrollBody("morse_test_body") {
            when (part) {
                MorsePart.INTRO -> {
                    Text("General grade, Part II", style = MaterialTheme.typography.titleLarge)
                    Text("1. Receive: about ${req.characters} characters of plain text at ${req.wpm} WPM (${req.minutes} minutes). Type what you hear.")
                    Text("2. Send: key a passage of the same length on the on-screen key.")
                    Text("Each part passes with ${req.maxErrors} errors or fewer. Both parts must pass.")
                    Button(
                        onClick = {
                            rxText = Drills.examPassage(); txText = Drills.examPassage()
                            rxTyped = ""; rxScore = null; txScore = null; decoded = ""
                            startedAt = System.currentTimeMillis()
                            part = MorsePart.RECEIVE
                        },
                        modifier = Modifier.fillMaxWidth().testTag("morse_test_start"),
                    ) { Text("Start") }
                }
                MorsePart.RECEIVE -> {
                    Text("Part 1 · Receive", style = MaterialTheme.typography.titleLarge)
                    if (rxScore == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    job?.cancel(); rxPlaying = true
                                    job = scope.launch { try { c.morsePlayer.play(rxText, timing, s.toneHz) } finally { rxPlaying = false } }
                                },
                                enabled = !rxPlaying,
                                modifier = Modifier.testTag("rx_play"),
                            ) { Text(if (rxPlaying) "Playing…" else "Play the test") }
                            OutlinedButton(
                                onClick = {
                                    job?.cancel()
                                    val sc = Scoring.score(rxText, rxTyped); rxScore = sc
                                    scope.launch { recordSession(c, "exam-receive", timing, sc, startedAt) }
                                },
                                modifier = Modifier.testTag("rx_check"),
                            ) { Text("Finish") }
                        }
                        CopyField(rxTyped, { rxTyped = it }, enabled = true, tag = "rx_input")
                    } else {
                        PartResult("rx", rxScore!!, req.maxErrors, MorseCode.clean(rxText))
                        Button(onClick = { part = MorsePart.SEND; startedAt = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth().testTag("rx_next")) { Text("Part 2: Send") }
                    }
                }
                MorsePart.SEND -> {
                    Text("Part 2 · Send", style = MaterialTheme.typography.titleLarge)
                    Text("Key this passage. Leave a longer pause between words.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(txText, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("tx_text"))
                    Text("Decoded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(decoded.ifEmpty { " " }, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("tx_decoded"))
                    KeyArea(decoder, s.toneHz, "tx_key", onChange = { decoded = decoder.decoded })
                    Button(
                        onClick = {
                            decoder.flushLetter(); decoded = decoder.decoded
                            val sc = Scoring.score(txText, decoded); txScore = sc
                            val both = passed(rxScore) && passed(sc)
                            scope.launch {
                                recordSession(c, "exam-send", timing, sc, startedAt)
                                c.game.copyResult((rxScore!!.accuracy + sc.accuracy) / 2, examPassed = both)
                            }
                            part = MorsePart.DONE
                        },
                        modifier = Modifier.fillMaxWidth().testTag("tx_finish"),
                    ) { Text("Finish") }
                }
                MorsePart.DONE -> {
                    val both = passed(rxScore) && passed(txScore)
                    Text(if (both) "Pass" else "Not yet", style = MaterialTheme.typography.displaySmall,
                        color = if (both) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.testTag("morse_test_result"))
                    rxScore?.let { PartResult("rx", it, req.maxErrors, null) }
                    txScore?.let { PartResult("tx", it, req.maxErrors, null) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { part = MorsePart.INTRO }, modifier = Modifier.testTag("morse_test_again")) { Text("Try again") }
                        OutlinedButton(onClick = onBack) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartResult(tag: String, sc: CopyScore, maxErrors: Int, sent: String?) {
    val ok = sc.errors <= maxErrors
    Text(
        "${if (tag == "rx") "Receive" else "Send"}: ${sc.errors} errors · ${if (ok) "pass" else "fail"} (max $maxErrors)",
        style = MaterialTheme.typography.titleMedium,
        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("${tag}_result"),
    )
    sent?.let { Text("Sent: $it", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
}
