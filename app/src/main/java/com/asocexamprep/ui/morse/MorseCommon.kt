package com.asocexamprep.ui.morse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.asocexamprep.AppContainer
import com.asocexamprep.data.db.MorseSession
import com.asocexamprep.morse.CopyScore
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.ui.screens.today
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Composable
fun MorseBack(onBack: () -> Unit) =
    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }

/** Text field for copying what is heard: capitals, no autocorrect, monospace. */
@Composable
fun CopyField(value: String, onChange: (String) -> Unit, enabled: Boolean, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.uppercase()) },
        enabled = enabled,
        label = { Text("Your copy") },
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
        ),
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

/** Sent text with each character green (copied) or red (missed). */
@Composable
fun ScoreView(score: CopyScore, sent: String, tagPrefix: String, extra: String? = null) {
    val good = MaterialTheme.colorScheme.primary
    val bad = MaterialTheme.colorScheme.error
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${(score.accuracy * 100).roundToInt()}%",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.testTag("${tagPrefix}_accuracy"),
        )
        Text("${score.errors} errors" + (extra?.let { " · $it" } ?: ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
        var k = 0
        Text(
            buildAnnotatedString {
                sent.forEach { ch ->
                    if (ch == ' ') append(' ') else {
                        val ok = score.results.getOrNull(k++)?.ok == true
                        withStyle(SpanStyle(color = if (ok) good else bad, fontWeight = if (ok) FontWeight.Normal else FontWeight.Bold)) { append(ch) }
                    }
                }
            },
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("${tagPrefix}_sent"),
        )
    }
}

/** Saves a practice session and counts its minutes toward today. */
suspend fun recordSession(c: AppContainer, mode: String, timing: MorseTiming, score: CopyScore?, startedAt: Long, kochLevel: Int = 0) {
    val duration = System.currentTimeMillis() - startedAt
    val stats = score?.perChar()?.mapKeys { it.key.toString() }?.mapValues { listOf(it.value.first, it.value.second) } ?: emptyMap()
    c.db.progress().insertMorse(
        MorseSession(
            mode = mode,
            charWpm = timing.charWpm,
            effectiveWpm = timing.effectiveWpm,
            accuracy = score?.accuracy ?: 0f,
            perCharStats = Json.encodeToString(stats),
            kochLevel = kochLevel,
            durationMillis = duration,
            startedAt = startedAt,
        ),
    )
    c.db.activity().add(today(), "morse", ((duration + 30_000) / 60_000).toInt().coerceAtLeast(1))
}

