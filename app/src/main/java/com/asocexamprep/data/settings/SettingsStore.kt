package com.asocexamprep.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Grade(val label: String) { GENERAL("General"), RESTRICTED("Restricted") }

data class Settings(
    val grade: Grade = Grade.GENERAL,
    /** Exam date as epoch day, or null when not set. */
    val examDay: Long? = null,
    val dailyMinutes: Int = 30,
    val toneHz: Int = 600,
    val charWpm: Int = 20,
    val effectiveWpm: Int = 8,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val grade = stringPreferencesKey("grade")
        val examDay = longPreferencesKey("exam_day")
        val dailyMinutes = intPreferencesKey("daily_minutes")
        val toneHz = intPreferencesKey("tone_hz")
        val charWpm = intPreferencesKey("char_wpm")
        val effectiveWpm = intPreferencesKey("effective_wpm")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            grade = p[Keys.grade]?.let { runCatching { Grade.valueOf(it) }.getOrNull() } ?: d.grade,
            examDay = p[Keys.examDay],
            dailyMinutes = p[Keys.dailyMinutes] ?: d.dailyMinutes,
            toneHz = p[Keys.toneHz] ?: d.toneHz,
            charWpm = p[Keys.charWpm] ?: d.charWpm,
            effectiveWpm = p[Keys.effectiveWpm] ?: d.effectiveWpm,
        )
    }

    suspend fun setGrade(g: Grade) = context.dataStore.edit { it[Keys.grade] = g.name }
    suspend fun setExamDay(day: Long?) = context.dataStore.edit {
        if (day == null) it.remove(Keys.examDay) else it[Keys.examDay] = day
    }
    suspend fun setDailyMinutes(m: Int) = context.dataStore.edit { it[Keys.dailyMinutes] = m.coerceIn(5, 240) }
}
