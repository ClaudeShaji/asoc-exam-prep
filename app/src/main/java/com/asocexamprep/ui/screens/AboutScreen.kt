package com.asocexamprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asocexamprep.container
import com.asocexamprep.voice.Phonetics

private data class Library(val name: String, val by: String)

private val libraries = listOf(
    Library("Jetpack Compose, Material 3, Material Icons", "Google / Android Open Source Project"),
    Library("AndroidX Core, Activity, Navigation, Lifecycle", "Google / Android Open Source Project"),
    Library("Room, DataStore", "Google / Android Open Source Project"),
    Library("Kotlin standard library", "JetBrains"),
    Library("kotlinx.coroutines, kotlinx.serialization", "JetBrains"),
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pack = context.container.content.pack
    val uri = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    val questionsBySource = remember { pack.questions.groupingBy { it.source }.eachCount() }

    ScreenScaffold(
        "About", "about",
        navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ASOC Exam Prep", style = MaterialTheme.typography.headlineSmall)
            Text("Version $version", Modifier.testTag("about_version"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "A free study aid for India's Amateur Station Operator's Certificate exam. Not affiliated with or endorsed by " +
                    "the Department of Telecommunications, WPC, NIAR or any author listed below. Check the official rules before your exam.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Heading("Study material and questions")
        Text(
            "Notes are quoted from these publications, with credit on every page. Copyright stays with each publisher; " +
                "government publications are reproduced as published.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pack.sources.forEach { s ->
            Item(
                tag = "about_source_${s.id}",
                title = s.title,
                lines = listOfNotNull(
                    "${s.publisher}, ${s.year}",
                    questionsBySource[s.id]?.let { "$it exam questions with the published answer key" },
                ),
                link = s.url,
                onLink = { runCatching { uri.openUri(s.url) } },
            )
        }
        Item(
            tag = "about_source_ncvec",
            title = "NCVEC question pools (USA)",
            lines = listOf("National Conference of Volunteer Examiner Coordinators · public domain", "Used to cross-check answer keys"),
            link = "https://www.ncvec.org",
            onLink = { runCatching { uri.openUri("https://www.ncvec.org") } },
        )

        Heading("Voice recordings")
        Item(
            tag = "about_voice",
            title = "Phonetic alphabet and digits",
            lines = listOf(
                "Recorded by Allison Smith for the Asterisk open-source PBX (asterisk-core-sounds-en), © 2003–2008 Allison Smith",
                "Creative Commons Attribution-ShareAlike 3.0. Converted from G.722 to Ogg; otherwise unmodified.",
            ),
            link = "https://downloads.asterisk.org/pub/telephony/sounds/",
            onLink = { runCatching { uri.openUri("https://downloads.asterisk.org/pub/telephony/sounds/") } },
        )
        LicenceText("about_licence_voice", "${Phonetics.DIR}/LICENSE.txt")

        Heading("Open-source libraries")
        libraries.forEach { Item(tag = null, title = it.name, lines = listOf("${it.by} · Apache License 2.0")) }
        LicenceText("about_licence_apache", "licenses/apache-2.0.txt")
        Text("", Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun Heading(text: String) =
    Text(text, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

@Composable
private fun Item(tag: String?, title: String, lines: List<String>, link: String? = null, onLink: () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().let { if (tag != null) it.testTag(tag) else it }.semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (link != null) TextButton(onClick = onLink, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(link.removePrefix("https://").removePrefix("http://").substringBefore('/'), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A collapsed licence; tap to read the full text bundled in the app. */
@Composable
private fun LicenceText(tag: String, asset: String) {
    val context = LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.testTag(tag)) {
        Text(if (open) "Hide licence text" else "Show licence text")
    }
    if (open) {
        val text = remember(asset) { runCatching { context.assets.open(asset).bufferedReader().readText() }.getOrDefault("") }
        SelectionContainer {
            Text(
                text.trim(), Modifier.testTag("${tag}_text"),
                fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
