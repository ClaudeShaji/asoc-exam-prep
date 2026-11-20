package com.radioprep.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicProgress::class, PdfProgress::class, QuestionAttempt::class, SrsCard::class,
        MorseSession::class, ExamAttempt::class, DailyActivity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun activity(): ActivityDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "radioprep.db").build()
    }
}
