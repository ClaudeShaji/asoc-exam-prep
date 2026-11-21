package com.asocexamprep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert suspend fun upsertTopic(p: TopicProgress)
    @Query("SELECT * FROM topic_progress") fun topics(): Flow<List<TopicProgress>>
    @Query("SELECT * FROM topic_progress WHERE topicId = :id") fun topic(id: String): Flow<TopicProgress?>
    @Query("SELECT topicId FROM topic_progress WHERE studied = 1") suspend fun studiedIds(): List<String>
    @Query("SELECT COUNT(*) FROM topic_progress WHERE studied = 1 AND lastSeenAt >= :since") suspend fun studiedSince(since: Long): Int
    @Query("SELECT * FROM morse_session WHERE startedAt >= :since") suspend fun morseSince(since: Long): List<MorseSession>
    @Query("SELECT COUNT(*) FROM morse_session WHERE mode = :mode AND accuracy >= :minAccuracy") suspend fun countMorse(mode: String, minAccuracy: Float): Int

    @Upsert suspend fun upsertPdf(p: PdfProgress)
    @Query("SELECT * FROM pdf_progress WHERE sourceId = :id") suspend fun pdf(id: String): PdfProgress?

    @Insert suspend fun insertAttempt(a: QuestionAttempt): Long
    @Query("SELECT COUNT(*) FROM question_attempt") fun attemptCount(): Flow<Int>

    @Upsert suspend fun upsertCard(c: SrsCard)
    @Query("SELECT COUNT(*) FROM srs_card WHERE dueAt <= :now") fun dueCount(now: Long): Flow<Int>

    @Insert suspend fun insertMorse(s: MorseSession): Long
    @Query("SELECT * FROM morse_session ORDER BY startedAt DESC LIMIT :limit")
    fun recentMorse(limit: Int): Flow<List<MorseSession>>

    @Insert suspend fun insertExam(e: ExamAttempt): Long
    @Query("SELECT * FROM exam_attempt ORDER BY startedAt DESC") fun exams(): Flow<List<ExamAttempt>>
}

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(a: DailyActivity): Long

    @Query("UPDATE daily_activity SET minutes = minutes + :minutes WHERE epochDay = :day AND area = :area")
    suspend fun increment(day: Long, area: String, minutes: Int)

    /** Adds minutes to a day/area, creating the row if needed. */
    suspend fun add(day: Long, area: String, minutes: Int) {
        if (insertIgnore(DailyActivity(day, area, minutes)) == -1L) increment(day, area, minutes)
    }

    @Query("SELECT DISTINCT epochDay FROM daily_activity WHERE minutes > 0 ORDER BY epochDay DESC")
    fun activeDays(): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM daily_activity WHERE epochDay = :day")
    fun minutesOn(day: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM daily_activity WHERE epochDay = :day")
    suspend fun minutesOnNow(day: Long): Int

    @Query("SELECT DISTINCT epochDay FROM daily_activity WHERE minutes > 0")
    suspend fun activeDaysNow(): List<Long>
}

@Dao
interface GameDao {
    /** Returns -1 when this reason was already awarded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertXp(e: XpEvent): Long
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_event") fun totalXp(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_event") suspend fun totalXpNow(): Int
    @Query("SELECT COUNT(*) FROM xp_event WHERE reason LIKE :prefix || '%'") suspend fun countReasons(prefix: String): Int
    @Query("SELECT COUNT(*) FROM xp_event WHERE reason = :reason") suspend fun hasReason(reason: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAchievement(a: AchievementRow): Long
    @Query("SELECT * FROM achievement") fun achievements(): Flow<List<AchievementRow>>
    @Query("SELECT id FROM achievement") suspend fun achievementIds(): List<String>

    @Query("SELECT * FROM step_best") fun stepBests(): Flow<List<StepBest>>
    @Query("SELECT * FROM step_best WHERE step = :step") suspend fun stepBest(step: Int): StepBest?
    @Upsert suspend fun upsertStepBest(b: StepBest)
    @Query("SELECT COUNT(*) FROM step_best WHERE accuracy >= 1.0") suspend fun threeStarCount(): Int
}
