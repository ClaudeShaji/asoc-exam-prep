package com.asocexamprep.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Plays the bundled Ogg clips with [MediaPlayer], one after another. */
class ClipPlayer(private val context: Context) : VoicePlayer {

    override suspend fun say(items: List<Phonetics.Spoken>, onItem: (Int) -> Unit) {
        items.forEachIndexed { i, item ->
            withContext(Dispatchers.Main) { onItem(i) }
            val clip = item.clip
            if (clip == null) delay(WORD_PAUSE_MS) else {
                playClip(clip)
                if (i < items.lastIndex) delay(GAP_MS)
            }
        }
    }

    private suspend fun playClip(name: String) = suspendCancellableCoroutine { cont ->
        val mp = MediaPlayer()
        try {
            context.assets.openFd("${Phonetics.DIR}/$name.ogg").use { fd ->
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mp.setOnCompletionListener { it.release(); if (cont.isActive) cont.resume(Unit) }
            mp.setOnErrorListener { p, _, _ -> p.release(); if (cont.isActive) cont.resume(Unit); true }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            mp.release()
            if (cont.isActive) cont.resume(Unit)
        }
        cont.invokeOnCancellation { runCatching { mp.stop() }; mp.release() }
    }

    override fun release() {}

    private companion object {
        const val GAP_MS = 120L
        const val WORD_PAUSE_MS = 450L
    }
}
