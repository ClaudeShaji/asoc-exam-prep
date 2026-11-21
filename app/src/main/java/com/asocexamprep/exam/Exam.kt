package com.asocexamprep.exam

import com.asocexamprep.content.ContentPack
import com.asocexamprep.content.ExamGrade
import com.asocexamprep.content.Question
import kotlin.random.Random

/** Score for one section: right answers out of questions asked. */
data class SectionScore(val section: String, val correct: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else Math.round(100f * correct / total)
}

data class ExamScore(val sections: List<SectionScore>, val passed: Boolean, val grade: ExamGrade) {
    val correct get() = sections.sumOf { it.correct }
    val total get() = sections.sumOf { it.total }
    val percent: Int get() = if (total == 0) 0 else Math.round(100f * correct / total)
}

object Exam {
    /** Questions a learner of [grade] may see: General-only topics are left out for Restricted. */
    fun pool(pack: ContentPack, grade: String): List<Question> {
        val allowed = pack.unitsFor(grade).flatMap { it.topics }.map { it.id }.toSet()
        return pack.questions.filter { it.topic in allowed }
    }

    /**
     * A mock paper in the real format: each section's question count from exam.json, spread
     * across that section's topics (round-robin over shuffled topics, so no topic dominates).
     */
    fun mock(pack: ContentPack, grade: String, rnd: Random = Random.Default): List<Question> =
        pack.exam.sections.flatMap { sec ->
            val byTopic = pool(pack, grade).filter { it.section == sec.id }.groupBy { it.topic }
                .mapValues { it.value.shuffled(rnd).toMutableList() }.toList().shuffled(rnd)
            val picked = mutableListOf<Question>()
            while (picked.size < sec.questions && byTopic.any { it.second.isNotEmpty() }) {
                for ((_, qs) in byTopic) if (qs.isNotEmpty() && picked.size < sec.questions) picked += qs.removeAt(0)
            }
            picked.shuffled(rnd)
        }

    /** Practice set: up to [count] questions for a topic id, or a whole section ("A"/"B"). */
    fun practice(pack: ContentPack, grade: String, key: String, count: Int = 10, rnd: Random = Random.Default): List<Question> =
        pool(pack, grade).filter { it.topic == key || it.section == key }.shuffled(rnd).take(count)

    /** Section scores and pass/fail by the grade's per-section and aggregate minimums. */
    fun score(pack: ContentPack, grade: ExamGrade, questions: List<Question>, answers: Map<String, Int>): ExamScore {
        val sections = pack.exam.sections.map { sec ->
            val qs = questions.filter { it.section == sec.id }
            SectionScore(sec.id, qs.count { answers[it.id] == it.answer }, qs.size)
        }.filter { it.total > 0 }
        val correct = sections.sumOf { it.correct }
        val total = sections.sumOf { it.total }
        val passed = total > 0 &&
            sections.all { 100 * it.correct >= grade.minSectionPercent * it.total } &&
            100 * correct >= grade.minAggregatePercent * total
        return ExamScore(sections, passed, grade)
    }
}

/**
 * Spaced repetition for questions: a wrong answer is due again at once; each right answer
 * pushes it further out (1, 3, then ×2.5 days). A card is cleared from "mistakes" after two
 * right answers in a row on different days.
 */
object Srs {
    const val DAY = 86_400_000L

    data class Card(val intervalDays: Int, val repetitions: Int, val dueAt: Long)

    fun next(card: Card?, correct: Boolean, now: Long): Card = when {
        !correct -> Card(0, 0, now)
        card == null || card.repetitions == 0 -> Card(1, 1, now + DAY)
        card.repetitions == 1 -> Card(3, 2, now + 3 * DAY)
        else -> (card.intervalDays * 2.5).toInt().let { Card(it, card.repetitions + 1, now + it * DAY) }
    }

    fun mastered(card: Card): Boolean = card.repetitions >= 2
}
