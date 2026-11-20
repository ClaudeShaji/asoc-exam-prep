package com.radioprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radioprep.container
import com.radioprep.data.settings.Settings
import com.radioprep.domain.currentStreak
import com.radioprep.domain.daysUntil

@Composable
fun HomeScreen(onSettings: () -> Unit) {
    val c = LocalContext.current.container
    val day = remember { today() }
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val activeDays by c.db.activity().activeDays().collectAsStateWithLifecycle(emptyList())
    val minutesToday by c.db.activity().minutesOn(day).collectAsStateWithLifecycle(0)
    val due by remember { c.db.progress().dueCount(System.currentTimeMillis()) }.collectAsStateWithLifecycle(0)

    ScreenScaffold(
        title = "Radio Prep",
        tag = "home",
        actions = {
            IconButton(onClick = onSettings, modifier = Modifier.testTag("open_settings")) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
    ) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Streak", "${currentStreak(activeDays, day)} d", Modifier.weight(1f))
            StatCard("Today", "$minutesToday / ${settings.dailyMinutes} min", Modifier.weight(1f))
        }
        StatCard("Due for review", "$due", Modifier.fillMaxWidth())
    }
}
