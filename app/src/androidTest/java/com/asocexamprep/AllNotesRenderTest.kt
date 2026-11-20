package com.asocexamprep

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.asocexamprep.ui.screens.TopicScreen
import com.asocexamprep.ui.theme.RadioPrepTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders every topic page with its notes, so a bad note can't crash the app. */
@RunWith(AndroidJUnit4::class)
class AllNotesRenderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyTopicRendersItsNotes() {
        val pack = app.container.content.pack
        var id by mutableStateOf(pack.topics.first().id)
        rule.setContent { RadioPrepTheme { TopicScreen(id, onBack = {}) } }
        var figures = 0
        for (t in pack.topics) {
            id = t.id
            rule.waitForIdle()
            rule.onNodeWithTag("screen_topic").assertExists()
            val first = pack.notesFor(t.id).first()
            rule.onNodeWithTag("notes_${first.source}").assertExists()
            figures += rule.onAllNodesWithTag("figure").fetchSemanticsNodes().size
        }
        assertTrue("figures rendered: $figures", figures > 40)
    }
}
