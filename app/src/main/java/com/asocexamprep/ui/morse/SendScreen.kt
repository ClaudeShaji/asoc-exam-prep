package com.asocexamprep.ui.morse

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.morse.Drills
import com.asocexamprep.morse.KeyDecoder
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.ui.screens.ScreenScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SendScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(Drills.sendTarget()) }
    val decoder = remember(s.charWpm) { KeyDecoder(1200.0 / s.charWpm) }
    var decoded by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    var wpm by remember { mutableIntStateOf(s.charWpm) }
    var down by remember { mutableStateOf(false) }
    var lastUp by remember { mutableLongStateOf(-1L) }
    val startedAt = remember { System.currentTimeMillis() }
    DisposableEffect(Unit) { onDispose { c.morsePlayer.keyUp() } }

    fun refresh() { decoded = decoder.decoded; pending = decoder.pending; wpm = decoder.wpm }

    // A pause of three dits ends the letter.
    LaunchedEffect(lastUp) {
        if (lastUp < 0) return@LaunchedEffect
        delay((3 * decoder.ditMs).toLong())
        decoder.flushLetter(); refresh()
    }

    val matched = decoded.trim() == target
    LaunchedEffect(matched) {
        if (matched) {
            recordSession(c, "send", MorseTiming(wpm.coerceIn(3, 60)), null, startedAt)
            c.game.sendCorrect()
        }
    }

    ScreenScaffold("Send practice", "send", navigationIcon = { MorseBack(onBack) }) {
        Text("Send", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(target, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("send_target"))
        MorsePattern(target, dot = 7.dp)
        Text("Decoded", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            decoded.ifEmpty { " " },
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("send_decoded"),
        )
        Box(Modifier.height(10.dp)) {
            if (pending.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pending.forEach { s ->
                    Box(Modifier.size(if (s == '.') 10.dp else 30.dp, 10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)))
                }
            }
        }
        Text("About $wpm WPM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (matched) Text("Correct", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("send_match"))

        var pressedAt by remember { mutableLongStateOf(0L) }
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (down) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .testTag("send_key")
                .pointerInput(s.toneHz) {
                    awaitEachGesture {
                        val d = awaitFirstDown()
                        pressedAt = d.uptimeMillis
                        down = true
                        c.morsePlayer.keyDown(s.toneHz)
                        val up = waitForUpOrCancellation()
                        c.morsePlayer.keyUp()
                        down = false
                        val upAt = up?.uptimeMillis ?: pressedAt
                        val gap = if (lastUp >= 0) (pressedAt - lastUp).toDouble() else null
                        decoder.mark((upAt - pressedAt).toDouble(), gap)
                        lastUp = upAt
                        refresh()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "KEY",
                style = MaterialTheme.typography.titleLarge,
                color = if (down) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { decoder.clear(); lastUp = -1; refresh() }, modifier = Modifier.testTag("send_clear")) { Text("Clear") }
            OutlinedButton(
                onClick = { target = Drills.sendTarget(); decoder.clear(); lastUp = -1; refresh() },
                modifier = Modifier.testTag("send_next"),
            ) { Text("New") }
            OutlinedButton(
                onClick = { scope.launch { c.morsePlayer.play(target, MorseTiming(s.charWpm, s.effectiveWpm), s.toneHz) } },
                modifier = Modifier.testTag("send_hear"),
            ) { Text("Hear it") }
        }
    }
}
