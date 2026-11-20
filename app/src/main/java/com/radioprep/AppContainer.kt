package com.radioprep

import android.content.Context
import com.radioprep.content.ContentRepository
import com.radioprep.data.db.AppDatabase
import com.radioprep.data.settings.SettingsStore

/** Manual dependency container, created once in [RadioPrepApp]. */
class AppContainer(context: Context) {
    val db: AppDatabase by lazy { AppDatabase.build(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val content: ContentRepository by lazy { ContentRepository(context) }
}
