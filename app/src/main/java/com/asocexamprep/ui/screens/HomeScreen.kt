package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.R
import com.asocexamprep.container
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.domain.currentStreak
import com.asocexamprep.domain.daysUntil
import com.asocexamprep.game.Quests
import com.asocexamprep.game.Today
import com.asocexamprep.game.Xp
import com.asocexamprep.morse.Trainer
import com.asocexamprep.ui.game.LevelCard

@Composable
fun HomeScreen(onSettings: () -> Unit) {
    val c = LocalContext.current.container
    val day = remember { today() }
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val activeDays by c.db.activity().activeDays().collectAsStateWithLifecycle(emptyList())
    val minutesToday by c.db.activity().minutesOn(day).collectAsStateWithLifecycle(0)
    val xp by c.db.game().totalXp().collectAsStateWithLifecycle(0)
    var todayStats by remember { mutableStateOf(Today()) }
    LaunchedEffect(xp, minutesToday, settings.dailyMinutes) { todayStats = c.game.todayStats() }
    val streak = currentStreak(activeDays, day)

    ScreenScaffold(
        title = stringResource(R.string.app_name),
        tag = "home",
        actions = {
            IconButton(onClick = onSettings, modifier = Modifier.testTag("open_settings")) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
    ) {
        GradeSwitch(tagPrefix = "home_grade")
        LearningCard(settings)
        LevelCard(xp, Modifier.fillMaxWidth().testTag("home_level"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment, null, Modifier.size(32.dp),
                        tint = if (streak > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    )
                    Column {
                        Text("$streak", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("home_streak"))
                        Text(if (streak == 1) "day streak" else "days streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (minutesToday.toFloat() / settings.dailyMinutes).coerceIn(0f, 1f) },
                            modifier = Modifier.size(40.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Column {
                        Text("$minutesToday/${settings.dailyMinutes}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("home_goal"))
                        Text("min today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Today's quests", style = MaterialTheme.typography.titleMedium)
                Quests.forDay(day).forEach { q ->
                    val done = q.done(todayStats)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quest_${q.id}").semantics { stateDescription = if (done) "Done" else "Open" },
                    ) {
                        Icon(
                            if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                        Text(q.title, Modifier.weight(1f), color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        Text("+${Xp.QUEST} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        val left = daysUntil(settings.examDay, day)
        StatCard(
            label = "Exam · ${settings.grade.label}",
            value = when {
                settings.examDay == null -> "No date"
                left == null -> "Done"
                left == 0L -> "Today"
                else -> "$left days"
            },
            modifier = Modifier.fillMaxWidth().testTag("exam_countdown"),
        )
    }
}

/** What has actually been learned: syllabus topics and Morse characters. Comes first on Home. */
@Composable
private fun LearningCard(settings: Settings) {
    val c = LocalContext.current.container
    val topics = remember(settings.grade) { c.content.pack.unitsFor(settings.grade.name).flatMap { it.topics }.map { it.id }.toSet() }
    val progress by c.db.progress().topics().collectAsStateWithLifecycle(emptyList())
    val studied = progress.count { it.studied && it.topicId in topics }
    val chars = Trainer.CHAR_ORDER.length
    val learned = settings.trainerStep.coerceAtMost(chars)
    val step = Trainer.step(settings.trainerStep)
    val last by c.db.progress().recentMorse(1).collectAsStateWithLifecycle(emptyList())
    var weak by remember { mutableStateOf(emptyList<Char>()) }
    LaunchedEffect(last) { weak = c.game.weakChars() }

    Card(Modifier.fillMaxWidth().testTag("home_learning"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your learning", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            LearningRow("Syllabus", "$studied of ${topics.size} topics", if (topics.isEmpty()) 0f else studied.toFloat() / topics.size, "home_syllabus")
            if (settings.grade == com.asocexamprep.data.settings.Grade.GENERAL) {
                LearningRow(
                    "Morse",
                    if (learned < chars) "$learned of $chars characters" else "All characters · ${step.label}",
                    settings.trainerStep.toFloat() / Trainer.steps.lastIndex,
                    "home_morse",
                )
            }
            if (weak.isNotEmpty()) {
                Text("Needs work: ${weak.joinToString(" ")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.testTag("home_weak"))
            }
        }
    }
}

@Composable
private fun LearningRow(label: String, value: String, fraction: Float, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag(tag).semantics(mergeDescendants = true) {}) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface,
            drawStopIndicator = {},
        )
    }
}
