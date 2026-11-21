package com.asocexamprep.exam

import com.asocexamprep.content.ContentPack
import com.asocexamprep.content.Question
import com.asocexamprep.data.db.AppDatabase
import com.asocexamprep.data.db.ExamAttempt
import com.asocexamprep.data.db.QuestionAttempt
import com.asocexamprep.data.db.SrsCard
import kotlinx.serialization.json.Json

/** Records answers and mock exams, and keeps the spaced-repetition queue of mistakes. */
class ExamStore(private val db: AppDatabase) {
    private val dao get() = db.progress()

    suspend fun answer(q: Question, chosen: Int, mode: String, millis: Long = 0, examId: Long? = null) {
        val now = System.currentTimeMillis()
        val ok = chosen == q.answer
        dao.insertAttempt(QuestionAttempt(questionId = q.id, topicId = q.topic, chosen = chosen, correct = ok, millis = millis, mode = mode, examAttemptId = examId, answeredAt = now))
        // Only mistakes enter the review queue; an existing card moves on with each answer.
        val card = dao.card(q.id)
        if (!ok || card != null) {
            val prev = card?.let { Srs.Card(it.intervalDays, it.repetitions, it.dueAt) }
            // A second right answer on the same day doesn't count toward clearing it.
            val sameDay = card != null && ok && card.repetitions > 0 && now < card.dueAt
            if (!sameDay) {
                val n = Srs.next(prev, ok, now)
                dao.upsertCard(SrsCard(q.id, card?.ease ?: 2.5f, n.intervalDays, n.repetitions, n.dueAt))
            }
        }
    }

    suspend fun saveMock(grade: String, score: ExamScore, startedAt: Long, examId: String = "asoc-in:full"): Long {
        val json = Json.encodeToString(score.sections.associate { it.section to listOf(it.correct, it.total) })
        return dao.insertExam(ExamAttempt(examId = examId, grade = grade, sectionScores = json, passed = score.passed, durationMillis = System.currentTimeMillis() - startedAt, startedAt = startedAt))
    }

    /** Mistakes due for review now, as questions (newest-due first is fine; capped at [limit]). */
    suspend fun dueMistakes(pack: ContentPack, limit: Int = 15): List<Question> {
        val byId = pack.questions.associateBy { it.id }
        return dao.dueMistakes(System.currentTimeMillis()).mapNotNull { byId[it.cardId] }.take(limit)
    }
}
