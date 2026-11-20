package com.asocexamprep.morse

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Pre-computed practice lists (assets content/<exam>/morse/training.json, built by tools/gen-training.py). */
@Serializable
data class TrainingData(
    /** Random letter groups keyed by length ("2".."5"), screened for offensive words. */
    val groups: Map<String, List<String>>,
    /** "short" and "long" word lists. */
    val words: Map<String, List<String>>,
    val sentences: List<String>,
)

enum class Stage(val label: String) { CHARACTERS("Characters"), GROUPS("Mixed letters"), WORDS("Words"), SENTENCES("Sentences") }

/** One rung of the ladder. */
data class Step(val index: Int, val stage: Stage, val label: String, val char: Char? = null, val key: String = "")

/**
 * The learning ladder: every character on its own (letters, then figures, then the exam's
 * punctuation), then mixed letter groups of 2-5, then short and long words, then sentences.
 * A session passes at 90%; passing the current step unlocks the next.
 */
object Trainer {
    val CHAR_ORDER: String = MorseCode.KOCH_ORDER.filter { it.isLetter() } + "5923847160" + ".,?/=-;"
    const val PASS = 0.9f
    const val CHAR_PROMPTS = 10

    val steps: List<Step> = buildList {
        CHAR_ORDER.forEach { c -> add(Step(size, Stage.CHARACTERS, "Character ${size + 1} of ${CHAR_ORDER.length}", char = c)) }
        for (n in 2..5) add(Step(size, Stage.GROUPS, "Groups of $n letters", key = "$n"))
        add(Step(size, Stage.WORDS, "Short words", key = "short"))
        add(Step(size, Stage.WORDS, "Long words", key = "long"))
        add(Step(size, Stage.SENTENCES, "Sentences"))
    }

    fun step(index: Int): Step = steps[index.coerceIn(0, steps.lastIndex)]
    fun firstOf(stage: Stage): Step = steps.first { it.stage == stage }

    /** Characters known once [stepIndex] is reached (all of them after the character stage). */
    fun learnedChars(stepIndex: Int): String = CHAR_ORDER.take((stepIndex + 1).coerceIn(1, CHAR_ORDER.length))

    /** Single-character prompts: the new character three times first, then a mix weighted to it. */
    fun charPrompts(step: Step, rnd: Random = Random.Default): List<Char> {
        val c = requireNotNull(step.char)
        val known = learnedChars(step.index)
        val pool = known + "$c$c"
        return List(3) { c } + List(CHAR_PROMPTS - 3) { pool[rnd.nextInt(pool.length)] }
    }

    /** Text for a group, word or sentence step. */
    fun text(step: Step, data: TrainingData, rnd: Random = Random.Default): String = when (step.stage) {
        Stage.GROUPS -> data.groups.getValue(step.key).shuffled(rnd).take(10).joinToString(" ")
        Stage.WORDS -> data.words.getValue(step.key).shuffled(rnd).take(8).joinToString(" ")
        Stage.SENTENCES -> data.sentences.shuffled(rnd).take(2).joinToString(" ")
        Stage.CHARACTERS -> error("character steps use charPrompts")
    }

    fun passed(accuracy: Float) = accuracy >= PASS
}
