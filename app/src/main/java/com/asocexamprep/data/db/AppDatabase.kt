package com.asocexamprep.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicProgress::class, PdfProgress::class, QuestionAttempt::class, SrsCard::class,
        MorseSession::class, ExamAttempt::class, DailyActivity::class,
        XpEvent::class, AchievementRow::class, StepBest::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun activity(): ActivityDao
    abstract fun game(): GameDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "radioprep.db").build()
    }
}
