package com.radioprep

import com.radioprep.domain.currentStreak
import com.radioprep.domain.daysUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreaksTest {
    @Test fun emptyIsZero() = assertEquals(0, currentStreak(emptyList(), 100))
    @Test fun countsBackFromToday() = assertEquals(3, currentStreak(listOf(100, 99, 98, 96), 100))
    @Test fun yesterdayKeepsStreakAlive() = assertEquals(2, currentStreak(listOf(99, 98), 100))
    @Test fun gapOfTwoDaysBreaks() = assertEquals(0, currentStreak(listOf(98, 97), 100))
    @Test fun orderDoesNotMatter() = assertEquals(3, currentStreak(listOf(98, 100, 99), 100))

    @Test fun daysUntilExam() {
        assertEquals(5L, daysUntil(105, 100))
        assertEquals(0L, daysUntil(100, 100))
        assertNull(daysUntil(99, 100))
        assertNull(daysUntil(null, 100))
    }
}
