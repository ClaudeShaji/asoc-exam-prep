package com.asocexamprep

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.asocexamprep.game.Celebration
import com.asocexamprep.game.Xp
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var player: FakeMorsePlayer

    @Before fun setUp() { player = resetForTest() }

    private val game get() = app.container.game
    private fun xp() = runBlocking { app.container.db.game().totalXpNow() }
    private fun badges() = runBlocking { app.container.db.game().achievementIds().toSet() }
    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
    private fun state(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = false).fetchSemanticsNode().config.getOrElse(SemanticsProperties.StateDescription) { "" }

    @Test fun studyingATopicPaysXpOnceAndEarnsTheFirstBadge() {
        rule.onNodeWithTag("tab_study").performClick()
        rule.onNodeWithTag("topic_a-atomic").performClick()
        rule.onNodeWithTag("toggle_studied").performClick()
        rule.waitUntil(5_000) { runBlocking { app.container.db.game().hasReason("topic:a-atomic") } == 1 }
        rule.waitUntil(5_000) { "first-topic" in badges() }
        val after = xp() // the topic, plus a quest if today's quests include one
        assertTrue(after >= Xp.TOPIC)
        // Unmark and mark again: no second payment.
        rule.onNodeWithTag("toggle_studied").performClick()
        rule.onNodeWithTag("toggle_studied").performClick()
        rule.waitForIdle()
        Thread.sleep(500)
        assertEquals(after, xp())

        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("tab_progress").performClick()
        rule.onNodeWithTag("level_xp").assertTextContains("$after XP", substring = true)
        rule.onNodeWithTag("scroll_progress").performScrollToNode(hasTestTag("badge_first-topic"))
        assertEquals("Earned", state("badge_first-topic"))
        assertEquals("Locked", state("badge_scholar".replace("scholar", "all-topics")))
        rule.onNodeWithTag("badge_count").assertTextContains("Badges · 1 of", substring = true)
    }

    @Test fun perfectDrillGivesThreeStarsComboXpAndUnlock() {
        rule.onNodeWithTag("tab_morse").performClick()
        rule.onNodeWithTag("morse_learn").performClick()
        val base = player.played.size
        rule.onNodeWithTag("char_start").performClick()
        var sawCombo = false
        for (i in 0 until 10) {
            rule.waitUntil(5_000) { player.played.size >= base + i + 1 && rule.onAllNodesWithTag("char_input").fetchSemanticsNodes().isNotEmpty() }
            rule.waitUntil(5_000) { text("char_count") == "${i + 1} of 10" }
            if (i >= 3) sawCombo = sawCombo || rule.onAllNodesWithTag("char_combo").fetchSemanticsNodes().isNotEmpty()
            rule.onNodeWithTag("char_input").performTextInput(player.played[base + i])
        }
        assertTrue("combo counter shown", sawCombo)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("learn_reward").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("learn_reward").assertTextContains("★★★", substring = true)
        val expected = Xp.PRACTICE + 3 * Xp.STAR + Xp.STEP_UNLOCK
        rule.waitUntil(5_000) { xp() >= expected }
        rule.waitUntil(5_000) { setOf("first-char", "perfect").all { it in badges() } }
        rule.onNodeWithTag("learn_best").assertTextContains("★★★")
        // The map marks step 1 with its stars; the next step is now open.
        rule.onNodeWithTag("step_map").performScrollToNode(hasTestTag("map_1"))
        rule.onNodeWithTag("map_1").performClick()
        rule.waitUntil(5_000) { text("learn_step") == "Character 2 of 43" }
    }

    @Test fun levelUpIsAnnouncedAndShownOnHome() {
        val levelUp = runBlocking {
            val waiter = async { withTimeout(10_000) { game.celebrations.filterIsInstance<Celebration.LevelUp>().first() } }
            // 5 topics × 20 XP = 100 XP = level 2.
            listOf("a-atomic", "a-resistors", "a-capacitors", "a-magnetism", "a-inductors").forEach { game.topicStudied(it) }
            waiter.await()
        }
        assertEquals(2, levelUp.level)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("level_title").fetchSemanticsNodes().isNotEmpty() && text("level_title") == "Level 2 · Listener" }
        assertEquals(3, rule.onAllNodesWithTag("quest_", useUnmergedTree = false).fetchSemanticsNodes().size.let { _ ->
            com.asocexamprep.game.Quests.forDay(java.time.LocalDate.now().toEpochDay()).count { q -> rule.onAllNodesWithTag("quest_${q.id}").fetchSemanticsNodes().isNotEmpty() }
        })
    }

    @Test fun questsCompleteAndPayOut() {
        val day = java.time.LocalDate.now().toEpochDay()
        val quests = com.asocexamprep.game.Quests.forDay(day).map { it.id }
        // Do everything a quest could ask for.
        runBlocking {
            val now = System.currentTimeMillis()
            listOf("a-atomic", "a-resistors").forEach {
                app.container.db.progress().upsertTopic(com.asocexamprep.data.db.TopicProgress(it, studied = true, lastSeenAt = now))
                game.topicStudied(it)
            }
            app.container.db.activity().add(day, "study", 30)
            app.container.db.progress().insertMorse(com.asocexamprep.data.db.MorseSession(mode = "learn", charWpm = 20, effectiveWpm = 8, accuracy = 1f, durationMillis = 1000, startedAt = System.currentTimeMillis()))
            app.container.db.progress().insertMorse(com.asocexamprep.data.db.MorseSession(mode = "copy-words", charWpm = 20, effectiveWpm = 8, accuracy = 1f, durationMillis = 1000, startedAt = System.currentTimeMillis()))
            app.container.db.progress().insertMorse(com.asocexamprep.data.db.MorseSession(mode = "send", charWpm = 20, effectiveWpm = 8, accuracy = 0f, durationMillis = 1000, startedAt = System.currentTimeMillis()))
            game.refresh()
        }
        val paid = runBlocking { quests.map { app.container.db.game().hasReason("quest:$day:$it") } }
        assertEquals(listOf(1, 1, 1), paid)
        assertEquals(1, runBlocking { app.container.db.game().hasReason("goal:$day") })
        rule.waitUntil(5_000) { quests.all { state("quest_$it") == "Done" } }
    }

    @Test fun replayingAMasteredStepEarnsLittleAndPracticeIsCapped() {
        runBlocking {
            val (stars, first) = game.learnResult(0, 1f)
            assertEquals(3, stars)
            assertEquals(Xp.PRACTICE + 3 * Xp.STAR, first)
            val (_, again) = game.learnResult(0, 1f)
            assertEquals(Xp.PRACTICE, again) // no new stars, just practice
            repeat(20) { game.learnResult(0, 1f) }
            val day = java.time.LocalDate.now().toEpochDay()
            assertEquals(Xp.PRACTICE_DAILY_CAP, app.container.db.game().countReasons("practice:$day:"))
            assertEquals(0, game.learnResult(0, 0.3f).second) // a poor session earns nothing
        }
    }

    @Test fun weakCharactersAreShownAndDrilled() {
        runBlocking {
            val stats = """{"E":[10,3],"T":[10,10],"K":[10,9]}"""
            repeat(2) {
                app.container.db.progress().insertMorse(com.asocexamprep.data.db.MorseSession(mode = "learn", charWpm = 20, effectiveWpm = 8, accuracy = 0.7f, perCharStats = stats, durationMillis = 1000, startedAt = System.currentTimeMillis()))
            }
            assertEquals(listOf('E'), game.weakChars())
        }
        rule.onNodeWithTag("tab_home").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("home_weak").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("home_weak").assertTextContains("Needs work: E")
        rule.onNodeWithTag("tab_morse").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("morse_weak").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("morse_weak").performClick()
        rule.onNodeWithTag("copy_start").performClick()
        rule.waitUntil(5_000) { player.played.isNotEmpty() }
        assertTrue(player.played.last(), player.played.last().replace(" ", "").all { it == 'E' })
    }

    @Test fun homeLeadsWithLearningProgress() {
        rule.onNodeWithTag("home_learning").assertExists()
        rule.onNodeWithTag("home_syllabus").assertTextContains("0 of 34 topics", substring = true)
        rule.onNodeWithTag("home_morse").assertTextContains("0 of 43 characters", substring = true)
        runBlocking {
            app.container.db.progress().upsertTopic(com.asocexamprep.data.db.TopicProgress("a-atomic", studied = true, lastSeenAt = System.currentTimeMillis()))
            app.container.settings.setTrainerStep(5)
        }
        rule.waitUntil(5_000) { text("home_syllabus").contains("1 of 34 topics") }
        rule.waitUntil(5_000) { text("home_morse").contains("5 of 43 characters") }
    }
}
