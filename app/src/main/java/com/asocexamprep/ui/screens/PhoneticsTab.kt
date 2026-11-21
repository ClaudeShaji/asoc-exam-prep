package com.asocexamprep.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.asocexamprep.container
import com.asocexamprep.content.Spelled
import com.asocexamprep.voice.Phonetics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What is playing: the row being spoken (by character) or the index in the spelled text. */
private data class Playing(val source: String, val index: Int)

@Composable
fun PhoneticsTab(table: List<Spelled>) {
    val player = LocalContext.current.container.voicePlayer
    val scope = rememberCoroutineScope()
    val words = remember(table) { table.associate { it.char.uppercase() to it.word } }
    var text by rememberSaveable { mutableStateOf("") }
    var playing by remember { mutableStateOf<Playing?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    fun play(source: String, items: List<Phonetics.Spoken>) {
        job?.cancel()
        if (items.isEmpty()) return
        job = scope.launch {
            try {
                player.say(items) { i -> playing = Playing(source, i) }
            } finally {
                playing = null
            }
        }
    }
    fun stop() { job?.cancel(); playing = null }

    val spelled = remember(text) { Phonetics.spell(text) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spell it out", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                label = { Text("Call sign or word") },
                placeholder = { Text("VU2ABC") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth().testTag("phon_input"),
            )
            if (spelled.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    spelled.forEachIndexed { i, s ->
                        if (s.clip == null) return@forEachIndexed
                        val on = playing == Playing("spell", i)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("phon_word_$i").semantics(mergeDescendants = true) { if (on) stateDescription = "Playing" },
                        ) {
                            Text(words[s.char.toString()] ?: s.char.toString(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val busy = playing?.source == "spell"
                Button(
                    onClick = { if (busy) stop() else play("spell", spelled) },
                    enabled = busy || spelled.isNotEmpty(),
                    modifier = Modifier.testTag("phon_spell"),
                ) {
                    Icon(if (busy) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, null)
                    Text(if (busy) "Stop" else "Play")
                }
                val allBusy = playing?.source == "all"
                OutlinedButton(
                    onClick = { if (allBusy) stop() else play("all", Phonetics.spell(table.joinToString("") { it.char })) },
                    modifier = Modifier.testTag("phon_all"),
                ) { Text(if (allBusy) "Stop" else "Play A to Z, 0 to 9") }
            }
        }
    }

    table.forEachIndexed { i, row ->
        val clip = row.char.firstOrNull()?.let(Phonetics::clipFor)
        val lit = playing?.let { it.source == "row:${row.char}" || (it.source == "all" && it.index == i) } == true
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = clip != null) { play("row:${row.char}", Phonetics.spell(row.char)) }
                .testTag("phon_row_${row.char}")
                .semantics { if (lit) stateDescription = "Playing" }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.char, Modifier.width(72.dp),
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium,
            )
            Text(
                row.word, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
                color = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (clip != null) Icon(
                Icons.AutoMirrored.Outlined.VolumeUp, "Play ${row.word}",
                tint = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
    Text(
        "Letters are spoken as their code words. The recordings say figures as plain numbers; the ITU figure code word is shown beside each. ${Phonetics.CREDIT}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 16.dp).testTag("phon_credit"),
    )
}
