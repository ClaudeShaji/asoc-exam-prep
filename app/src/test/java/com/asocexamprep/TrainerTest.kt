package com.asocexamprep

import com.asocexamprep.content.ContentPack
import com.asocexamprep.morse.MorseCode
import com.asocexamprep.morse.Stage
import com.asocexamprep.morse.Trainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class TrainerTest {
    private val data = ContentPack.load { File("src/main/assets/content/asoc-in/$it").readText() }.training!!

    @Test fun ladderGoesCharactersGroupsWordsSentences() {
        assertEquals(26 + 10 + 7, Trainer.CHAR_ORDER.length)
        assertEquals(Trainer.CHAR_ORDER.length, Trainer.CHAR_ORDER.toSet().size)
        Trainer.CHAR_ORDER.forEach { assertTrue("$it", MorseCode.pattern(it) != null) }
        assertTrue(Trainer.CHAR_ORDER.take(26).all { it.isLetter() })
        val stages = Trainer.steps.map { it.stage }
        assertEquals(Stage.entries.toList(), stages.distinct())
        assertEquals(stages, stages.sortedBy { it.ordinal })
        assertEquals('K', Trainer.steps[0].char)
        assertEquals(Trainer.steps.indices.toList(), Trainer.steps.map { it.index })
    }

    @Test fun firstCharacterIsDrilledAlone() {
        val p = Trainer.charPrompts(Trainer.steps[0], Random(1))
        assertEquals(Trainer.CHAR_PROMPTS, p.size)
        assertTrue(p.all { it == 'K' })
    }

    @Test fun laterCharactersMixOnlyLearnedOnes() {
        val step = Trainer.steps[4] // K M U R E
        val p = Trainer.charPrompts(step, Random(2))
        assertEquals(listOf('E', 'E', 'E'), p.take(3))
        assertTrue(p.all { it in "KMURE" })
    }

    @Test fun groupsWordsAndSentencesComeFromTheSafeLists() {
        val rnd = Random(3)
        for (step in Trainer.steps.filter { it.stage != Stage.CHARACTERS }) {
            val text = Trainer.text(step, data, rnd)
            assertEquals(MorseCode.clean(text), text)
            if (step.stage == Stage.GROUPS) {
                val groups = text.split(' ')
                assertEquals(10, groups.size)
                assertTrue(groups.all { it.length == step.key.toInt() && it in data.groups.getValue(step.key) })
            }
        }
    }

    @Test fun safeListsAreWellFormed() {
        for ((len, list) in data.groups) {
            assertTrue(list.size >= 100)
            assertEquals(list.size, list.toSet().size)
            assertTrue(list.all { it.length == len.toInt() && it.all { c -> c in 'A'..'Z' } })
        }
        assertTrue(data.words.getValue("short").isNotEmpty() && data.words.getValue("long").isNotEmpty())
        assertTrue(data.sentences.all { MorseCode.clean(it) == it })
    }

    @Test fun ninetyPercentPasses() {
        assertTrue(Trainer.passed(0.9f))
        assertFalse(Trainer.passed(0.89f))
    }
}
