package com.asocexamprep

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private lateinit var player: FakeMorsePlayer

    @Before fun setUp() {
        player = resetForTest()
        rule.onNodeWithTag("tab_exam").performClick()
        rule.onNodeWithTag("screen_exam").assertIsDisplayed()
    }

    private val pack get() = app.container.content.pack
    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
    private fun currentQuestion() = rule.onNodeWithTag("quiz_stem").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        .let { id -> pack.questions.first { it.id == id } }
    private fun choose(i: Int) = click("opt_$i")
    /** Scrolls [tag] into view when it sits in a scrolling column, then taps it. */
    private fun click(tag: String) {
        runCatching { rule.onNodeWithTag(tag).performScrollTo() }
        rule.onNodeWithTag(tag).performClick()
    }

    @Test fun everyExamOptionOpens() {
        rule.onNodeWithTag("exam_rules").assertTextContains("50% in each section and 60% overall", substring = true)
        click("exam_mock"); rule.onNodeWithTag("screen_mock").assertIsDisplayed(); click("back")
        click("exam_paper_B"); rule.onNodeWithTag("screen_mock").assertIsDisplayed(); click("back")
        click("exam_morse"); rule.onNodeWithTag("screen_morse_test").assertIsDisplayed(); click("back")
        click("exam_practice"); rule.onNodeWithTag("screen_practice_topics").assertIsDisplayed(); click("back")
        click("exam_review"); rule.onNodeWithTag("quiz_empty").assertExists()
    }

    @Test fun practiceExplainsAnswersAndQueuesMistakes() {
        click("exam_practice")
        rule.onNodeWithTag("scroll_practice_topics").performScrollToNode(hasTestTag("practice_b-phonetics-codes"))
        click("practice_b-phonetics-codes")
        rule.waitUntil(5_000) { exists("quiz_stem") }
        var wrongId = ""
        for (n in 0 until 10) {
            rule.waitUntil(5_000) { exists("quiz_count") && text("quiz_count").startsWith("${n + 1} of") }
            val q = currentQuestion()
            assertEquals("b-phonetics-codes", q.topic)
            if (n == 0) {
                click("quiz_hint")
                rule.onNodeWithTag("quiz_hint_panel").assertExists()
                wrongId = q.id
                choose((q.answer + 1) % 4)
                rule.onNodeWithTag("quiz_feedback").assertTextContains("The answer is ${'A' + q.answer}", substring = true)
                // Every answer names the published bank it comes from.
                val src = pack.source(q.source)!!.title
                rule.onNodeWithTag("quiz_source").assertTextContains("Source: $src", substring = true)
                rule.onNodeWithTag("opt_${q.answer}").assertExists()
            } else {
                choose(q.answer)
                rule.onNodeWithTag("quiz_feedback").assertTextContains("Correct")
            }
            click("quiz_next")
        }
        rule.waitUntil(5_000) { exists("quiz_score") }
        rule.onNodeWithTag("quiz_score").assertTextContains("9 / 10")
        rule.waitUntil(5_000) { runBlocking { app.container.db.progress().openMistakes().first().map { it.cardId } } == listOf(wrongId) }

        // The mistake comes back in Review; answering it right moves it on.
        click("quiz_done")
        click("back")
        rule.waitUntil(5_000) { text("exam_review").contains("1 due now") || exists("exam_review") }
        click("exam_review")
        rule.waitUntil(5_000) { exists("quiz_stem") }
        val q = currentQuestion()
        assertEquals(wrongId, q.id)
        choose(q.answer)
        click("quiz_next")
        rule.onNodeWithTag("quiz_score").assertTextContains("1 / 1")
        rule.waitUntil(5_000) { runBlocking { app.container.db.progress().card(wrongId)?.repetitions } == 1 }
    }

    private fun paperQuestion(n: Int): com.asocexamprep.content.Question {
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_$n"))
        val id = rule.onNodeWithTag("mq_stem_$n").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        return pack.questions.first { it.id == id }
    }

    private fun pick(n: Int, option: Int) {
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_${n}_opt_$option"))
        rule.onNodeWithTag("mq_${n}_opt_$option").performClick()
    }

    private fun state(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config.getOrElse(SemanticsProperties.StateDescription) { "" }

    /** Answers a paper on its single page: [answerOf] gives the option to pick, or null to skip. */
    private fun runPaper(open: String, count: Int, answerOf: (com.asocexamprep.content.Question) -> Int?) {
        click(open)
        click("mock_start")
        rule.waitUntil(5_000) { exists("paper_list") }
        for (n in 0 until count) {
            val q = paperQuestion(n)
            answerOf(q)?.let { pick(n, it) }
        }
        rule.onNodeWithTag("mock_submit").performClick()
        rule.onNodeWithTag("mock_confirm").performClick()
        rule.waitUntil(5_000) { exists("mock_result") }
    }

    @Test fun aPerfectMockPassesAndIsSaved() {
        runPaper("exam_mock", 100) { it.answer }
        rule.onNodeWithTag("mock_result").assertTextContains("Pass")
        rule.onNodeWithTag("mock_section_A").assertTextContains("50 / 50", substring = true)
        rule.onNodeWithTag("mock_section_B").assertTextContains("50 / 50", substring = true)
        rule.onNodeWithTag("mock_hints").assertTextContains("No hints used")
        rule.waitUntil(5_000) { runBlocking { app.container.db.progress().exams().first() }.size == 1 }
        rule.waitUntil(5_000) { "mock-pass" in runBlocking { app.container.db.game().achievementIds() } }
        assertTrue(exists("mock_morse")) // General grade goes on to the Morse test
        click("mock_done")
        rule.waitUntil(5_000) { exists("exam_last") }
        rule.onNodeWithTag("exam_last").assertTextContains("Last paper (Full): 100% · Pass")
    }

    @Test fun aMockWithSectionBBlankFails() {
        runPaper("exam_mock", 100) { q -> if (q.section == "A") q.answer else null }
        rule.onNodeWithTag("mock_result").assertTextContains("Not yet")
        rule.onNodeWithTag("mock_section_B").assertTextContains("0 / 50", substring = true)
        rule.waitUntil(10_000) { runBlocking { app.container.db.progress().openMistakes().first() }.size == 50 }
    }

    @Test fun rulesPaperIsSectionBOnly() {
        click("exam_paper_B")
        click("mock_start")
        rule.waitUntil(5_000) { exists("paper_list") }
        rule.onNodeWithTag("mock_timer").assertTextContains("1:00:00", substring = true)
        for (n in 0 until 50) assertEquals("B", paperQuestion(n).section)
        assertEquals(0, rule.onAllNodesWithTag("mq_stem_50").fetchSemanticsNodes().size)
        rule.onNodeWithTag("mock_submit").performClick()
        rule.onNodeWithTag("mock_confirm").performClick()
        rule.waitUntil(5_000) { exists("mock_result") }
        rule.onNodeWithTag("mock_section_B").assertTextContains("0 / 50", substring = true)
        assertEquals(0, rule.onAllNodesWithTag("mock_section_A").fetchSemanticsNodes().size)
    }

    @Test fun liveResultsMarkPicksAndHintsAreCounted() {
        click("exam_paper_B")
        click("mock_start")
        rule.waitUntil(5_000) { exists("paper_list") }
        val q = paperQuestion(0)
        val wrong = (q.answer + 1) % 4
        // Off: a pick is only highlighted, not judged.
        pick(0, wrong)
        assertEquals("", state("mq_0_opt_$wrong"))
        // On: the pick shows red and the right answer green.
        rule.onNodeWithTag("live_toggle").performClick()
        rule.waitUntil(5_000) { state("mq_0_opt_$wrong") == "Your answer, wrong" }
        assertEquals("Correct answer", state("mq_0_opt_${q.answer}"))
        val q1 = paperQuestion(1)
        pick(1, q1.answer)
        rule.waitUntil(5_000) { state("mq_1_opt_${q1.answer}") == "Correct answer" }
        // Hints open a passage from the notes (or say there is none) and are counted.
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_hint_2"))
        rule.onNodeWithTag("mq_hint_2").performClick()
        rule.onNodeWithTag("mq_hint_2_panel").assertExists()
        // Scrolled far away and back, the hint is still open.
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_30"))
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_hint_2_panel"))
        rule.onNodeWithTag("mq_hint_2_panel").assertExists()
        rule.onNodeWithTag("live_toggle").performClick() // back off for later tests
        rule.waitUntil(5_000) { !runBlocking { app.container.settings.settings.first().liveResults } }
        rule.onNodeWithTag("mock_submit").performClick()
        rule.onNodeWithTag("mock_confirm").performClick()
        rule.waitUntil(5_000) { exists("mock_hints") }
        rule.onNodeWithTag("mock_hints").assertTextContains("Hints used: 1")
    }

    /** Keys [text] on [tag] with beginner finger timing (150 ms dots, 450 ms dashes, long word gaps). */
    private fun keyText(tag: String, text: String) {
        rule.onNodeWithTag(tag).performTouchInput {
            var first = true
            text.forEach { ch ->
                if (ch == ' ') { advanceEventTime(1800); return@forEach }
                if (!first) advanceEventTime(900)
                first = false
                com.asocexamprep.morse.MorseCode.pattern(ch)!!.forEachIndexed { si, sym ->
                    if (si > 0) advanceEventTime(200)
                    down(center); advanceEventTime(if (sym == '.') 150 else 450); up()
                }
            }
        }
    }

    @Test fun morseTestReceiveAndSend() {
        click("exam_morse")
        click("morse_test_start")
        click("rx_play")
        rule.waitUntil(5_000) { player.played.isNotEmpty() }
        val sent = player.played.last()
        assertTrue(sent.length >= 200)
        rule.onNodeWithTag("rx_input").performTextInput(sent)
        click("rx_check")
        rule.onNodeWithTag("rx_result").assertTextContains("0 errors · pass", substring = true)
        click("rx_next")
        val passage = text("tx_text")
        keyText("tx_key", passage)
        rule.waitUntil(10_000) { text("tx_decoded").replace(" ", "").length >= passage.replace(" ", "").length - 1 }
        click("tx_finish")
        rule.onNodeWithTag("morse_test_result").assertTextContains("Pass")
        rule.onNodeWithTag("tx_result").assertTextContains("pass", substring = true)
    }

    @Test fun topicPageOffersPractice() {
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("scroll_study").performScrollToNode(hasTestTag("topic_b-phonetics-codes"))
        rule.onNodeWithTag("topic_b-phonetics-codes").performClick()
        click("topic_practice")
        rule.waitUntil(5_000) { exists("quiz_stem") }
        assertEquals("b-phonetics-codes", currentQuestion().topic)
    }
}
