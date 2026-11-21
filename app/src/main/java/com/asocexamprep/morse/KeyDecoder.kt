package com.asocexamprep.morse

/**
 * Decodes hand-keyed Morse from key-down durations and the gaps between them.
 * Both the dit length and the learner's gap between dots and dashes adapt, so
 * slow or uneven finger keying still splits letters and words in the right places.
 */
class KeyDecoder(initialDitMs: Double) {
    /** Typical dot and dash lengths so far; a press is whichever it is nearer to (on a log scale). */
    private var dotMs: Double = initialDitMs
    private var dashMs: Double = 3 * initialDitMs
    val ditMs: Double get() = (dotMs + dashMs / 3) / 2
    /** Running average of the gap inside a letter (between dots and dashes). */
    private var elementGapMs: Double = initialDitMs
    private val symbols = StringBuilder()
    private val text = StringBuilder()

    val decoded: String get() = text.toString()
    val pending: String get() = symbols.toString()
    val wpm: Int get() = (1200.0 / ditMs).toInt()

    /** Silence longer than this ends a letter. */
    val letterGapMs: Double get() = maxOf(2.2 * ditMs, 1.8 * elementGapMs)
    /** Silence longer than this ends a word. */
    val wordGapMs: Double get() = 2.8 * letterGapMs

    /** A key press of [markMs], [gapBeforeMs] after the previous release (null for the first). */
    fun mark(markMs: Double, gapBeforeMs: Double?) {
        if (gapBeforeMs != null) gap(gapBeforeMs)
        val threshold = kotlin.math.sqrt(dotMs * dashMs)
        if (markMs < threshold) {
            symbols.append('.')
            dotMs = 0.5 * dotMs + 0.5 * markMs
            dashMs = 0.5 * dashMs + 0.5 * (3 * markMs)
        } else {
            symbols.append('-')
            dashMs = 0.5 * dashMs + 0.5 * markMs
            dotMs = 0.5 * dotMs + 0.5 * (markMs / 3)
        }
        dotMs = dotMs.coerceIn(25.0, 600.0)
        dashMs = dashMs.coerceIn(2 * dotMs, 1800.0)
    }

    /** Silence of [ms] since the last release: inside a letter, or the end of a letter or word. */
    fun gap(ms: Double) {
        when {
            ms > wordGapMs -> { flushLetter(); if (text.isNotEmpty() && text.last() != ' ') text.append(' ') }
            ms > letterGapMs -> flushLetter()
            else -> elementGapMs = (0.5 * elementGapMs + 0.5 * ms).coerceIn(20.0, 1200.0)
        }
    }

    fun flushLetter() {
        if (symbols.isEmpty()) return
        text.append(MorseCode.decode(symbols.toString()) ?: '*')
        symbols.clear()
    }

    fun clear() { symbols.clear(); text.clear() }

    companion object {
        /** Starting dit for a learner keying by finger: their overall speed, but no faster than 15 WPM. */
        fun initialDitFor(effectiveWpm: Int): Double = 1200.0 / effectiveWpm.coerceIn(5, 15)
    }
}
