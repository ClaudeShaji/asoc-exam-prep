package com.asocexamprep.game


/** XP needed for each level: L2 at 100, L3 at 300, L4 at 600 … (50·L·(L−1)). */
object Levels {
    fun xpFor(level: Int): Int = 50 * level * (level - 1)

    fun levelFor(xp: Int): Int {
        var l = 1
        while (xpFor(l + 1) <= xp) l++
        return l
    }

    /** Ham radio ranks, unlocked as levels rise. */
    private val ranks = listOf(1 to "Listener", 3 to "Novice", 5 to "Operator", 8 to "Ragchewer", 12 to "DXer", 16 to "Contester", 20 to "Elmer")
    fun rank(level: Int): String = ranks.last { level >= it.first }.second

    /** 0..1 progress from this level to the next. */
    fun progress(xp: Int): Float {
        val l = levelFor(xp)
        return (xp - xpFor(l)).toFloat() / (xpFor(l + 1) - xpFor(l))
    }
}

/**
 * XP rewards learning, not volume: most XP comes from progress (a topic studied, a new star,
 * the next step unlocked, the exam test passed); practice earns a little, capped each day.
 */
object Xp {
    const val TOPIC = 20
    const val STEP_UNLOCK = 25
    /** Per star newly earned on a Learn step (beating your own best). */
    const val STAR = 10
    const val EXAM_PASS = 50
    const val MOCK_PASS = 50
    const val DAILY_GOAL = 30
    const val QUEST = 20
    /** Any genuine practice session (at least half right), up to [PRACTICE_DAILY_CAP] a day. */
    const val PRACTICE = 5
    const val PRACTICE_DAILY_CAP = 10
    const val PRACTICE_MIN_ACCURACY = 0.5f

    fun newStars(previousBest: Float?, accuracy: Float): Int = (stars(accuracy) - (previousBest?.let { stars(it) } ?: 0)).coerceAtLeast(0)
}

/** 1-3 stars for a session: 90%, 95%, 100%. */
fun stars(accuracy: Float): Int = when {
    accuracy >= 1f -> 3
    accuracy >= 0.95f -> 2
    accuracy >= 0.9f -> 1
    else -> 0
}

/** Everything badges are judged on. */
data class Stats(
    val topicsStudied: Int = 0,
    val topicsTotal: Int = Int.MAX_VALUE,
    val sectionADone: Boolean = false,
    val sectionBDone: Boolean = false,
    val trainerStep: Int = 0,
    val perfectDrills: Int = 0,
    val sendsCorrect: Int = 0,
    val examPasses: Int = 0,
    val mockPasses: Int = 0,
    val streak: Int = 0,
    val goalDays: Int = 0,
    val threeStarSteps: Int = 0,
    val level: Int = 1,
)

data class Badge(val id: String, val title: String, val description: String, val icon: String, val earned: (Stats) -> Boolean)

object Badges {
    private val chars = com.asocexamprep.morse.Trainer.CHAR_ORDER
    private val lastStep = com.asocexamprep.morse.Trainer.steps.lastIndex
    private fun stepOf(stage: com.asocexamprep.morse.Stage) = com.asocexamprep.morse.Trainer.firstOf(stage).index

    val all: List<Badge> = listOf(
        Badge("first-topic", "First steps", "Study your first topic", "book") { it.topicsStudied >= 1 },
        Badge("ten-topics", "Bookworm", "Study 10 topics", "book") { it.topicsStudied >= 10 },
        Badge("section-a", "Theory done", "Study every Section A topic", "bolt") { it.sectionADone },
        Badge("section-b", "Rule keeper", "Study every Section B topic", "gavel") { it.sectionBDone },
        Badge("all-topics", "Scholar", "Study the whole syllabus", "school") { it.topicsStudied >= it.topicsTotal },
        Badge("first-char", "First beep", "Unlock your second character", "graphic") { it.trainerStep >= 1 },
        Badge("ten-chars", "Ten down", "Learn 10 characters", "graphic") { it.trainerStep >= 10 },
        Badge("letters", "Alphabet", "Learn all 26 letters", "abc") { it.trainerStep >= 26 },
        Badge("all-chars", "Full set", "Learn every character, figure and sign", "star") { it.trainerStep >= chars.length },
        Badge("words", "Wordsmith", "Reach the word stage", "chat") { it.trainerStep >= stepOf(com.asocexamprep.morse.Stage.WORDS) },
        Badge("sentences", "Fluent", "Reach the sentence stage", "chat") { it.trainerStep >= lastStep },
        Badge("perfect", "Perfect", "Score 100% in a Learn session", "target") { it.perfectDrills >= 1 },
        Badge("three-stars", "Star collector", "Earn 3 stars on 10 steps", "star") { it.threeStarSteps >= 10 },
        Badge("first-send", "On the key", "Send a call sign or word correctly", "key") { it.sendsCorrect >= 1 },
        Badge("exam-pass", "Exam ready", "Pass the 8 WPM copy test", "verified") { it.examPasses >= 1 },
        Badge("mock-pass", "Paper passed", "Pass a full mock exam", "verified") { it.mockPasses >= 1 },
        Badge("mock-3", "Consistent", "Pass mock exams on 3 days", "verified") { it.mockPasses >= 3 },
        Badge("streak-3", "Warming up", "3-day streak", "fire") { it.streak >= 3 },
        Badge("streak-7", "On air every day", "7-day streak", "fire") { it.streak >= 7 },
        Badge("goal", "Goal met", "Meet your daily goal", "flag") { it.goalDays >= 1 },
        Badge("level-5", "Operator", "Reach level 5", "badge") { it.level >= 5 },
    )
}

/** What happened today, for daily quests. */
data class Today(
    val topicsStudied: Int = 0,
    val learnPasses: Int = 0,
    val copySessions: Int = 0,
    val sendsCorrect: Int = 0,
    val minutes: Int = 0,
    val goalMinutes: Int = 30,
    /** Learn sessions today on steps already passed. */
    val reviews: Int = 0,
)

data class Quest(val id: String, val title: String, val done: (Today) -> Boolean)

object Quests {
    private val pool = listOf(
        Quest("topic", "Study a topic") { it.topicsStudied >= 1 },
        Quest("learn", "Pass a Learn step at 90%") { it.learnPasses >= 1 },
        Quest("copy", "Finish a copy practice") { it.copySessions >= 1 },
        Quest("send", "Send one word correctly") { it.sendsCorrect >= 1 },
        Quest("half-goal", "Practise for half your daily goal") { it.minutes * 2 >= it.goalMinutes },
        Quest("two-topics", "Study two topics") { it.topicsStudied >= 2 },
        Quest("review", "Review an earlier Learn step") { it.reviews >= 1 },
    )

    /** Three quests for the day, the same all day, different on most days. */
    fun forDay(epochDay: Long): List<Quest> = pool.shuffled(kotlin.random.Random(epochDay)).take(3)
}

/** Characters answered wrongly often enough to need extra practice: at least 5 tries, under 80% right. */
fun weakChars(stats: Map<Char, Pair<Int, Int>>, limit: Int = 6): List<Char> = stats
    .filter { (_, v) -> v.first >= 5 && v.second.toFloat() / v.first < 0.8f }
    .entries.sortedBy { (_, v) -> v.second.toFloat() / v.first }
    .take(limit).map { it.key }
