package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.asocexamprep.game.Badges
import com.asocexamprep.game.stars
import com.asocexamprep.ui.game.LevelCard
import com.asocexamprep.ui.game.badgeIcon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.domain.currentStreak

@Composable
fun ExamScreen() = ScreenScaffold("Exam", "exam") {
    PlannedRow(Icons.Outlined.Edit, "Topic practice", "M5")
    PlannedRow(Icons.Outlined.Timer, "Mock exam", "M5")
    PlannedRow(Icons.Outlined.Repeat, "Review", "M5")
    PlannedRow(Icons.Outlined.History, "History", "M5")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen() {
    val c = LocalContext.current.container
    val day = remember { today() }
    val days by c.db.activity().activeDays().collectAsStateWithLifecycle(emptyList())
    val attempts by c.db.progress().attemptCount().collectAsStateWithLifecycle(0)
    val morse by c.db.progress().recentMorse(1).collectAsStateWithLifecycle(emptyList())
    val xp by c.db.game().totalXp().collectAsStateWithLifecycle(0)
    val earned by c.db.game().achievements().collectAsStateWithLifecycle(emptyList())
    val bests by c.db.game().stepBests().collectAsStateWithLifecycle(emptyList())
    val earnedIds = earned.map { it.id }.toSet()

    ScreenScaffold("Progress", "progress") {
        LevelCard(xp, Modifier.fillMaxWidth().testTag("progress_level"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Streak", "${currentStreak(days, day)} d", Modifier.weight(1f))
            StatCard("Stars", "${bests.sumOf { stars(it.accuracy) }}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Active days", "${days.size}", Modifier.weight(1f))
            StatCard("Morse", morse.firstOrNull()?.let { "${it.effectiveWpm} WPM" } ?: "—", Modifier.weight(1f))
        }
        Text(
            "Badges · ${earnedIds.size} of ${Badges.all.size}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp).testTag("badge_count"),
        )
        BoxWithConstraints {
            val w = (maxWidth - 8.dp * 2) / 3 - 1.dp
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Badges.all.forEach { b ->
                    val has = b.id in earnedIds
                    Card(
                        modifier = Modifier.width(w).alpha(if (has) 1f else 0.45f).testTag("badge_${b.id}")
                            .semantics(mergeDescendants = true) { stateDescription = if (has) "Earned" else "Locked" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (has) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (has) badgeIcon(b.icon) else Icons.Outlined.Lock, null, Modifier.size(28.dp))
                            Text(b.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                            Text(b.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Text("$attempts questions answered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
