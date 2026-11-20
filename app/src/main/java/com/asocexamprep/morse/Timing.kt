package com.asocexamprep.morse

/** One key-down (tone) or key-up (silence) interval. */
data class Element(val on: Boolean, val ms: Double)

/**
 * PARIS timing with optional Farnsworth spacing: characters are sent at [charWpm]
 * while the gaps between characters and words are stretched so the overall speed is
 * [effectiveWpm] (ARRL formula).
 */
data class MorseTiming(val charWpm: Int, val effectiveWpm: Int = charWpm) {
    init {
        require(charWpm in 3..60) { "charWpm out of range" }
        require(effectiveWpm in 1..charWpm) { "effectiveWpm must be 1..charWpm" }
    }

    val ditMs: Double = 1200.0 / charWpm

    val charGapMs: Double
    val wordGapMs: Double

    init {
        if (effectiveWpm >= charWpm) {
            charGapMs = 3 * ditMs
            wordGapMs = 7 * ditMs
        } else {
            val c = charWpm.toDouble()
            val s = effectiveWpm.toDouble()
            val taSec = (60 * c - 37.2 * s) / (s * c)
            charGapMs = 3 * taSec / 19 * 1000
            wordGapMs = 7 * taSec / 19 * 1000
        }
    }

    /** Elements for [text]; unknown characters are skipped, spaces become word gaps. */
    fun elements(text: String): List<Element> {
        val out = mutableListOf<Element>()
        val words = MorseCode.clean(text).split(' ').filter { it.isNotEmpty() }
        words.forEachIndexed { wi, word ->
            if (wi > 0) out += Element(false, wordGapMs)
            word.forEachIndexed { ci, ch ->
                if (ci > 0) out += Element(false, charGapMs)
                val p = MorseCode.pattern(ch) ?: return@forEachIndexed
                p.forEachIndexed { si, sym ->
                    if (si > 0) out += Element(false, ditMs)
                    out += Element(true, if (sym == '.') ditMs else 3 * ditMs)
                }
            }
        }
        return out
    }

    fun durationMs(text: String): Double = elements(text).sumOf { it.ms }
}
