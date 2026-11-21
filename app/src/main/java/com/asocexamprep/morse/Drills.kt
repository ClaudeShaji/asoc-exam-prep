package com.asocexamprep.morse

import kotlin.random.Random

enum class CopyMode(val label: String) {
    LETTERS("Letters"), WEAK("Needs work"), CALLSIGNS("Call signs"), QCODES("Q codes"), WORDS("Words"), EXAM("Exam test"),
}

/** Practice text generators. */
object Drills {
    private val words = listOf(
        "CQ", "DE", "RST", "UR", "NAME", "QTH", "RIG", "ANT", "WX", "TEMP", "HR", "TNX", "FER", "QSO",
        "73", "GM", "GE", "GA", "OM", "YL", "HW", "CPY", "PSE", "AGN", "BK", "PWR", "WATTS", "DIPOLE",
        "SUNNY", "RAIN", "CLOUDY", "FB", "ES", "SOLID", "COPY", "FINE", "CALL", "REPORT", "SIGNAL",
    )

    private val passages = listOf(
        "THE STATION LOG MUST BE KEPT IN A BOUND BOOK WITH NO GAPS BETWEEN ENTRIES.",
        "CALL SIGNS ARE SENT AT THE START AND END OF EACH CONTACT, AND EVERY TEN MINUTES.",
        "RADIO AMATEURS HELP DURING FLOODS AND CYCLONES WHEN NORMAL LINKS FAIL.",
        "THE FORTY METRE BAND RUNS FROM 7000 TO 7200 KHZ IN INDIA.",
        "MY RIG IS A 100 WATT TRANSCEIVER INTO A HALF WAVE DIPOLE AT 12 METRES.",
        "WX HERE IS WARM, 31 DEGREES; SKY CLEAR. PSE QSL VIA THE BUREAU.",
        "IST IS FIVE AND A HALF HOURS AHEAD OF UTC. LOG TIMES IN IST.",
        "SIGNALS ARE FADING NOW; QSB AND SOME QRM ON 14 MHZ. WILL QSY TO 21 MHZ?",
        "THE ASOC TEST AT 8 WPM HAS 200 CHARACTERS SENT OVER FIVE MINUTES.",
        "TNX FER THE NICE QSO OM. HOPE TO MEET AGN SOON. 73 ES GOOD DX.",
    )

    fun kochChars(level: Int): String = MorseCode.KOCH_ORDER.take(level.coerceIn(2, MorseCode.KOCH_ORDER.length))

    /** Groups of five random characters from [chars], weighting the newest one double. */
    fun groups(chars: String, count: Int, rnd: Random = Random.Default): String {
        val pool = chars + chars.last()
        return List(count) { List(5) { pool[rnd.nextInt(pool.length)] }.joinToString("") }.joinToString(" ")
    }

    fun callsign(rnd: Random = Random.Default): String {
        val prefix = listOf("VU2", "VU3", "VU2", "VU3", "AT2", "VU4", "VU7")[rnd.nextInt(7)]
        val n = 2 + rnd.nextInt(2)
        return prefix + List(n) { ('A' + rnd.nextInt(26)) }.joinToString("")
    }

    fun text(mode: CopyMode, qcodes: List<String>, chars: String, rnd: Random = Random.Default): String = when (mode) {
        CopyMode.LETTERS, CopyMode.WEAK -> groups(chars, 5, rnd)
        CopyMode.CALLSIGNS -> List(5) { callsign(rnd) }.joinToString(" ")
        CopyMode.QCODES -> List(6) { qcodes[rnd.nextInt(qcodes.size)] }.joinToString(" ")
        CopyMode.WORDS -> List(8) { words[rnd.nextInt(words.size)] }.joinToString(" ")
        CopyMode.EXAM -> examPassage(rnd)
    }

    /** About 200 characters of plain text like the exam's test piece. */
    fun examPassage(rnd: Random = Random.Default): String {
        val order = passages.shuffled(rnd)
        val sb = StringBuilder()
        for (p in order) {
            if (sb.length >= 200) break
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(p)
        }
        return MorseCode.clean(sb.toString())
    }

    fun sendTarget(rnd: Random = Random.Default): String =
        if (rnd.nextBoolean()) callsign(rnd) else words[rnd.nextInt(words.size)]
}
