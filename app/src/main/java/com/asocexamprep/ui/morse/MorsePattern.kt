package com.asocexamprep.ui.morse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asocexamprep.morse.MorseCode

/** Dots and dashes drawn to Morse proportions: a dash is three dots long, one dot apart. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorsePattern(text: String, modifier: Modifier = Modifier, dot: Dp = 6.dp, color: Color = MaterialTheme.colorScheme.secondary) {
    val spoken = text.mapNotNull { MorseCode.pattern(it) }.joinToString(" ") { p -> p.map { if (it == '.') "dot" else "dash" }.joinToString(" ") }
    FlowRow(
        modifier.semantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(dot * 3),
        verticalArrangement = Arrangement.spacedBy(dot * 2),
    ) {
        text.forEach { ch ->
            val p = MorseCode.pattern(ch) ?: return@forEach
            Row(Modifier.height(dot * 2), horizontalArrangement = Arrangement.spacedBy(dot), verticalAlignment = Alignment.CenterVertically) {
                p.forEach { s ->
                    if (s == '.') Box(Modifier.size(dot).background(color, CircleShape))
                    else Box(Modifier.size(dot * 3, dot).background(color, RoundedCornerShape(dot / 2)))
                }
            }
        }
    }
}
