package com.asocexamprep.voice

/**
 * Spells text with the phonetic alphabet, using the bundled voice recordings
 * (assets/audio/phonetics, Asterisk core sounds by Allison Smith, CC BY-SA 3.0).
 */
object Phonetics {
    const val DIR = "audio/phonetics"

    /** One spoken item: the character, and the clip to play (null = a pause between words). */
    data class Spoken(val char: Char, val clip: String?)

    fun clipFor(c: Char): String? = when (val l = c.lowercaseChar()) {
        in 'a'..'z' -> l.toString()
        in '0'..'9' -> "d$l"
        else -> null
    }

    /** Letters and digits become clips; spaces become a pause; anything else is skipped. */
    fun spell(text: String): List<Spoken> {
        val out = mutableListOf<Spoken>()
        for (c in text.trim()) {
            val clip = clipFor(c)
            when {
                clip != null -> out += Spoken(c.uppercaseChar(), clip)
                c.isWhitespace() && out.isNotEmpty() && out.last().clip != null -> out += Spoken(' ', null)
            }
        }
        return out
    }
}

/** Plays phonetic clips in sequence. Swappable so UI tests run silently. */
interface VoicePlayer {
    /** Plays [items] in order, calling [onItem] with the index as each starts; cancel to stop. */
    suspend fun say(items: List<Phonetics.Spoken>, onItem: (Int) -> Unit = {})
    fun release()
}
