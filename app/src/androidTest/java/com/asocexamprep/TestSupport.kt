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

/** Records which phonetic clips would play, without sound. */
class FakeVoicePlayer : com.asocexamprep.voice.VoicePlayer {
    val said: MutableList<String> = Collections.synchronizedList(mutableListOf())
    override suspend fun say(items: List<com.asocexamprep.voice.Phonetics.Spoken>, onItem: (Int) -> Unit) {
        items.forEachIndexed { i, it -> onItem(i); it.clip?.let { c -> said += c }; delay(30) }
    }
    override fun release() {}
}

val app: RadioPrepApp get() = ApplicationProvider.getApplicationContext()

/** Known starting state: empty progress, General grade, first Learn step, 20/8 WPM, silent audio. */
fun resetForTest(): FakeMorsePlayer = runBlocking {
    app.container.db.clearAllTables()
    val s = app.container.settings
    s.setGrade(Grade.GENERAL)
    s.setTrainerStep(0)
    s.setCharWpm(20)
    s.setEffectiveWpm(8)
    s.setLiveResults(false)
    app.container.voicePlayer = FakeVoicePlayer()
    FakeMorsePlayer().also { app.container.morsePlayer = it }
}
