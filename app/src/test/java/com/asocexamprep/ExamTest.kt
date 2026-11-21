package com.asocexamprep

import com.asocexamprep.content.ContentPack
import com.asocexamprep.exam.Exam
import com.asocexamprep.exam.Srs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ExamTest {
    private val pack = ContentPack.load { File("src/main/assets/content/asoc-in/$it").readText() }

    @Test fun bankIsLargeAndValid() {
        assertTrue(pack.questions.size >= 200)
        // Every answer comes from a published bank in sources.json.
        assertTrue(pack.questions.all { q -> pack.sources.any { it.id == q.source } })
        // Corrections from the official pool are recorded on the question.
        val dc = pack.questions.first { it.id == "vu2mue-basic-1" }
        assertEquals("A direct current", dc.options[dc.answer])
        assertTrue(dc.verified.single().endsWith("T5A04"))
        assertTrue(pack.questions.count { it.section == "A" } >= 100)
        assertTrue(pack.questions.count { it.section == "B" } >= 50)
    }

    @Test fun hintsAreCopiedFromTheNotes() {
        assertTrue(pack.hints.size >= 30)
        // The validator (packIsValid) fails if any hint is not verbatim notes text; double-check one.
        val (id, h) = pack.hints.entries.first { it.key == "vu2mue-rules-49" }
        assertTrue(h.text.contains("30 seconds"))
        assertTrue(pack.questions.any { it.id == id })
    }

    @Test fun mockPaperHasTheRealShape() {
        for (grade in listOf("GENERAL", "RESTRICTED")) {
            val paper = Exam.mock(pack, grade, Random(1))
            assertEquals(50, paper.count { it.section == "A" })
            assertEquals(50, paper.count { it.section == "B" })
            assertEquals(paper.size, paper.map { it.id }.toSet().size)
        }
        // Restricted papers never include General-only topics.
        val restricted = Exam.mock(pack, "RESTRICTED", Random(2))
        assertTrue(restricted.none { it.topic.startsWith("g-") })
        // Questions are spread over many topics.
        assertTrue(Exam.mock(pack, "GENERAL", Random(3)).filter { it.section == "A" }.map { it.topic }.toSet().size >= 15)
    }

    @Test fun passRulesFollowTheGrade() {
        val general = pack.grade("GENERAL")!!
        val restricted = pack.grade("RESTRICTED")!!
        val paper = Exam.mock(pack, "GENERAL", Random(4))
        fun answers(pctA: Int, pctB: Int): Map<String, Int> {
            val a = paper.filter { it.section == "A" }; val b = paper.filter { it.section == "B" }
            return (a.take(a.size * pctA / 100) + b.take(b.size * pctB / 100)).associate { it.id to it.answer }
        }
        assertTrue(Exam.score(pack, general, paper, answers(60, 60)).passed)
        assertFalse(Exam.score(pack, general, paper, answers(80, 46)).passed) // B under 50%
        assertFalse(Exam.score(pack, general, paper, answers(56, 56)).passed) // aggregate under 60%
        assertTrue(Exam.score(pack, restricted, paper, answers(56, 56)).passed)
        assertFalse(Exam.score(pack, restricted, paper, answers(70, 38)).passed)
        val s = Exam.score(pack, general, paper, answers(100, 100))
        assertEquals(100, s.percent)
    }

    @Test fun practiceByTopicOrSection() {
        assertTrue(Exam.practice(pack, "GENERAL", "b-phonetics-codes").all { it.topic == "b-phonetics-codes" })
        assertEquals(10, Exam.practice(pack, "GENERAL", "A").size)
        assertTrue(Exam.practice(pack, "RESTRICTED", "g-fax-tv").isEmpty())
    }

    @Test fun spacedRepetitionSchedule() {
        val now = 1_000_000L
        val wrong = Srs.next(null, false, now)
        assertEquals(now, wrong.dueAt)
        val r1 = Srs.next(wrong, true, now)
        assertEquals(now + Srs.DAY, r1.dueAt)
        assertFalse(Srs.mastered(r1))
        val r2 = Srs.next(r1, true, now)
        assertEquals(now + 3 * Srs.DAY, r2.dueAt)
        assertTrue(Srs.mastered(r2))
        assertEquals(0, Srs.next(r2, false, now).repetitions)
    }

    @Test fun sectionPapers() {
        val b = Exam.mock(pack, "GENERAL", Random(7), setOf("B"))
        assertEquals(50, b.size)
        assertTrue(b.all { it.section == "B" })
        assertEquals(60, Exam.minutesFor(pack, pack.grade("GENERAL")!!, 50))
        assertEquals(30, Exam.minutesFor(pack, pack.grade("RESTRICTED")!!, 50))
    }
}
