package com.asocexamprep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Markdown blocks used by the content pack: headings, paragraphs, lists, tables, italic notes. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullets(val items: List<String>, val ordered: Boolean) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
}

fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val lines = src.lines()
    var i = 0
    fun cells(l: String) = l.trim().trim('|').split('|').map { it.trim() }
    while (i < lines.size) {
        val l = lines[i].trimEnd()
        when {
            l.isBlank() -> i++
            l.startsWith("#") -> {
                val level = l.takeWhile { it == '#' }.length
                out += MdBlock.Heading(level, l.drop(level).trim()); i++
            }
            l.trimStart().startsWith("|") -> {
                val header = cells(l); i++
                if (i < lines.size && lines[i].contains("---")) i++
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) { rows += cells(lines[i]); i++ }
                out += MdBlock.Table(header, rows)
            }
            Regex("^\\s*([-*]|\\d+\\.) ").containsMatchIn(l) -> {
                val ordered = Regex("^\\s*\\d+\\. ").containsMatchIn(l)
                val items = mutableListOf<String>()
                while (i < lines.size && Regex("^\\s*([-*]|\\d+\\.) ").containsMatchIn(lines[i])) {
                    items += lines[i].replaceFirst(Regex("^\\s*([-*]|\\d+\\.) "), ""); i++
                }
                out += MdBlock.Bullets(items, ordered)
            }
            else -> {
                val para = StringBuilder(l.trim()); i++
                while (i < lines.size && lines[i].isNotBlank() && !Regex("^\\s*(#|\\||[-*] |\\d+\\. )").containsMatchIn(lines[i])) {
                    para.append(' ').append(lines[i].trim()); i++
                }
                out += MdBlock.Paragraph(para.toString())
            }
        }
    }
    return out
}

/** Inline **bold** and *italic*. */
fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) && text.indexOf("**", i + 2) > 0 -> {
                val end = text.indexOf("**", i + 2)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 2, end)) }
                i = end + 2
            }
            text[i] == '*' && text.indexOf('*', i + 1) > 0 -> {
                val end = text.indexOf('*', i + 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                i = end + 1
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
fun Markdown(src: String, modifier: Modifier = Modifier, skipTitle: Boolean = true) {
    val blocks = parseMarkdown(src).let { b ->
        if (skipTitle && (b.firstOrNull() as? MdBlock.Heading)?.level == 1) b.drop(1) else b
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Heading -> Text(
                    b.text,
                    style = if (b.level <= 1) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                is MdBlock.Paragraph -> Text(inline(b.text), style = MaterialTheme.typography.bodyLarge)
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    b.items.forEachIndexed { n, item ->
                        Row {
                            Text(if (b.ordered) "${n + 1}." else "•", Modifier.width(24.dp), color = MaterialTheme.colorScheme.secondary)
                            Text(inline(item), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is MdBlock.Table -> Column {
                    TableRow(b.header, header = true)
                    HorizontalDivider()
                    b.rows.forEach { TableRow(it, header = false); HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.forEachIndexed { i, c ->
            Text(
                inline(c),
                Modifier.weight(if (i == 0) 1.6f else 1f),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
