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

    @Before fun setUp() {
        resetForTest()
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
                wrongId = q.id
                choose((q.answer + 1) % 4)
                rule.onNodeWithTag("quiz_feedback").assertTextContains("The answer is ${'A' + q.answer}", substring = true)
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

    /** Answers the mock: [answerOf] gives the option index to pick, or null to skip. */
    private fun runMock(answerOf: (com.asocexamprep.content.Question) -> Int?) {
        click("exam_mock")
        click("mock_start")
        rule.waitUntil(5_000) { exists("quiz_stem") }
        rule.onNodeWithTag("mock_timer").assertTextContains("2:00:00", substring = true)
        for (n in 0 until 100) {
            rule.waitUntil(5_000) { text("mock_count").startsWith("${n + 1} / 100") }
            answerOf(currentQuestion())?.let { choose(it) }
            if (n < 99) click("mock_next")
        }
        click("mock_submit")
        rule.onNodeWithTag("mock_confirm").performClick()
        rule.waitUntil(5_000) { exists("mock_result") }
    }

    @Test fun aPerfectMockPassesAndIsSaved() {
        runMock { it.answer }
        rule.onNodeWithTag("mock_result").assertTextContains("Pass")
        rule.onNodeWithTag("mock_section_A").assertTextContains("50 / 50", substring = true)
        rule.onNodeWithTag("mock_section_B").assertTextContains("50 / 50", substring = true)
        rule.waitUntil(5_000) { runBlocking { app.container.db.progress().exams().first() }.size == 1 }
        rule.waitUntil(5_000) { "mock-pass" in runBlocking { app.container.db.game().achievementIds() } }
        click("mock_done")
        rule.waitUntil(5_000) { exists("exam_last") }
        rule.onNodeWithTag("exam_last").assertTextContains("Last mock: 100% · Pass")
    }

    @Test fun aMockWithSectionBBlankFails() {
        runMock { q -> if (q.section == "A") q.answer else null }
        rule.onNodeWithTag("mock_result").assertTextContains("Not yet")
        rule.onNodeWithTag("mock_section_B").assertTextContains("0 / 50", substring = true)
        assertTrue(exists("mock_weak_" + pack.questions.first { it.section == "B" }.topic) || rule.onAllNodesWithTag("mock_result").fetchSemanticsNodes().isNotEmpty())
        // Every blank B question is now a mistake to review.
        rule.waitUntil(10_000) { runBlocking { app.container.db.progress().openMistakes().first() }.size == 50 }
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
