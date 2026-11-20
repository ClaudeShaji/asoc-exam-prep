package com.asocexamprep

import com.asocexamprep.content.ContentPack
import com.asocexamprep.content.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentPackTest {
    private val pack = ContentPack.load { File("src/main/assets/content/asoc-in/$it").readText() }

    @Test fun packIsValid() = assertEquals(emptyList<String>(), validate(pack))

    @Test fun examRulesMatchSyllabus() {
        val general = pack.grade("GENERAL")!!
        val restricted = pack.grade("RESTRICTED")!!
        assertEquals(8, general.morse!!.wpm)
        assertEquals(null, restricted.morse)
        assertEquals(50 to 60, general.minSectionPercent to general.minAggregatePercent)
        assertEquals(40 to 50, restricted.minSectionPercent to restricted.minAggregatePercent)
        assertEquals(100, pack.exam.sections.sumOf { it.questions })
    }

    @Test fun restrictedHidesGeneralOnlyTopics() {
        val restricted = pack.unitsFor("RESTRICTED").flatMap { it.topics }.map { it.id }
        assertTrue("g-space" !in restricted && "m-morse" !in restricted)
        assertTrue("g-space" in pack.unitsFor("GENERAL").flatMap { it.topics }.map { it.id })
    }

    @Test fun officialReadingComesFirst() {
        val r = pack.readings("b-licensing")
        assertEquals("rules-2024", r.first().source.id)
        assertEquals(9..12, r.first().range)
    }

    @Test fun referenceIsComplete() {
        assertEquals(26, pack.reference.phonetics.size)
        assertEquals(10, pack.reference.figures.size)
        assertEquals(30, pack.reference.qcodes.size)
        assertEquals(17, pack.reference.abbreviations.size)
    }
}
