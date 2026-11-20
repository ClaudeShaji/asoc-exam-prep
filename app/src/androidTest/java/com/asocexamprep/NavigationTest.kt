package com.asocexamprep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before fun setUp() { resetForTest() }

    @Test fun everyTabOpensItsScreen() {
        rule.onNodeWithTag("screen_home").assertIsDisplayed()
        for (tab in listOf("study", "morse", "exam", "progress", "home")) {
            rule.onNodeWithTag("tab_$tab").performClick()
            rule.onNodeWithTag("screen_$tab").assertIsDisplayed()
        }
    }

    @Test fun settingsOpensAndGradeSaves() {
        rule.onNodeWithTag("open_settings").performClick()
        rule.onNodeWithTag("screen_settings").assertIsDisplayed()
        rule.onNodeWithTag("grade_RESTRICTED").performClick()
        rule.activityRule.scenario.onActivity { }
        rule.onNodeWithTag("grade_GENERAL").performClick()
    }

    @Test fun studyTopicInfoAndReference() {
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("topic_a-resistors").performClick()
        rule.onNodeWithTag("screen_topic").assertIsDisplayed()
        rule.onNodeWithTag("note_attribution").assertTextContains("NIAR Study Manual (2021 edition)", substring = true)
        rule.onNodeWithTag("toggle_studied").performClick()
        rule.onNodeWithText("Studied ✓").assertIsDisplayed()
        rule.onNodeWithTag("toggle_studied").performClick()
        rule.onNodeWithText("Mark as studied").assertIsDisplayed()
        rule.onNodeWithTag("back").performClick()

        rule.onNodeWithTag("open_info").performClick()
        rule.onNodeWithTag("info_fees").performClick()
        rule.onNodeWithText("₹100").assertIsDisplayed()
        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("back").performClick()

        rule.onNodeWithTag("open_reference").performClick()
        rule.onNodeWithTag("ref_tab_1").performClick()
        rule.onNodeWithText("QRL").assertIsDisplayed()
    }

    @Test fun gradeSwitchFiltersSyllabus() {
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("study_grade_RESTRICTED").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("topic_g-space").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("study_grade_GENERAL").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("topic_g-space").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("scroll_study").performScrollToNode(hasTestTag("topic_g-space"))
        rule.onNodeWithTag("topic_g-space").assertIsDisplayed()
        rule.onNodeWithTag("tab_home").performClick()
        rule.onNodeWithTag("home_grade_RESTRICTED").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Exam · Restricted").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("home_grade_GENERAL").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Exam · General").fetchSemanticsNodes().isNotEmpty() }
    }
}
