package com.asocexamprep.ui.morse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Grade
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.ui.screens.ScreenScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MorseScreen(onOpen: (String) -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val last by c.db.progress().recentMorse(1).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    ScreenScaffold("Morse", "morse") {
        if (s.grade == Grade.RESTRICTED) {
            Text("Morse is only tested for the General grade.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Exam: receive and send at 8 WPM for 5 minutes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val step = com.asocexamprep.morse.Trainer.step(s.trainerStep)
        MenuCard(Icons.Outlined.School, "Learn", step.label + (step.char?.let { " · $it" } ?: ""), "morse_learn") { onOpen("learn") }
        MenuCard(Icons.Outlined.Hearing, "Copy practice", "Letters, call signs, Q codes, words, exam test", "morse_copy") { onOpen("copy") }
        MenuCard(Icons.Outlined.TouchApp, "Send practice", "Key with your finger", "morse_send") { onOpen("send") }
        MenuCard(Icons.Outlined.GridView, "Alphabet", "Tap a character to hear it", "morse_alphabet") { onOpen("alphabet") }

        last.firstOrNull()?.let {
            Text(
                "Last: ${modeLabel(it.mode)} · ${(it.accuracy * 100).roundToInt()}% at ${it.charWpm}/${it.effectiveWpm} WPM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Speed and tone", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        SettingSlider("Character speed", s.charWpm, "WPM", 5..30, "speed_char") { scope.launch { c.settings.setCharWpm(it) } }
        SettingSlider("Overall speed", s.effectiveWpm, "WPM", 3..s.charWpm.coerceAtLeast(4), "speed_effective") { scope.launch { c.settings.setEffectiveWpm(it) } }
        SettingSlider("Tone", s.toneHz, "Hz", 400..1000, "tone", step = 50) { scope.launch { c.settings.setToneHz(it) } }
        OutlinedButton(
            onClick = { scope.launch { c.morsePlayer.play("VVV", MorseTiming(s.charWpm, s.effectiveWpm), s.toneHz) } },
            modifier = Modifier.testTag("tone_test"),
        ) { Text("Test sound") }
        Text(
            "Characters are sent at the character speed with longer gaps, so you learn the sound of each letter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
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

@Composable
private fun SettingSlider(label: String, value: Int, unit: String, range: IntRange, tag: String, step: Int = 1, onSet: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    // Save shortly after the value settles: works for drags, taps and accessibility actions alike.
    LaunchedEffect(v) {
        if (v.roundToInt() != value) { delay(300); onSet(v.roundToInt()) }
    }
    Column {
        Row {
            Text(label, Modifier.weight(1f))
            Text("${v.roundToInt()} $unit", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("${tag}_value"))
        }
        Slider(
            value = v.coerceIn(range.first.toFloat(), range.last.toFloat()),
            onValueChange = { v = (Math.round(it / step) * step).toFloat() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.testTag(tag),
        )
    }
}

private fun modeLabel(mode: String) = when {
    mode == "learn" -> "Learn"
    mode == "send" -> "Send"
    mode.startsWith("copy-") -> "Copy, " + (com.asocexamprep.morse.CopyMode.entries.firstOrNull { it.name.equals(mode.removePrefix("copy-"), true) }?.label ?: "practice").lowercase()
    else -> mode
}
