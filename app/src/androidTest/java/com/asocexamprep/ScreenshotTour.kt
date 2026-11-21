package com.asocexamprep

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Design-review screenshots (run explicitly); saved to the app's files/shots folder. */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.waitForIdle()
        val dir = File(app.filesDir, "shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun examScreens() {
        resetForTest()
        rule.onNodeWithTag("tab_exam").performClick()
        shot("exam-tab")
        rule.onNodeWithTag("exam_paper_B").performClick()
        rule.onNodeWithTag("mock_start").performClick()
        rule.onNodeWithTag("live_toggle").performClick()
        val pack = app.container.content.pack
        for (n in 0..1) {
            rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_$n"))
            val id = rule.onNodeWithTag("mq_stem_$n").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            val q = pack.questions.first { it.id == id }
            val pick = if (n == 0) q.answer else (q.answer + 1) % 4
            rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_${n}_opt_$pick"))
            rule.onNodeWithTag("mq_${n}_opt_$pick").performClick()
        }
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_0"))
        shot("paper-live")
        // Open the first hint that has a passage.
        val withHint = (0 until 50).first { n ->
            rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_$n"))
            rule.onNodeWithTag("mq_stem_$n").fetchSemanticsNode().config[SemanticsProperties.StateDescription] in pack.hints
        }
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_hint_$withHint"))
        rule.onNodeWithTag("mq_hint_$withHint").performClick()
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_$withHint"))
        shot("paper-hint")
        rule.onNodeWithTag("live_toggle").performClick()
    }

    @Test fun phonetics() {
        resetForTest()
        // Hold on the third word so the highlight shows.
        app.container.voicePlayer = object : com.asocexamprep.voice.VoicePlayer {
            override suspend fun say(items: List<com.asocexamprep.voice.Phonetics.Spoken>, onItem: (Int) -> Unit) {
                onItem(2); kotlinx.coroutines.awaitCancellation()
            }
            override fun release() {}
        }
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("open_reference").performClick()
        rule.onNodeWithTag("phon_input").performTextInput("VU2ABC")
        rule.onNodeWithTag("phon_spell").performClick()
        shot("phonetics")
        rule.onNodeWithTag("phon_spell").performClick()
    }

    @Test fun about() {
        resetForTest()
        rule.onNodeWithTag("open_settings").performClick()
        rule.onNodeWithTag("scroll_settings").performScrollToNode(hasTestTag("open_about"))
        rule.onNodeWithTag("open_about").performClick()
        shot("about-top")
        rule.onNodeWithTag("scroll_about").performScrollToNode(hasTestTag("about_licence_voice"))
        shot("about-voice")
    }
}
