package com.asocexamprep.ui.morse

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.asocexamprep.container
import com.asocexamprep.morse.KeyDecoder
import kotlinx.coroutines.delay

/**
 * On-screen straight key: sidetone while pressed, timing fed to [decoder], letters ended by a
 * pause (only while the key is up). [onChange] runs after each press and each completed letter.
 */
@Composable
fun KeyArea(decoder: KeyDecoder, toneHz: Int, tag: String, onChange: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalContext.current.container
    var down by remember { mutableStateOf(false) }
    var lastUp by remember(decoder) { mutableLongStateOf(-1L) }
    val tone by rememberUpdatedState(toneHz)
    val changed by rememberUpdatedState(onChange)
    DisposableEffect(Unit) { onDispose { c.morsePlayer.keyUp(); c.morsePlayer.release() } }

    LaunchedEffect(lastUp, down, decoder) {
        if (lastUp < 0 || down) return@LaunchedEffect
        delay(decoder.letterGapMs.toLong())
        decoder.flushLetter(); changed()
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (down) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
            .testTag(tag)
            .pointerInput(decoder) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    val pressedAt = d.uptimeMillis
                    down = true
                    c.morsePlayer.keyDown(tone)
                    val up = waitForUpOrCancellation()
                    c.morsePlayer.keyUp()
                    val upAt = up?.uptimeMillis ?: pressedAt
                    decoder.mark((upAt - pressedAt).toDouble(), if (lastUp >= 0) (pressedAt - lastUp).toDouble() else null)
                    lastUp = upAt
                    down = false
                    changed()
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
}
