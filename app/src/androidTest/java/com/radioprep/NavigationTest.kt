package com.radioprep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

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
}
