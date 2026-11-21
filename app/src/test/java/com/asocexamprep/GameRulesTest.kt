package com.asocexamprep

import com.asocexamprep.game.Badges
import com.asocexamprep.game.Levels
import com.asocexamprep.game.Quests
import com.asocexamprep.game.Stats
import com.asocexamprep.game.Today
import com.asocexamprep.game.Xp
import com.asocexamprep.game.stars
import com.asocexamprep.game.weakChars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    @Test fun levelsFollowTheCurve() {
        assertEquals(1, Levels.levelFor(0))
        assertEquals(1, Levels.levelFor(99))
        assertEquals(2, Levels.levelFor(100))
        assertEquals(3, Levels.levelFor(300))
        assertEquals(4, Levels.levelFor(600))
        assertEquals(0.5f, Levels.progress(200), 1e-6f)
        assertEquals("Listener", Levels.rank(1))
        assertEquals("Operator", Levels.rank(5))
        assertEquals("Elmer", Levels.rank(40))
    }

    @Test fun starsByAccuracy() {
        assertEquals(0, stars(0.89f))
        assertEquals(1, stars(0.9f))
        assertEquals(2, stars(0.95f))
        assertEquals(3, stars(1f))
    }

    @Test fun xpRewardsProgressNotRepetition() {
        assertEquals(3, Xp.newStars(null, 1f))
        assertEquals(1, Xp.newStars(0.9f, 0.95f))
        assertEquals(0, Xp.newStars(1f, 1f)) // replaying a mastered step earns no star XP
        assertEquals(0, Xp.newStars(0.95f, 0.5f))
        assertTrue(Xp.STEP_UNLOCK > Xp.PRACTICE && Xp.STAR > Xp.PRACTICE)
    }

    @Test fun weakCharactersNeedEnoughTriesAndErrors() {
        val stats = mapOf('E' to (10 to 4), 'T' to (10 to 9), 'K' to (3 to 0), 'S' to (8 to 5))
        assertEquals(listOf('E', 'S'), weakChars(stats))
    }

    @Test fun badgesUnlockFromStats() {
        val none = Badges.all.filter { it.earned(Stats()) }
        assertTrue(none.isEmpty())
        val some = Badges.all.filter { it.earned(Stats(topicsStudied = 1, trainerStep = 26, streak = 3)) }.map { it.id }.toSet()
        assertEquals(setOf("first-topic", "first-char", "ten-chars", "letters", "streak-3"), some)
        assertEquals(Badges.all.size, Badges.all.map { it.id }.toSet().size)
    }

    @Test fun dailyQuestsAreStableForADay() {
        val q1 = Quests.forDay(20000).map { it.id }
        assertEquals(q1, Quests.forDay(20000).map { it.id })
        assertEquals(3, q1.size)
        assertNotEquals((20000L..20010L).map { Quests.forDay(it).map { q -> q.id } }.toSet().size, 1)
        val topic = Quests.forDay(0).first()
        assertFalse(topic.done(Today()))
        assertTrue(Quests.forDay(0).all { it.done(Today(topicsStudied = 2, learnPasses = 1, copySessions = 1, sendsCorrect = 1, minutes = 30)) })
    }
}
