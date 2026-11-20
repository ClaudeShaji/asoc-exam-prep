package com.asocexamprep.morse

/**
 * Decodes hand-keyed Morse from key-down durations and the gaps between them,
 * adapting the dit length to the sender's speed.
 */
class KeyDecoder(initialDitMs: Double) {
    var ditMs: Double = initialDitMs
        private set
    private val symbols = StringBuilder()
    private val text = StringBuilder()

    val decoded: String get() = text.toString()
    val pending: String get() = symbols.toString()
    val wpm: Int get() = (1200.0 / ditMs).toInt()

    /** A key press of [markMs], [gapBeforeMs] after the previous release (null for the first). */
    fun mark(markMs: Double, gapBeforeMs: Double?) {
        if (gapBeforeMs != null) gap(gapBeforeMs)
        if (markMs < 2 * ditMs) {
            symbols.append('.')
            ditMs = 0.7 * ditMs + 0.3 * markMs
        } else {
            symbols.append('-')
            ditMs = 0.7 * ditMs + 0.3 * (markMs / 3)
        }
        ditMs = ditMs.coerceIn(25.0, 400.0)
    }

    /** Silence of [ms] since the last release: ends a letter or a word. */
    fun gap(ms: Double) {
        when {
            ms > 5 * ditMs -> { flushLetter(); if (text.isNotEmpty() && text.last() != ' ') text.append(' ') }
            ms > 2 * ditMs -> flushLetter()
        }
    }

    fun flushLetter() {
        if (symbols.isEmpty()) return
        text.append(MorseCode.decode(symbols.toString()) ?: '*')
        symbols.clear()
    }

    fun clear() { symbols.clear(); text.clear() }
}
