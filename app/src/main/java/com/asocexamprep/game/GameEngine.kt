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

    /** Pays [amount] XP once for [reason]; announces it and any level-up. */
    private suspend fun award(amount: Int, reason: String, label: String): Boolean {
        if (amount <= 0) return false
        val before = dao.totalXpNow()
        if (dao.insertXp(XpEvent(at = System.currentTimeMillis(), amount = amount, reason = reason)) == -1L) return false
        _celebrations.tryEmit(Celebration.XpGained(amount, label))
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

    /** A Learn session finished. Returns the stars earned this time. */
    suspend fun learnResult(step: Int, accuracy: Float, correct: Int = 0, bestCombo: Int = 0, charDrill: Boolean): Int = lock.withLock {
        val xp = if (charDrill) Xp.charDrill(correct, bestCombo) else Xp.textDrill(accuracy)
        award(xp, "learn:${System.nanoTime()}", "Learn session")
        val best = dao.stepBest(step)
        if (best == null || accuracy > best.accuracy) dao.upsertStepBest(StepBest(step, accuracy, System.currentTimeMillis()))
        refreshLocked()
        stars(accuracy)
    }

    suspend fun stepUnlocked(newStep: Int) = lock.withLock {
        award(Xp.STEP_UNLOCK, "step:$newStep", "New step unlocked")
        refreshLocked()
    }

    suspend fun copyResult(accuracy: Float, examPassed: Boolean) = lock.withLock {
        award(Xp.copyPractice(accuracy), "copy:${System.nanoTime()}", "Copy practice")
        if (examPassed) award(Xp.EXAM_PASS, "exam:${System.nanoTime()}", "Exam test passed")
        refreshLocked()
    }

    suspend fun sendCorrect() = lock.withLock {
        award(Xp.SEND_CORRECT, "send:${System.nanoTime()}", "Sent correctly")
        refreshLocked()
    }

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
            sendsCorrect = dao.countReasons("send:"),
            examPasses = dao.countReasons("exam:"),
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
