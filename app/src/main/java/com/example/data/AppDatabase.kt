package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, AcademicEventEntity::class, AulaHistoryEntity::class],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun academicEventDao(): AcademicEventDao
    abstract fun aulaHistoryDao(): AulaHistoryDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `grade1` TEXT NOT NULL,
                        `grade2` TEXT NOT NULL,
                        `rec1` TEXT NOT NULL,
                        `grade3` TEXT NOT NULL,
                        `grade4` TEXT NOT NULL,
                        `classGrade1` TEXT NOT NULL,
                        `classGrade2` TEXT NOT NULL,
                        `classGrade3` TEXT NOT NULL,
                        `classGrade4` TEXT NOT NULL,
                        `rec2` TEXT NOT NULL,
                        `recFinal` TEXT NOT NULL,
                        `finalGrade` TEXT NOT NULL,
                        `absences` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `detailUrl` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `subjects_new` (
                        `id`, `name`, `url`, `grade1`, `grade2`, `rec1`, `grade3`, `grade4`,
                        `classGrade1`, `classGrade2`, `classGrade3`, `classGrade4`, `rec2`,
                        `recFinal`, `finalGrade`, `absences`, `status`, `isFavorite`, `detailUrl`
                    )
                    SELECT 
                        CASE 
                            WHEN url IS NOT NULL AND TRIM(url) != '' THEN TRIM(url) 
                            ELSE TRIM(name) 
                        END AS id,
                        name, url, grade1, grade2, rec1, grade3, grade4,
                        classGrade1, classGrade2, classGrade3, classGrade4, rec2,
                        recFinal, finalGrade, absences, status, isFavorite, detailUrl
                    FROM `subjects`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `subjects`")
                db.execSQL("ALTER TABLE `subjects_new` RENAME TO `subjects`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "suap_database")
                    .addMigrations(MIGRATION_8_9)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
