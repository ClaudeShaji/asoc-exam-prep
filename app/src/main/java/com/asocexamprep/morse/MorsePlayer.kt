package com.asocexamprep.morse

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Plays Morse and the sending sidetone. Swappable so UI tests run silently. */
interface MorsePlayer {
    /** Plays [text]; returns when finished; cancelling the caller stops the sound. */
    suspend fun play(text: String, timing: MorseTiming, toneHz: Int)
    fun keyDown(toneHz: Int)
    fun keyUp()
    fun release()
}

class AudioTrackPlayer : MorsePlayer {

    private fun attrs() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun format() = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(ToneGenerator.SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    private fun staticTrack(pcm: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs())
            .setAudioFormat(format())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    override suspend fun play(text: String, timing: MorseTiming, toneHz: Int) {
        val pcm = withContext(Dispatchers.Default) { ToneGenerator.pcm(timing.elements(text), toneHz) }
        if (pcm.isEmpty()) return
        val track = staticTrack(pcm)
        try {
            track.play()
            delay(pcm.size * 1000L / ToneGenerator.SAMPLE_RATE + 80)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /**
     * Sidetone: a low-latency stream that runs while the send screen is open and fades the
     * tone in and out (5 ms) on key down/up, so there are no clicks or start-up delays.
     */
    private inner class Sidetone(val hz: Int) : Thread("sidetone") {
        @Volatile var keyed = false
        @Volatile var running = true
        private val track: AudioTrack = AudioTrack.Builder()
            .setAudioAttributes(attrs())
            .setAudioFormat(format())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(
                maxOf(
                    AudioTrack.getMinBufferSize(ToneGenerator.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                    512 * 2,
                ),
            )
            .build()

        override fun run() {
            val sr = ToneGenerator.SAMPLE_RATE
            val buf = ShortArray(128)
            val step = 1.0 / (0.005 * sr)
            val dPhase = 2 * Math.PI * hz / sr
            var env = 0.0
            var phase = 0.0
            track.play()
            while (running) {
                for (i in buf.indices) {
                    env = if (keyed) minOf(1.0, env + step) else maxOf(0.0, env - step)
                    buf[i] = (0.6 * Short.MAX_VALUE * env * kotlin.math.sin(phase)).toInt().toShort()
                    phase += dPhase
                    if (phase > 2 * Math.PI) phase -= 2 * Math.PI
                }
                track.write(buf, 0, buf.size)
            }
            runCatching { track.stop() }
            track.release()
        }
    }

    private var sidetone: Sidetone? = null

    override fun keyDown(toneHz: Int) {
        val st = sidetone?.takeIf { it.hz == toneHz && it.running } ?: run {
            sidetone?.running = false
            Sidetone(toneHz).also { it.start(); sidetone = it }
        }
        st.keyed = true
    }

    override fun keyUp() {
        sidetone?.keyed = false
    }

    /** Stops the sidetone stream (call when leaving the send screen). */
    override fun release() {
        sidetone?.running = false
        sidetone = null
    }
}
