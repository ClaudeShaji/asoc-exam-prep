package com.asocexamprep.morse

/** One expected character and whether it was copied correctly. */
data class CharResult(val expected: Char, val typed: Char?, val ok: Boolean)

data class CopyScore(
    val results: List<CharResult>,
    /** Edit distance between expected and typed (spaces ignored). */
    val errors: Int,
    val accuracy: Float,
) {
    /** Per expected character: (sent, correct). */
    fun perChar(): Map<Char, Pair<Int, Int>> = results.groupBy { it.expected }
        .mapValues { (_, r) -> r.size to r.count { it.ok } }
}

object Scoring {
    /** Aligns typed copy against the sent text (Levenshtein) ignoring spaces and case. */
    fun score(sent: String, typed: String): CopyScore {
        val a = MorseCode.clean(sent).replace(" ", "")
        val b = typed.uppercase().filterNot { it.isWhitespace() }
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            d[i][j] = minOf(
                d[i - 1][j] + 1,
                d[i][j - 1] + 1,
                d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
            )
        }
        // Walk back to label each expected character.
        val results = ArrayDeque<CharResult>()
        var i = a.length
        var j = b.length
        while (i > 0) {
            when {
                j > 0 && d[i][j] == d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1 -> {
                    results.addFirst(CharResult(a[i - 1], b[j - 1], a[i - 1] == b[j - 1])); i--; j--
                }
                d[i][j] == d[i - 1][j] + 1 -> { results.addFirst(CharResult(a[i - 1], null, false)); i-- }
                else -> j--
            }
        }
        val errors = d[a.length][b.length]
        val accuracy = if (a.isEmpty()) 0f else (1f - errors.toFloat() / a.length).coerceAtLeast(0f)
        return CopyScore(results.toList(), errors, accuracy)
    }
}
