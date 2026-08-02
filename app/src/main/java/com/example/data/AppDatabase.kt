package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Subject::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "suap_database")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
