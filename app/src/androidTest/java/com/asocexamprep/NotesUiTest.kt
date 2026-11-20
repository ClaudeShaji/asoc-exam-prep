package com.asocexamprep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() { resetForTest() }

    private fun openTopic(id: String) {
        rule.onNodeWithTag("tab_study").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("topic_g-space").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("scroll_study").performScrollToNode(hasTestTag("topic_$id"))
        rule.onNodeWithTag("topic_$id").performClick()
        rule.onNodeWithTag("screen_topic").assertIsDisplayed()
    }

    @Test fun resistorNotesShowTextTablesAndFigures() {
        openTopic("a-resistors")
        rule.onNodeWithTag("note_attribution").assertTextContains("NIAR Study Manual (2021 edition)", substring = true)
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasText("RESISTORS IN SERIES"))
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasText("10²"))
        assertTrue(rule.onAllNodesWithTag("figure").fetchSemanticsNodes().size >= 3)
    }

    @Test fun licensingShowsRulesThenManual() {
        openTopic("b-licensing")
        rule.onNodeWithTag("notes_rules-2024").assertExists()
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasText("Short title and commencement", substring = true))
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasTestTag("note_source_niar-2021"))
        rule.onNodeWithTag("note_source_niar-2021").performClick()
        rule.onNodeWithTag("notes_niar-2021").assertExists()
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasText("Written for the 1978 rules", substring = true))
    }

    @Test fun morseTopicHasTheManualChapter() {
        openTopic("m-morse")
        rule.onNodeWithTag("scroll_topic").performScrollToNode(hasText("HINTS ON LEARNING MORSE CODE"))
    }
}
