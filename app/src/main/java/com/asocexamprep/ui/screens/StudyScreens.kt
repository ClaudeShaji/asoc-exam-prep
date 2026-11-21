package com.asocexamprep.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.db.TopicProgress
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.ui.Markdown
import kotlinx.coroutines.launch

private fun sectionTitle(id: String) = when (id) {
    "A" -> "Section A · Radio Theory and Practice"
    "B" -> "Section B · Radio Regulations"
    else -> "Morse"
}

fun infoIcon(name: String): ImageVector = when (name) {
    "person" -> Icons.Outlined.Person
    "quiz" -> Icons.Outlined.Quiz
    "assignment" -> Icons.Outlined.Assignment
    "payments" -> Icons.Outlined.Payments
    "badge" -> Icons.Outlined.Badge
    else -> Icons.Outlined.Radio
}

@Composable
fun StudyScreen(onTopic: (String) -> Unit, onInfo: () -> Unit, onReference: () -> Unit) {
    val c = LocalContext.current.container
    val pack = c.content.pack
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val progress by c.db.progress().topics().collectAsStateWithLifecycle(emptyList())
    val studied = progress.filter { it.studied }.map { it.topicId }.toSet()
    val units = remember(settings.grade) { pack.unitsFor(settings.grade.name) }
    val all = units.flatMap { it.topics }

    ScreenScaffold("Study", "study") {
        GradeSwitch(tagPrefix = "study_grade")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutCard("Licence info", Icons.Outlined.Verified, Modifier.weight(1f).testTag("open_info"), onInfo)
            ShortcutCard("Reference", Icons.Outlined.TableChart, Modifier.weight(1f).testTag("open_reference"), onReference)
        }
        Column {
            Text(
                "${settings.grade.label} · ${all.count { it.id in studied }} of ${all.size} topics",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(6.dp))
            LinearProgressIndicator(
                progress = { if (all.isEmpty()) 0f else all.count { it.id in studied } / all.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
        }
        var lastSection: String? = null
        units.forEach { unit ->
            if (unit.section != lastSection) {
                lastSection = unit.section
                Text(
                    sectionTitle(unit.section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(unit.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${unit.topics.count { it.id in studied }}/${unit.topics.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    unit.topics.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onTopic(t.id) }.testTag("topic_${t.id}")
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (t.id in studied) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                tint = if (t.id in studied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(t.title, Modifier.weight(1f))
                            Text("${t.minutes} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        Text("Sources", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        pack.sources.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.outline)
                Column(Modifier.weight(1f)) {
                    Text(s.title)
                    Text("${s.publisher} · ${s.year} · ${s.pages} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s.official) Icon(Icons.Outlined.Verified, "Official", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ShortcutCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun shortTitle(sourceId: String) = when (sourceId) {
    "rules-2024" -> "Rules 2024"
    "niar-2021" -> "NIAR manual"
    else -> sourceId
}

private fun pagesLabel(pages: String): String? {
    val (a, b) = pages.split("-").takeIf { it.size == 2 } ?: return null
    return if (a == b) "p. $a" else "pp. $a–$b"
}

@Composable
private fun BackButton(onBack: () -> Unit) =
    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }

@Composable
fun TopicScreen(topicId: String, onBack: () -> Unit, onPractice: ((String) -> Unit)? = null) {
    val c = LocalContext.current.container
    val pack = c.content.pack
    val topic = pack.topic(topicId) ?: return
    val unit = pack.unitOf(topicId)
    val progress by remember(topicId) { c.db.progress().topic(topicId) }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    val studied = progress?.studied == true

    ScreenScaffold(topic.title, "topic", navigationIcon = { BackButton(onBack) }) {
        Text(
            listOfNotNull(unit?.let { "${sectionTitle(it.section).substringBefore(" ·")} · ${it.title}" }, "${topic.minutes} min").joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (topic.grades.size == 1) AssistChip(onClick = {}, label = { Text("${pack.grade(topic.grades[0])?.title} only") })
        val toggle: () -> Unit = {
            scope.launch {
                val now = System.currentTimeMillis()
                val p = progress ?: TopicProgress(topicId)
                c.db.progress().upsertTopic(p.copy(studied = !studied, lastSeenAt = now))
                // Minutes and XP count once per topic, however often it is re-marked.
                if (!studied) c.game.topicStudied(topicId, topic.minutes)
            }
        }
        if (studied) OutlinedButton(onClick = toggle, modifier = Modifier.fillMaxWidth().testTag("toggle_studied")) { Text("Studied ✓") }
        else FilledTonalButton(onClick = toggle, modifier = Modifier.fillMaxWidth().testTag("toggle_studied")) { Text("Mark as studied") }
        val questionCount = remember(topicId) { pack.questions.count { it.topic == topicId } }
        if (onPractice != null && questionCount > 0) {
            OutlinedButton(onClick = { onPractice(topicId) }, modifier = Modifier.fillMaxWidth().testTag("topic_practice")) {
                Text("Practice questions ($questionCount)")
            }
        }

        Text("Syllabus", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Markdown(topic.syllabus.joinToString("\n") { "- $it" })

        val notes = remember(topicId) { pack.notesFor(topicId) }
        if (notes.isNotEmpty()) {
            var selected by rememberSaveable(topicId) { mutableIntStateOf(0) }
            Text("Notes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            if (notes.size > 1) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    notes.forEachIndexed { i, n ->
                        SegmentedButton(
                            selected = selected == i,
                            onClick = { selected = i },
                            shape = SegmentedButtonDefaults.itemShape(i, notes.size),
                            modifier = Modifier.testTag("note_source_${n.source}"),
                        ) { Text(pack.source(n.source)?.let { shortTitle(it.id) } ?: n.source) }
                    }
                }
            }
            val note = notes[selected.coerceIn(0, notes.lastIndex)]
            val src = pack.source(note.source)
            Text(
                listOfNotNull(src?.title, pagesLabel(note.pages)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("note_attribution"),
            )
            if (src?.official == false && unit?.section == "B") {
                Text(
                    "Written for the 1978 rules. Current rules and fees are in Licence info.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Markdown(
                pack.noteText(note),
                modifier = Modifier.testTag("notes_${note.source}"),
                skipTitle = false,
                image = { c.content.noteImage(it) },
            )
        }

    }
}

@Composable
fun InfoListScreen(onPage: (String) -> Unit, onBack: () -> Unit) {
    val pack = LocalContext.current.container.content.pack
    ScreenScaffold("Licence info", "info", navigationIcon = { BackButton(onBack) }) {
        pack.info.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clickable { onPage(p.id) }.testTag("info_${p.id}").padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(infoIcon(p.icon), null, tint = MaterialTheme.colorScheme.primary)
                Text(p.title, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
        Text(pack.exam.authority, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun InfoPageScreen(id: String, onBack: () -> Unit) {
    val pack = LocalContext.current.container.content.pack
    val page = pack.info.firstOrNull { it.id == id } ?: return
    val source = pack.sources.firstOrNull { it.id == page.source }
    ScreenScaffold(page.title, "info_page", navigationIcon = { BackButton(onBack) }) {
        Markdown(pack.infoText(id).orEmpty())
        Text(
            "Checked ${page.verified}" + (source?.let { " · ${it.title}" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
