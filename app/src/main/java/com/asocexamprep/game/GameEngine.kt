package com.asocexamprep.game

import com.asocexamprep.content.ContentRepository
import com.asocexamprep.data.db.AchievementRow
import com.asocexamprep.data.db.AppDatabase
import com.asocexamprep.data.db.StepBest
import com.asocexamprep.data.db.XpEvent
import com.asocexamprep.data.settings.SettingsStore
import com.asocexamprep.domain.currentStreak
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/** Something worth celebrating on screen. */
sealed interface Celebration {
    data class XpGained(val amount: Int, val label: String) : Celebration
    data class LevelUp(val level: Int, val rank: String) : Celebration
    data class BadgeEarned(val badge: Badge) : Celebration
}

/** Awards XP, stars and badges for what the learner does, and announces them. */
class GameEngine(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val content: ContentRepository,
) {
    private val dao get() = db.game()
    private val lock = Mutex()
    private val _celebrations = MutableSharedFlow<Celebration>(extraBufferCapacity = 32)
    val celebrations: SharedFlow<Celebration> = _celebrations

    private fun epochDay() = LocalDate.now().toEpochDay()
    private fun dayStartMillis() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Pays [amount] XP once for [reason]; announces it (unless [quiet]) and any level-up. */
    private suspend fun award(amount: Int, reason: String, label: String, quiet: Boolean = false): Boolean {
        if (amount <= 0) return false
        val before = dao.totalXpNow()
        if (dao.insertXp(XpEvent(at = System.currentTimeMillis(), amount = amount, reason = reason)) == -1L) return false
        if (!quiet) _celebrations.tryEmit(Celebration.XpGained(amount, label))
        val (was, now) = Levels.levelFor(before) to Levels.levelFor(before + amount)
        if (now > was) _celebrations.tryEmit(Celebration.LevelUp(now, Levels.rank(now)))
        return true
    }

    /** First time a topic is studied: XP, and its study minutes count toward today. */
    suspend fun topicStudied(topicId: String, minutes: Int = 0) = lock.withLock {
        if (award(Xp.TOPIC, "topic:$topicId", "Topic studied") && minutes > 0) {
            db.activity().add(epochDay(), "study", minutes)
        }
        refreshLocked()
    }

    /** Small, capped XP for a genuine practice session. Returns the XP paid. */
    private suspend fun practice(accuracy: Float): Int {
        if (accuracy < Xp.PRACTICE_MIN_ACCURACY) return 0
        val day = epochDay()
        val done = dao.countReasons("practice:$day:")
        if (done >= Xp.PRACTICE_DAILY_CAP) return 0
        return if (award(Xp.PRACTICE, "practice:$day:$done", "Practice", quiet = true)) Xp.PRACTICE else 0
    }

    /** A Learn session finished. Returns (stars this time, XP earned). */
    suspend fun learnResult(step: Int, accuracy: Float): Pair<Int, Int> = lock.withLock {
        var xp = practice(accuracy)
        val best = dao.stepBest(step)
        val gained = Xp.newStars(best?.accuracy, accuracy)
        if (gained > 0) {
            val total = stars(accuracy)
            if (award(Xp.STAR * gained, "stars:$step:$total", if (gained == 1) "New star" else "$gained new stars")) xp += Xp.STAR * gained
        }
        if (best == null || accuracy > best.accuracy) dao.upsertStepBest(StepBest(step, accuracy, System.currentTimeMillis()))
        refreshLocked()
        stars(accuracy) to xp
    }

    suspend fun stepUnlocked(newStep: Int) = lock.withLock {
        award(Xp.STEP_UNLOCK, "step:$newStep", "New step unlocked")
        refreshLocked()
    }

    suspend fun copyResult(accuracy: Float, examPassed: Boolean) = lock.withLock {
        practice(accuracy)
        if (examPassed) award(Xp.EXAM_PASS, "exam:${epochDay()}", "Exam test passed")
        refreshLocked()
    }

    /** A topic or review quiz finished. */
    suspend fun quizResult(accuracy: Float) = lock.withLock {
        practice(accuracy)
        refreshLocked()
    }

    /** A mock exam was submitted. */
    suspend fun mockResult(accuracy: Float, passed: Boolean) = lock.withLock {
        practice(accuracy)
        if (passed) award(Xp.MOCK_PASS, "mock:${epochDay()}", "Mock exam passed")
        refreshLocked()
    }

    suspend fun sendCorrect() = lock.withLock {
        practice(1f)
        refreshLocked()
    }

    /** Per character (sent, correct) from the last 40 Learn and copy sessions. */
    suspend fun charStats(): Map<Char, Pair<Int, Int>> {
        val out = HashMap<Char, Pair<Int, Int>>()
        db.progress().recentMorseNow(40).filter { it.mode == "learn" || it.mode.startsWith("copy") }.forEach { s ->
            runCatching { kotlinx.serialization.json.Json.decodeFromString<Map<String, List<Int>>>(s.perCharStats) }.getOrNull()?.forEach { (k, v) ->
                val c = k.firstOrNull() ?: return@forEach
                val (a, b) = out[c] ?: (0 to 0)
                out[c] = (a + v[0]) to (b + v[1])
            }
        }
        return out
    }

    suspend fun weakChars(): List<Char> = weakChars(charStats())

    /** Re-checks the daily goal, quests and badges (e.g. on app start). */
    suspend fun refresh() = lock.withLock { refreshLocked() }

    suspend fun todayStats(): Today {
        val s = settings.settings.first()
        val since = dayStartMillis()
        val sessions = db.progress().morseSince(since)
        return Today(
            topicsStudied = db.progress().studiedSince(since),
            learnPasses = sessions.count { it.mode == "learn" && it.accuracy >= 0.9f },
            copySessions = sessions.count { it.mode.startsWith("copy") },
            sendsCorrect = sessions.count { it.mode == "send" },
            reviews = sessions.count { it.mode == "learn" && it.kochLevel < s.trainerStep },
            minutes = db.activity().minutesOnNow(epochDay()),
            goalMinutes = s.dailyMinutes,
        )
    }

    suspend fun stats(): Stats {
        val s = settings.settings.first()
        val pack = content.pack
        val topics = pack.unitsFor(s.grade.name).flatMap { u -> u.topics.map { u.section to it.id } }
        val studied = db.progress().studiedIds().toSet()
        fun sectionDone(sec: String) = topics.filter { it.first == sec }.let { l -> l.isNotEmpty() && l.all { it.second in studied } }
        val xp = dao.totalXpNow()
        return Stats(
            topicsStudied = topics.count { it.second in studied },
            topicsTotal = topics.size,
            sectionADone = sectionDone("A"),
            sectionBDone = sectionDone("B"),
            trainerStep = s.trainerStep,
            perfectDrills = db.progress().countMorse("learn", 1f),
            sendsCorrect = db.progress().countMorse("send", 0f),
            examPasses = dao.countReasons("exam:"),
            mockPasses = dao.countReasons("mock:"),
            streak = currentStreak(db.activity().activeDaysNow(), epochDay()),
            goalDays = dao.countReasons("goal:"),
            threeStarSteps = dao.threeStarCount(),
            level = Levels.levelFor(xp),
        )
    }

    private suspend fun refreshLocked() {
        val day = epochDay()
        val t = todayStats()
        if (t.minutes >= t.goalMinutes) award(Xp.DAILY_GOAL, "goal:$day", "Daily goal met")
        Quests.forDay(day).filter { it.done(t) }.forEach { award(Xp.QUEST, "quest:$day:${it.id}", "Quest: ${it.title}") }
        val have = dao.achievementIds().toSet()
        val st = stats()
        Badges.all.filter { it.id !in have && it.earned(st) }.forEach { b ->
            if (dao.insertAchievement(AchievementRow(b.id, System.currentTimeMillis())) != -1L) _celebrations.tryEmit(Celebration.BadgeEarned(b))
        }
    }
}
