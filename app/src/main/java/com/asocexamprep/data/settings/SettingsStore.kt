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
    /** Highest unlocked step of the Morse learning ladder (see morse.Trainer). */
    val trainerStep: Int = 0,
    /** Exam papers: show right/wrong as soon as an answer is picked. */
    val liveResults: Boolean = false,
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
        val trainerStep = intPreferencesKey("trainer_step")
        val liveResults = androidx.datastore.preferences.core.booleanPreferencesKey("live_results")
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
            trainerStep = p[Keys.trainerStep] ?: d.trainerStep,
            liveResults = p[Keys.liveResults] ?: d.liveResults,
        )
    }

    suspend fun setGrade(g: Grade) = context.dataStore.edit { it[Keys.grade] = g.name }
    suspend fun setExamDay(day: Long?) = context.dataStore.edit {
        if (day == null) it.remove(Keys.examDay) else it[Keys.examDay] = day
    }
    suspend fun setCharWpm(w: Int) = context.dataStore.edit {
        val c = w.coerceIn(5, 40)
        it[Keys.charWpm] = c
        if ((it[Keys.effectiveWpm] ?: Settings().effectiveWpm) > c) it[Keys.effectiveWpm] = c
    }
    suspend fun setEffectiveWpm(w: Int) = context.dataStore.edit {
        it[Keys.effectiveWpm] = w.coerceIn(3, it[Keys.charWpm] ?: Settings().charWpm)
    }
    suspend fun setToneHz(hz: Int) = context.dataStore.edit { it[Keys.toneHz] = hz.coerceIn(400, 1000) }
    suspend fun setTrainerStep(step: Int) = context.dataStore.edit {
        it[Keys.trainerStep] = step.coerceIn(0, com.asocexamprep.morse.Trainer.steps.lastIndex)
    }
    suspend fun setLiveResults(on: Boolean) = context.dataStore.edit { it[Keys.liveResults] = on }
    suspend fun setDailyMinutes(m: Int) = context.dataStore.edit { it[Keys.dailyMinutes] = m.coerceIn(5, 240) }
}
