package com.asocexamprep.morse

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Renders Morse elements to 16-bit mono PCM with raised-cosine edges (no key clicks). */
object ToneGenerator {
    const val SAMPLE_RATE = 44_100

    fun samplesFor(ms: Double, sampleRate: Int = SAMPLE_RATE): Int = (ms * sampleRate / 1000.0).roundToInt()

    fun pcm(
        elements: List<Element>,
        toneHz: Int,
        sampleRate: Int = SAMPLE_RATE,
        rampMs: Double = 5.0,
        amplitude: Double = 0.6,
        leadInMs: Double = 60.0,
    ): ShortArray {
        val lead = samplesFor(leadInMs, sampleRate)
        val total = lead + elements.sumOf { samplesFor(it.ms, sampleRate) }
        val out = ShortArray(total)
        var pos = lead
        val ramp = samplesFor(rampMs, sampleRate).coerceAtLeast(1)
        val peak = amplitude * Short.MAX_VALUE
        for (e in elements) {
            val n = samplesFor(e.ms, sampleRate)
            if (e.on) {
                val r = minOf(ramp, n / 2)
                for (i in 0 until n) {
                    val env = when {
                        i < r -> 0.5 - 0.5 * cos(PI * i / r)
                        i >= n - r -> 0.5 - 0.5 * cos(PI * (n - 1 - i) / r)
                        else -> 1.0
                    }
                    out[pos + i] = (peak * env * sin(2 * PI * toneHz * (pos + i) / sampleRate)).toInt().toShort()
                }
            }
            pos += n
        }
        return out
    }

    /** One second of continuous tone (an integer number of cycles, so it loops cleanly). */
    fun loop(toneHz: Int, sampleRate: Int = SAMPLE_RATE, amplitude: Double = 0.6): ShortArray =
        ShortArray(sampleRate) { i -> (amplitude * Short.MAX_VALUE * sin(2 * PI * toneHz * i / sampleRate)).toInt().toShort() }
}
