package com.radioprep.domain

/**
 * Consecutive active days ending today, or ending yesterday when today has no
 * activity yet (the streak is still alive until the day is over).
 * [activeDays] are epoch days with any activity, in any order.
 */
fun currentStreak(activeDays: Collection<Long>, today: Long): Int {
    val days = activeDays.toHashSet()
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) { streak++; day-- }
    return streak
}

/** Days left until the exam (0 on exam day), or null if no date or it has passed. */
fun daysUntil(examDay: Long?, today: Long): Long? =
    examDay?.let { it - today }?.takeIf { it >= 0 }
