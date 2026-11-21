package com.asocexamprep

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneticsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var voice: FakeVoicePlayer

    @Before fun setUp() {
        resetForTest()
        voice = app.container.voicePlayer as FakeVoicePlayer
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("open_reference").performClick()
        rule.onNodeWithTag("ref_tab_0").performClick()
    }

    @Test fun spellsWhatYouType() {
        rule.onNodeWithTag("phon_spell").assertIsNotEnabled()
        rule.onNodeWithTag("phon_input").performTextInput("vu2 ab")
        rule.onNodeWithTag("phon_word_0").assertTextContains("Victor")
        rule.onNodeWithTag("phon_word_2").assertTextContains("Bissotwo")
        rule.onNodeWithTag("phon_word_5").assertTextContains("Bravo")
        rule.onNodeWithTag("phon_spell").assertIsEnabled().performClick()
        rule.waitUntil(3_000) { voice.said.size == 5 }
        assertEquals(listOf("v", "u", "d2", "a", "b"), voice.said.toList())
    }

    @Test fun eachRowAndTheWholeTablePlay() {
        rule.onNodeWithTag("scroll_reference").performScrollToNode(hasTestTag("phon_row_Q"))
        rule.onNodeWithTag("phon_row_Q").performClick()
        rule.waitUntil(3_000) { voice.said.toList() == listOf("q") }
        rule.onNodeWithTag("scroll_reference").performScrollToNode(hasTestTag("phon_credit"))
        rule.onNodeWithTag("phon_credit").assertTextContains("Settings → About", substring = true)
        rule.onNodeWithTag("scroll_reference").performScrollToNode(hasTestTag("phon_all"))
        rule.onNodeWithTag("phon_all").performClick()
        rule.waitUntil(10_000) { voice.said.size == 37 }
        assertEquals("d9", voice.said.last())
    }
}
