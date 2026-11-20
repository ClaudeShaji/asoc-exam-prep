package com.asocexamprep.ui.morse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asocexamprep.container
import com.asocexamprep.data.settings.Settings
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.MorseTiming
import kotlinx.coroutines.launch

/** The three pages of the chart: letters, figures, and punctuation/prosigns. */
private val pages: List<Pair<String, List<Char>>> = listOf(
    "Letters" to MorseCode.table.keys.filter { it.isLetter() },
    "Numbers" to MorseCode.table.keys.filter { it.isDigit() },
    "Symbols" to MorseCode.table.keys.filter { !it.isLetterOrDigit() },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlphabetScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { pages.size }

    Column(Modifier.fillMaxSize().testTag("screen_alphabet")) {
        TopAppBar(title = { Text("Alphabet") }, navigationIcon = { MorseBack(onBack) })
        PrimaryTabRow(selectedTabIndex = pager.currentPage) {
            pages.forEachIndexed { i, (title, _) ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(title) },
                    modifier = Modifier.testTag("alpha_tab_$i"),
                )
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            BoxWithConstraints(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("alpha_page_$page"),
            ) {
                val cell = (maxWidth - 8.dp * 3) / 4 - 1.dp
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages[page].second.forEach { ch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.width(cell).testTag("alpha_${ch.code}")
                                .clickable { scope.launch { c.morsePlayer.play("$ch", MorseTiming(s.charWpm, s.effectiveWpm), s.toneHz) } },
                        ) {
                            Column(
                                Modifier.padding(vertical = 12.dp).width(cell),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(MorseCode.prosign[ch] ?: "$ch", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                                MorsePattern("$ch", dot = 4.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
