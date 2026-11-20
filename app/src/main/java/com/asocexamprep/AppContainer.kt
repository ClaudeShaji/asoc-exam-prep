package com.asocexamprep

import android.content.Context
import com.asocexamprep.content.ContentRepository
import com.asocexamprep.data.db.AppDatabase
import com.asocexamprep.data.settings.SettingsStore
import com.asocexamprep.morse.AudioTrackPlayer
import com.asocexamprep.morse.MorsePlayer

/** Manual dependency container, created once in [RadioPrepApp]. */
class AppContainer(context: Context) {
    val db: AppDatabase by lazy { AppDatabase.build(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val content: ContentRepository by lazy { ContentRepository(context) }

    private var player: MorsePlayer? = null
    /** Audio for Morse; UI tests replace it with a silent recorder. */
    var morsePlayer: MorsePlayer
        get() = player ?: AudioTrackPlayer().also { player = it }
        set(value) { player = value }
}
