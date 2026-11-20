package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
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
fun MorseScreen() = ScreenScaffold("Morse", "morse") {
    PlannedRow(Icons.Outlined.School, "Learn (Koch)", "M3")
    PlannedRow(Icons.Outlined.Hearing, "Copy", "M3")
    PlannedRow(Icons.Outlined.TouchApp, "Send", "M4")
    PlannedRow(Icons.Outlined.Badge, "Morse test", "M4")
}

@Composable
fun ExamScreen() = ScreenScaffold("Exam", "exam") {
    PlannedRow(Icons.Outlined.Edit, "Topic practice", "M5")
    PlannedRow(Icons.Outlined.Timer, "Mock exam", "M5")
    PlannedRow(Icons.Outlined.Repeat, "Review", "M5")
    PlannedRow(Icons.Outlined.History, "History", "M5")
}

@Composable
fun ProgressScreen() {
    val c = LocalContext.current.container
    val day = remember { today() }
    val days by c.db.activity().activeDays().collectAsStateWithLifecycle(emptyList())
    val attempts by c.db.progress().attemptCount().collectAsStateWithLifecycle(0)
    val exams by c.db.progress().exams().collectAsStateWithLifecycle(emptyList())
    val morse by c.db.progress().recentMorse(1).collectAsStateWithLifecycle(emptyList())

    ScreenScaffold("Progress", "progress") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Streak", "${currentStreak(days, day)} d", Modifier.weight(1f))
            StatCard("Active days", "${days.size}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Questions", "$attempts", Modifier.weight(1f))
            StatCard("Mock exams", "${exams.size}", Modifier.weight(1f))
        }
        StatCard(
            "Morse",
            morse.firstOrNull()?.let { "${it.effectiveWpm} WPM" } ?: "—",
            Modifier.fillMaxWidth(),
        )
        PlannedRow(Icons.AutoMirrored.Outlined.MenuBook, "Topic mastery", "M6")
        PlannedRow(Icons.Outlined.Keyboard, "Morse charts", "M6")
    }
}
