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
    private var sidetone: AudioTrack? = null
    private var sidetoneHz = 0

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

    override fun keyDown(toneHz: Int) {
        val t = sidetone?.takeIf { sidetoneHz == toneHz } ?: run {
            sidetone?.release()
            val pcm = ToneGenerator.loop(toneHz)
            staticTrack(pcm).also {
                it.setLoopPoints(0, pcm.size, -1)
                sidetone = it
                sidetoneHz = toneHz
            }
        }
        runCatching {
            t.pause()
            t.playbackHeadPosition = 0
            t.play()
        }
    }

    override fun keyUp() {
        runCatching { sidetone?.pause() }
    }

    override fun release() {
        sidetone?.release()
        sidetone = null
    }
}
