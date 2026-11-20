package com.asocexamprep

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.asocexamprep.morse.AudioTrackPlayer
import com.asocexamprep.morse.MorseTiming
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Plays real audio through the phone (one short "E" and a sidetone blip). */
@RunWith(AndroidJUnit4::class)
class AudioPlaybackTest {
    @Test fun playsMorseAndSidetone() = runBlocking {
        val player = AudioTrackPlayer()
        val t0 = System.nanoTime()
        player.play("EE", MorseTiming(20), 600)
        val ms = (System.nanoTime() - t0) / 1_000_000
        // 2 dits + a letter gap = 300 ms of Morse, plus lead-in and tail.
        assertTrue("played for $ms ms", ms in 300..1500)
        player.keyDown(700); Thread.sleep(80); player.keyUp()
        player.keyDown(700); Thread.sleep(80); player.keyUp()
        player.release()
    }
}
