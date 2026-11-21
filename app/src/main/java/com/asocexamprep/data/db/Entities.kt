package com.asocexamprep.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Study state of one syllabus topic. */
@Entity(tableName = "topic_progress")
data class TopicProgress(
    @PrimaryKey val topicId: String,
    val studied: Boolean = false,
    /** 0..1 share of the topic's notes scrolled through. */
    val notesRead: Float = 0f,
    val pdfSectionsDone: Int = 0,
    val lastSeenAt: Long = 0,
)

/** Reading state of one PDF (bundled or user-imported), keyed by content hash. */
@Entity(tableName = "pdf_progress")
data class PdfProgress(
    @PrimaryKey val sourceId: String,
    /** Comma-separated 0-based page indices viewed. */
    val pagesViewed: String = "",
    /** Comma-separated topic ids whose mapped section is marked done. */
    val sectionsDone: String = "",
    val lastPage: Int = 0,
    val updatedAt: Long = 0,
)

@Entity(
    tableName = "question_attempt",
    indices = [Index("questionId"), Index("answeredAt")],
)
data class QuestionAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val topicId: String,
    val chosen: Int,
    val correct: Boolean,
    val millis: Long,
    /** practice | mock | review | srs */
    val mode: String,
    val examAttemptId: Long? = null,
    val answeredAt: Long,
)

/** Spaced-repetition schedule for a question or flashcard. */
@Entity(tableName = "srs_card", indices = [Index("dueAt")])
data class SrsCard(
    @PrimaryKey val cardId: String,
    val ease: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = 0,
)

@Entity(tableName = "morse_session", indices = [Index("startedAt")])
data class MorseSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** koch | copy | send | exam */
    val mode: String,
    val charWpm: Int,
    val effectiveWpm: Int,
    val accuracy: Float,
    /** JSON map char -> {sent, correct}. */
    val perCharStats: String = "{}",
    val kochLevel: Int = 0,
    val durationMillis: Long,
    val startedAt: Long,
)

@Entity(tableName = "exam_attempt", indices = [Index("startedAt")])
data class ExamAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: String,
    val grade: String,
    /** JSON map section -> {score, max}. */
    val sectionScores: String,
    val passed: Boolean,
    val durationMillis: Long,
    val startedAt: Long,
)

/** Minutes spent per area on one day (epoch day in local time). */
@Entity(tableName = "daily_activity", primaryKeys = ["epochDay", "area"])
data class DailyActivity(
    val epochDay: Long,
    /** study | pdf | morse | exam */
    val area: String,
    val minutes: Int,
)

/** One XP award. [reason] is unique, so the same award can never be paid twice. */
@Entity(tableName = "xp_event", indices = [Index(value = ["reason"], unique = true), Index("at")])
data class XpEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val amount: Int,
    val reason: String,
)

@Entity(tableName = "achievement")
data class AchievementRow(@PrimaryKey val id: String, val unlockedAt: Long)

/** Best accuracy reached on a Learn step (drives its stars). */
@Entity(tableName = "step_best")
data class StepBest(@PrimaryKey val step: Int, val accuracy: Float, val at: Long)
