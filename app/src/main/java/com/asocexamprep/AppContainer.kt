package com.asocexamprep

import android.content.Context
import com.asocexamprep.content.ContentRepository
import com.asocexamprep.data.db.AppDatabase
import com.asocexamprep.data.settings.SettingsStore

/** Manual dependency container, created once in [RadioPrepApp]. */
class AppContainer(context: Context) {
    val db: AppDatabase by lazy { AppDatabase.build(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val content: ContentRepository by lazy { ContentRepository(context) }
}
