package com.radioprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radioprep.container
import com.radioprep.data.settings.Grade
import com.radioprep.data.settings.Settings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DAY_MS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val store = LocalContext.current.container.settings
    val s by store.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Settings",
        tag = "settings",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        },
    ) {
        Section("Grade") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Grade.entries.forEachIndexed { i, g ->
                    SegmentedButton(
                        selected = s.grade == g,
                        onClick = { scope.launch { store.setGrade(g) } },
                        shape = SegmentedButtonDefaults.itemShape(i, Grade.entries.size),
                        modifier = Modifier.testTag("grade_${g.name}"),
                    ) { Text(g.label) }
                }
            }
        }
        Section("Exam date") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.testTag("exam_date")) {
                    Text(
                        s.examDay?.let {
                            LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        } ?: "Set date",
                    )
                }
                if (s.examDay != null) TextButton(onClick = { scope.launch { store.setExamDay(null) } }) { Text("Clear") }
            }
        }
        Section("Daily goal") {
            var minutes by remember(s.dailyMinutes) { mutableFloatStateOf(s.dailyMinutes.toFloat()) }
            Text("${minutes.toInt()} min")
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                onValueChangeFinished = { scope.launch { store.setDailyMinutes(minutes.toInt()) } },
                valueRange = 5f..120f,
                steps = 22,
            )
        }
    }

    if (picking) {
        val today = remember { LocalDate.now().toEpochDay() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (s.examDay ?: today) * DAY_MS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS >= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> scope.launch { store.setExamDay(ms / DAY_MS) } }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}
