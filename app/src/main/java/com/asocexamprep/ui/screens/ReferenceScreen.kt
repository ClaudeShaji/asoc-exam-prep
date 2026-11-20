package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asocexamprep.container

private val tabs = listOf("Phonetics", "Q codes", "Abbreviations", "Bands")

@Composable
fun ReferenceScreen(onBack: () -> Unit) {
    val ref = LocalContext.current.container.content.pack.reference
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column {
        ScreenScaffold(
            "Reference", "reference",
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
        ) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }, modifier = Modifier.testTag("ref_tab_$i"))
                }
            }
            when (tab) {
                0 -> {
                    (ref.phonetics + ref.figures).forEach { KeyRow(it.char, it.word) }
                }
                1 -> ref.qcodes.forEach { q ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Code(q.code)
                            Column {
                                Text(q.question, style = MaterialTheme.typography.bodyLarge)
                                Text(q.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
                2 -> ref.abbreviations.forEach { KeyRow(it.code, it.meaning) }
                else -> {
                    Text(ref.bands.power, style = MaterialTheme.typography.bodyMedium)
                    ref.bands.rows.forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(b.band, Modifier.width(64.dp), fontWeight = FontWeight.SemiBold)
                            Column(Modifier.weight(1f)) {
                                Text(b.range)
                                if (b.note.isNotEmpty()) Text(b.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Text("NIAR Study Manual (2021), p. ${ref.bands.page}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun Code(code: String) = Text(
    code,
    Modifier.width(72.dp),
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.typography.titleMedium,
)

@Composable
private fun KeyRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Code(key)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
