package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Grade
import com.asocexamprep.data.settings.Settings
import kotlinx.coroutines.launch

/** General / Restricted switch; the choice is saved and filters syllabus, exams and Morse everywhere. */
@Composable
fun GradeSwitch(modifier: Modifier = Modifier, tagPrefix: String = "grade") {
    val store = LocalContext.current.container.settings
    val s by store.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        Grade.entries.forEachIndexed { i, g ->
            SegmentedButton(
                selected = s.grade == g,
                onClick = { scope.launch { store.setGrade(g) } },
                shape = SegmentedButtonDefaults.itemShape(i, Grade.entries.size),
                modifier = Modifier.testTag("${tagPrefix}_${g.name}"),
            ) { Text(g.label) }
        }
    }
}
