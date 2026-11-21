package com.asocexamprep

import com.asocexamprep.content.ContentPack
import com.asocexamprep.voice.Phonetics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhoneticsTest {
    @Test fun spellsCallSignsAndSkipsOtherMarks() {
        val s = Phonetics.spell(" vu2abc / 73 ")
        assertEquals("VU2ABC 73", s.joinToString("") { it.char.toString() })
        assertEquals(listOf("v", "u", "d2", "a", "b", "c", null, "d7", "d3"), s.map { it.clip })
        assertEquals(emptyList<Phonetics.Spoken>(), Phonetics.spell("  -/ "))
    }

    @Test fun everyReferenceCharacterHasABundledClip() {
        val pack = ContentPack.load { File("src/main/assets/content/asoc-in/$it").readText() }
        val chars = (pack.reference.phonetics + pack.reference.figures).map { it.char }
        assertEquals(36, chars.size)
        for (c in chars) {
            val clip = Phonetics.clipFor(c.single())!!
            assertTrue("missing $clip", File("src/main/assets/${Phonetics.DIR}/$clip.ogg").length() > 1000)
        }
        assertTrue(File("src/main/assets/${Phonetics.DIR}/LICENSE.txt").readText().contains("Allison Smith"))
    }
}
