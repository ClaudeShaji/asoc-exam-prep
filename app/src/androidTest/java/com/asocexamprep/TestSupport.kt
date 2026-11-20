package com.asocexamprep

import androidx.test.core.app.ApplicationProvider
import com.asocexamprep.data.settings.Grade
import com.asocexamprep.morse.MorsePlayer
import com.asocexamprep.morse.MorseTiming
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.Collections

/** Silent player that records what the app asked to play. */
class FakeMorsePlayer : MorsePlayer {
    val played: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile var keyDowns = 0
    override suspend fun play(text: String, timing: MorseTiming, toneHz: Int) {
        played += text
        delay(30)
    }
    override fun keyDown(toneHz: Int) { keyDowns++ }
    override fun keyUp() {}
    override fun release() {}
}

val app: RadioPrepApp get() = ApplicationProvider.getApplicationContext()

/** Known starting state: General grade, first Learn step, 20/8 WPM, silent audio. */
fun resetForTest(): FakeMorsePlayer = runBlocking {
    val s = app.container.settings
    s.setGrade(Grade.GENERAL)
    s.setTrainerStep(0)
    s.setCharWpm(20)
    s.setEffectiveWpm(8)
    FakeMorsePlayer().also { app.container.morsePlayer = it }
}
