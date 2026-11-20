package com.asocexamprep

import com.asocexamprep.morse.CopyMode
import com.asocexamprep.morse.Drills
import com.asocexamprep.morse.KeyDecoder
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.MorseTiming
import com.asocexamprep.morse.Scoring
import com.asocexamprep.morse.ToneGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class MorseTest {
    @Test fun parisIsFiftyDitsAtStandardSpacing() {
        val t = MorseTiming(20)
        assertEquals(60.0, t.ditMs, 1e-9)
        assertEquals(43 * 60.0, t.durationMs("PARIS"), 1e-6)
        // PARIS + word gap = 50 dits = one word at 20 WPM in 3 s.
        assertEquals(3000.0, t.durationMs("PARIS") + t.wordGapMs, 1e-6)
    }

    @Test fun farnsworthStretchesOnlyTheGaps() {
        val t = MorseTiming(charWpm = 20, effectiveWpm = 8)
        assertEquals(60.0, t.ditMs, 1e-9)
        assertTrue(t.charGapMs > 3 * t.ditMs)
        // One PARIS word should take 60/8 = 7.5 s overall.
        val word = t.durationMs("PARIS") + t.wordGapMs
        assertTrue("word took $word", abs(word - 7500) < 1)
    }

    @Test fun elementsFollowThePattern() {
        val e = MorseTiming(20).elements("A")
        assertEquals(listOf(true to 60.0, false to 60.0, true to 180.0), e.map { it.on to it.ms })
    }

    @Test fun unknownCharactersAreSkipped() {
        assertEquals("CQ DE VU2ABC", MorseCode.clean("cq  de\tvu2abc!"))
    }

    @Test fun everyKochCharacterHasACode() {
        assertEquals(MorseCode.KOCH_ORDER.length, MorseCode.KOCH_ORDER.toSet().size)
        MorseCode.KOCH_ORDER.forEach { assertTrue("$it", MorseCode.pattern(it) != null) }
        assertEquals("KM", Drills.kochChars(2))
    }

    @Test fun pcmLengthMatchesTimingAndStartsSilent() {
        val t = MorseTiming(20)
        val pcm = ToneGenerator.pcm(t.elements("E"), 600, leadInMs = 0.0)
        assertEquals(ToneGenerator.samplesFor(60.0), pcm.size)
        assertTrue(abs(pcm[0].toInt()) < 50)
        assertTrue(pcm.maxOrNull()!! > 15000)
        assertTrue(abs(pcm.last().toInt()) < 2000)
    }

    @Test fun decoderReadsCleanKeying() {
        val d = KeyDecoder(60.0)
        // "ET": dit, letter gap, dah.
        d.mark(60.0, null)
        d.mark(180.0, 180.0)
        d.flushLetter()
        assertEquals("ET", d.decoded)
    }

    @Test fun decoderAdaptsToASlowerSender() {
        val d = KeyDecoder(60.0) // starts expecting 20 WPM
        val dit = 120.0 // sender is at 10 WPM
        fun send(p: String, gap: Double?) { p.forEachIndexed { i, s -> d.mark(if (s == '.') dit else 3 * dit, if (i == 0) gap else dit) } }
        send("-.-.", null); send("--.-", 3 * dit); send("--.-", 7 * dit); send("-.-.", 3 * dit)
        d.flushLetter()
        assertEquals("CQ QC", d.decoded)
        assertTrue(d.wpm in 8..14)
    }

    @Test fun decoderRoundTripsGeneratedTiming() {
        val t = MorseTiming(18)
        val d = KeyDecoder(1200.0 / 18)
        var gap: Double? = null
        for (e in t.elements("VU2ABC DE VU3XYZ")) {
            if (e.on) { d.mark(e.ms, gap); gap = null } else gap = (gap ?: 0.0) + e.ms
        }
        d.flushLetter()
        assertEquals("VU2ABC DE VU3XYZ", d.decoded)
    }

    @Test fun scoringCountsErrorsAndIgnoresSpaces() {
        val perfect = Scoring.score("KMKMK MKKMM", "kmkmkmkkmm")
        assertEquals(0, perfect.errors)
        assertEquals(1f, perfect.accuracy)
        val oneMissed = Scoring.score("ABCDE", "ABDE")
        assertEquals(1, oneMissed.errors)
        assertEquals(listOf(true, true, false, true, true), oneMissed.results.map { it.ok })
        assertEquals(0.8f, oneMissed.accuracy, 1e-6f)
        assertEquals(0f, Scoring.score("ABC", "").accuracy)
    }

    @Test fun drillsUseOnlySendableCharacters() {
        val rnd = Random(7)
        CopyMode.entries.forEach { mode ->
            val text = Drills.text(mode, listOf("QRL", "QTH"), "KMRS", rnd)
            assertEquals(MorseCode.clean(text), text)
            assertTrue(text.isNotBlank())
        }
        assertTrue(Drills.examPassage(rnd).length >= 200)
        assertTrue(Drills.groups("KM", 5, rnd).replace(" ", "").all { it in "KM" })
    }
}
