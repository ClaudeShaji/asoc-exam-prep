package com.asocexamprep.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Markdown blocks used by the content pack: headings, paragraphs, lists, tables, figures. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    /** Items with their own markers ("•" or the source's number, e.g. "5."). */
    data class Items(val items: List<Pair<String, String>>) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data class Figures(val paths: List<String>) : MdBlock
}

private val ITEM = Regex("^\\s*([-*]|\\d{1,3}\\.) ")
private val FIGURE = Regex("!\\[[^\\]]*]\\(([^)]+)\\)")

fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val lines = src.lines()
    var i = 0
    fun cells(l: String) = l.trim().trim('|').split('|').map { it.trim() }
    while (i < lines.size) {
        val l = lines[i].trimEnd()
        when {
            l.isBlank() -> i++
            l.trimStart().startsWith("![") -> {
                out += MdBlock.Figures(FIGURE.findAll(l).map { it.groupValues[1] }.toList()); i++
            }
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
            ITEM.containsMatchIn(l) -> {
                val items = mutableListOf<Pair<String, String>>()
                while (i < lines.size && ITEM.containsMatchIn(lines[i])) {
                    val m = ITEM.find(lines[i])!!
                    val marker = m.groupValues[1].let { if (it == "-" || it == "*") "•" else it }
                    items += marker to lines[i].substring(m.range.last + 1); i++
                }
                out += MdBlock.Items(items)
            }
            else -> {
                val para = StringBuilder(l.trim()); i++
                while (i < lines.size && lines[i].isNotBlank() &&
                    !Regex("^\\s*(#|\\||!\\[|[-*] |\\d{1,3}\\. )").containsMatchIn(lines[i])
                ) {
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
fun Markdown(
    src: String,
    modifier: Modifier = Modifier,
    skipTitle: Boolean = true,
    image: (String) -> ImageBitmap? = { null },
) {
    val blocks = remember(src) {
        parseMarkdown(src).let { b -> if (skipTitle && (b.firstOrNull() as? MdBlock.Heading)?.level == 1) b.drop(1) else b }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Heading -> Text(
                    b.text,
                    style = when (b.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                is MdBlock.Paragraph -> Text(inline(b.text), style = MaterialTheme.typography.bodyLarge)
                is MdBlock.Items -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val markerWidth = if (b.items.any { it.first.length > 2 }) 32.dp else 24.dp
                    b.items.forEach { (marker, item) ->
                        Row {
                            Text(marker, Modifier.width(markerWidth), color = MaterialTheme.colorScheme.secondary)
                            Text(inline(item), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is MdBlock.Table -> MdTable(b)
                is MdBlock.Figures -> Figures(b.paths, image)
            }
        }
    }
}

@Composable
private fun Figures(paths: List<String>, image: (String) -> ImageBitmap?) {
    val bitmaps = remember(paths) { paths.mapNotNull(image) }
    if (bitmaps.isEmpty()) return
    val density = LocalDensity.current
    // Figures are scanned at ~150-200 dpi; show them near print size, never wider than the column.
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("figure"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bitmaps.forEach { bmp ->
            val natural = with(density) { (bmp.width * 0.75f).toDp() }
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(6.dp),
            ) {
                Image(
                    bmp, contentDescription = null,
                    modifier = Modifier.widthIn(max = if (bitmaps.size == 1) natural.coerceAtMost(360.dp) else natural.coerceAtMost(240.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

@Composable
private fun MdTable(t: MdBlock.Table) {
    val cols = maxOf(t.header.size, t.rows.maxOfOrNull { it.size } ?: 0)
    val wide = cols > 4
    Box(if (wide) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth()) {
        Column(if (wide) Modifier.width((cols * 110).dp) else Modifier.fillMaxWidth()) {
            TableRow(t.header, cols, header = true)
            HorizontalDivider()
            t.rows.forEach { TableRow(it, cols, header = false); HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, cols: Int, header: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until cols) {
            Text(
                inline(cells.getOrElse(i) { "" }),
                Modifier.weight(1f),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
