package com.asocexamprep.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

fun badgeIcon(name: String): ImageVector = when (name) {
    "book" -> Icons.AutoMirrored.Outlined.MenuBook
    "bolt" -> Icons.Outlined.Bolt
    "gavel" -> Icons.Outlined.Gavel
    "school" -> Icons.Outlined.School
    "graphic" -> Icons.Outlined.GraphicEq
    "abc" -> Icons.Outlined.Abc
    "star" -> Icons.Outlined.Star
    "chat" -> Icons.AutoMirrored.Outlined.Chat
    "target" -> Icons.Outlined.TrackChanges
    "key" -> Icons.Outlined.Key
    "verified" -> Icons.Outlined.Verified
    "fire" -> Icons.Outlined.LocalFireDepartment
    "flag" -> Icons.Outlined.Flag
    "badge" -> Icons.Outlined.Badge
    else -> Icons.Outlined.EmojiEvents
}

/** Round level medal. */
@Composable
fun LevelMedal(level: Int, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
        Text("$level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
    }
}

/** "★★☆" for 0-3 stars. */
fun starText(n: Int) = "★".repeat(n) + "☆".repeat(3 - n)

/** Level medal, rank and progress to the next level. */
@androidx.compose.runtime.Composable
fun LevelCard(xp: Int, modifier: Modifier = Modifier) {
    val level = com.asocexamprep.game.Levels.levelFor(xp)
    val next = com.asocexamprep.game.Levels.xpFor(level + 1)
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        androidx.compose.foundation.layout.Row(
            androidx.compose.ui.Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            LevelMedal(level, androidx.compose.ui.Modifier.size(56.dp))
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                Text("Level $level · ${com.asocexamprep.game.Levels.rank(level)}", style = MaterialTheme.typography.titleMedium, modifier = androidx.compose.ui.Modifier.testTag("level_title"))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { com.asocexamprep.game.Levels.progress(xp) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Text("$xp XP · ${next - xp} to level ${level + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = androidx.compose.ui.Modifier.testTag("level_xp"))
            }
        }
    }
}
