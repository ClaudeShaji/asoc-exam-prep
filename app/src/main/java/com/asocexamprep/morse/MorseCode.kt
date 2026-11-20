package com.asocexamprep.morse

/** International Morse code for the characters the ASOC Morse test uses. */
object MorseCode {
    val table: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.", '=' to "-...-",
        '-' to "-....-", ';' to "-.-.-.", '+' to ".-.-.",
    )

    private val reverse: Map<String, Char> = table.entries.associate { (k, v) -> v to k }

    fun pattern(c: Char): String? = table[c.uppercaseChar()]
    fun decode(pattern: String): Char? = reverse[pattern]

    /** Koch order (LCWO): two letters to start, one more per level. */
    const val KOCH_ORDER = "KMURESNAPTLWI.JZ=FOY,VG5/Q92H38B?47C1D60X"

    /** Prosign names shown for characters that stand for them. */
    val prosign: Map<Char, String> = mapOf('=' to "BT", '+' to "AR")

    /** Pattern drawn with middle dot and minus sign, for display. */
    fun display(c: Char): String = pattern(c).orEmpty().map { if (it == '.') '·' else '−' }.joinToString(" ")

    /** Keeps only characters Morse can send, collapsing whitespace to single spaces. */
    fun clean(text: String): String =
        text.uppercase().map { if (it.isWhitespace()) ' ' else it }
            .filter { it == ' ' || it in table }
            .joinToString("").trim().replace(Regex(" +"), " ")
}
