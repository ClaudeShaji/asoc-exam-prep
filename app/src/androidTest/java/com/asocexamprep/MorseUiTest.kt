package com.asocexamprep

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.asocexamprep.morse.Stage
import com.asocexamprep.morse.Trainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MorseUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var player: FakeMorsePlayer

    @Before fun setUp() {
        player = resetForTest()
        rule.onNodeWithTag("tab_morse").performClick()
        rule.onNodeWithTag("screen_morse").assertIsDisplayed()
    }

    private fun waitForTag(tag: String) = rule.waitUntil(5_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test fun everyMorseItemOpensItsScreen() {
        for ((item, screen) in listOf("morse_learn" to "screen_learn", "morse_copy" to "screen_copy", "morse_send" to "screen_send", "morse_alphabet" to "screen_alphabet")) {
            rule.onNodeWithTag(item).performClick()
            rule.onNodeWithTag(screen).assertIsDisplayed()
            rule.onNodeWithTag("back").performClick()
            rule.onNodeWithTag("screen_morse").assertIsDisplayed()
        }
    }

    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()

    /** Answers the 10 single-character prompts; [answer] maps the played character to what is typed. */
    private fun answerCharDrill(answer: (String) -> String) {
        val base = player.played.size
        rule.onNodeWithTag("char_start").performClick()
        for (i in 0 until 10) {
            rule.waitUntil(5_000) { player.played.size >= base + i + 1 && rule.onAllNodesWithTag("char_input").fetchSemanticsNodes().isNotEmpty() }
            rule.waitUntil(5_000) { text("char_count") == "${i + 1} of 10" }
            rule.onNodeWithTag("char_input").performTextInput(answer(player.played[base + i]))
        }
        waitForTag("char_score")
    }

    @Test fun firstCharacterIsLearnedAloneThenTheNextUnlocks() {
        rule.onNodeWithTag("morse_learn").performClick()
        rule.onNodeWithTag("learn_step").assertTextContains("Character 1 of 43")
        rule.onNodeWithTag("char_new").assertTextContains("K")
        rule.onNodeWithTag("char_hear").performClick()
        rule.waitUntil(5_000) { player.played.isNotEmpty() }
        assertEquals("K K K", player.played.last())

        answerCharDrill { it.lowercase() }
        assertTrue("only K was sent", player.played.drop(1).all { it == "K" })
        rule.onNodeWithTag("char_score").assertTextContains("10 / 10")
        rule.onNodeWithTag("learn_unlocked").assertTextContains("Character 2 of 43: M", substring = true)
        rule.waitUntil(5_000) { runBlocking { app.container.settings.settings.first().trainerStep } == 1 }
        rule.onNodeWithTag("learn_continue").performScrollTo().performClick()
        rule.onNodeWithTag("learn_step").assertTextContains("Character 2 of 43")
        rule.onNodeWithTag("char_new").assertTextContains("M")
    }

    @Test fun wrongAnswersShowTheCharacterAndDoNotUnlock() {
        rule.onNodeWithTag("morse_learn").performClick()
        answerCharDrill { "E" }
        rule.onNodeWithTag("char_score").assertTextContains("0 / 10")
        assertEquals(0, rule.onAllNodesWithTag("learn_unlocked").fetchSemanticsNodes().size)
        assertEquals(0, runBlocking { app.container.settings.settings.first().trainerStep })
    }

    @Test fun wrongAnswerFeedbackNamesTheCharacter() {
        rule.onNodeWithTag("morse_learn").performClick()
        val base = player.played.size
        rule.onNodeWithTag("char_start").performClick()
        rule.waitUntil(5_000) { player.played.size > base }
        // Hold the clock so the feedback (shown briefly before the next prompt) can be checked.
        rule.onNodeWithTag("char_input").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("char_input").performTextInput("E")
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("char_feedback").assertExists()
        assertEquals(1, rule.onAllNodesWithText("It was K").fetchSemanticsNodes().size)
        rule.mainClock.autoAdvance = true
        rule.waitUntil(5_000) { text("char_count").startsWith("2 of") }
    }

    @Test fun mixedLetterGroupsComeAfterAllCharacters() {
        val groups = Trainer.firstOf(Stage.GROUPS).index
        runBlocking { app.container.settings.setTrainerStep(groups) }
        rule.onNodeWithTag("morse_learn").performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Groups of 2 letters" }
        rule.onNodeWithTag("learn_start").performClick()
        waitForTag("learn_check")
        val sent = player.played.last()
        val safe = app.container.content.pack.training!!.groups.getValue("2")
        assertTrue(sent, sent.split(" ").size == 10 && sent.split(" ").all { it in safe })
        rule.onNodeWithTag("learn_input").performTextInput(sent)
        rule.onNodeWithTag("learn_check").performClick()
        rule.onNodeWithTag("learn_accuracy").assertTextContains("100%")
        waitForTag("learn_unlocked")
        rule.onNodeWithTag("learn_unlocked").assertTextContains("Groups of 3 letters", substring = true)
        rule.onNodeWithTag("learn_continue").performScrollTo().performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Groups of 3 letters" }

        // Earlier stages stay open for review; later ones are locked.
        rule.onNodeWithTag("learn_stage_WORDS").assertIsNotEnabled()
        rule.onNodeWithTag("learn_stage_CHARACTERS").performScrollTo().performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Character 43 of 43" }
        rule.onNodeWithTag("learn_stage_GROUPS").performScrollTo().performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Groups of 3 letters" }
    }

    @Test fun wordsAndSentencesAreTheLastStages() {
        runBlocking { app.container.settings.setTrainerStep(Trainer.steps.lastIndex) }
        rule.onNodeWithTag("morse_learn").performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Sentences" }
        rule.onNodeWithTag("learn_start").performClick()
        waitForTag("learn_check")
        val sent = player.played.last()
        assertTrue(sent, app.container.content.pack.training!!.sentences.any { sent.contains(it) })
        rule.onNodeWithTag("learn_stage_WORDS").performScrollTo().performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Long words" }
    }

    @Test fun copyPracticeScoresCallSignsAndExam() {
        rule.onNodeWithTag("morse_copy").performClick()
        rule.onNodeWithTag("copy_mode_CALLSIGNS").performClick()
        rule.onNodeWithTag("copy_start").performClick()
        waitForTag("copy_check")
        val calls = player.played.last()
        assertTrue(calls, calls.split(" ").all { Regex("^(VU[2347]|AT2)[A-Z]{2,3}$").matches(it) })
        rule.onNodeWithTag("copy_input").performTextInput(calls.split(" ").first())
        rule.onNodeWithTag("copy_check").performClick()
        val partial = rule.onNodeWithTag("copy_accuracy").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        assertTrue(partial, partial.removeSuffix("%").toInt() < 50)

        rule.onNodeWithTag("copy_mode_EXAM").performClick()
        rule.onNodeWithTag("copy_start").performClick()
        waitForTag("copy_check")
        val passage = player.played.last()
        assertTrue(passage.length >= 200)
        rule.onNodeWithTag("copy_input").performTextInput(passage)
        rule.onNodeWithTag("copy_check").performClick()
        rule.onNodeWithTag("copy_accuracy").assertTextContains("100%")
        assertTrue(rule.onAllNodesWithText("0 errors · Pass").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun keyTapsAreDecoded() {
        rule.onNodeWithTag("morse_send").performClick()
        // At 20 WPM a dit is 60 ms: dit, letter gap, dah = "ET".
        rule.onNodeWithTag("send_key").performTouchInput {
            down(center); advanceEventTime(60); up()
            advanceEventTime(200)
            down(center); advanceEventTime(200); up()
        }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithTag("send_decoded").fetchSemanticsNodes().firstOrNull()
                ?.config?.get(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString()?.startsWith("ET") == true
        }
        assertEquals(2, player.keyDowns)
        rule.onNodeWithTag("send_clear").performClick()
        rule.onNodeWithTag("send_hear").performClick()
        rule.waitUntil(5_000) { player.played.isNotEmpty() }
        rule.onNodeWithTag("send_target").assertTextContains(player.played.last())
    }

    @Test fun alphabetHasSeparatePagesAndPlaysTappedCharacter() {
        rule.onNodeWithTag("morse_alphabet").performClick()
        // Letters page: letters only.
        rule.onNodeWithTag("alpha_${'K'.code}").performClick()
        rule.waitUntil(5_000) { player.played.isNotEmpty() }
        assertEquals("K", player.played.last())
        assertEquals(0, rule.onAllNodesWithTag("alpha_${'7'.code}").fetchSemanticsNodes().size)

        rule.onNodeWithTag("alpha_tab_1").performClick()
        rule.onNodeWithTag("alpha_${'7'.code}").assertIsDisplayed()
        rule.onNodeWithTag("alpha_${'7'.code}").performClick()
        rule.waitUntil(5_000) { player.played.last() == "7" }
        assertEquals(0, rule.onAllNodesWithTag("alpha_${'K'.code}").fetchSemanticsNodes().size)

        rule.onNodeWithTag("alpha_tab_2").performClick()
        rule.onNodeWithTag("alpha_${'='.code}").assertIsDisplayed()
        rule.onNodeWithTag("alpha_${'='.code}").performClick()
        rule.waitUntil(5_000) { player.played.last() == "=" }
        assertEquals(0, rule.onAllNodesWithTag("alpha_${'7'.code}").fetchSemanticsNodes().size)
    }

    @Test fun speedSettingsSave() {
        rule.onNodeWithTag("scroll_morse").performScrollToNode(hasTestTag("speed_char"))
        rule.onNodeWithTag("speed_char").performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        rule.waitUntil(5_000) { runBlocking { app.container.settings.settings.first().charWpm } == 25 }
        rule.onNodeWithTag("speed_char_value").assertTextContains("25 WPM")
        rule.onNodeWithTag("scroll_morse").performScrollToNode(hasTestTag("tone_test"))
        rule.onNodeWithTag("tone_test").performClick()
        rule.waitUntil(5_000) { player.played.lastOrNull() == "VVV" }
    }
}
