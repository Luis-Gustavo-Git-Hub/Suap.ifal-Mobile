package com.example.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate8To9_preservesDataAndUpdatesPrimaryKeyAndFavorites() {
        // Create database in version 8
        var db = helper.createDatabase(TEST_DB, 8)

        // Insert test subjects in version 8 schema (integer autoincrement ID)
        val cv1 = ContentValues().apply {
            put("id", 1)
            put("name", "Estruturas de Dados")
            put("url", "/edu/disciplina/101/")
            put("grade1", "8.5")
            put("grade2", "9.0")
            put("rec1", "")
            put("grade3", "7.5")
            put("grade4", "8.0")
            put("classGrade1", "")
            put("classGrade2", "")
            put("classGrade3", "")
            put("classGrade4", "")
            put("rec2", "")
            put("recFinal", "")
            put("finalGrade", "8.3")
            put("absences", "2")
            put("status", "Aprovado")
            put("isFavorite", 1)
            put("detailUrl", "/edu/disciplina/101/detalhes/")
        }
        db.insert("subjects", SQLiteDatabase.CONFLICT_REPLACE, cv1)

        val cv2 = ContentValues().apply {
            put("id", 2)
            put("name", "Física 1")
            put("url", "")
            put("grade1", "6.0")
            put("grade2", "6.5")
            put("rec1", "")
            put("grade3", "7.0")
            put("grade4", "7.0")
            put("classGrade1", "")
            put("classGrade2", "")
            put("classGrade3", "")
            put("classGrade4", "")
            put("rec2", "")
            put("recFinal", "")
            put("finalGrade", "6.6")
            put("absences", "4")
            put("status", "Cursando")
            put("isFavorite", 0)
            put("detailUrl", "")
        }
        db.insert("subjects", SQLiteDatabase.CONFLICT_REPLACE, cv2)
        db.close()

        // Run migration to version 9 and validate schema
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, AppDatabase.MIGRATION_8_9)

        // Query the migrated subjects table
        val cursor = db.query("SELECT id, name, url, grade1, isFavorite, status FROM subjects ORDER BY name ASC")
        assertTrue(cursor.moveToFirst())

        // 1st item: "Estruturas de Dados", id should be the url "/edu/disciplina/101/"
        val id1 = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val name1 = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val grade1_1 = cursor.getString(cursor.getColumnIndexOrThrow("grade1"))
        val isFav1 = cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite"))
        val status1 = cursor.getString(cursor.getColumnIndexOrThrow("status"))

        assertEquals("/edu/disciplina/101/", id1)
        assertEquals("Estruturas de Dados", name1)
        assertEquals("8.5", grade1_1)
        assertEquals(1, isFav1)
        assertEquals("Aprovado", status1)

        // 2nd item: "Física 1", url was empty so id should fallback to name "Física 1"
        assertTrue(cursor.moveToNext())
        val id2 = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val name2 = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val grade1_2 = cursor.getString(cursor.getColumnIndexOrThrow("grade1"))
        val isFav2 = cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite"))
        val status2 = cursor.getString(cursor.getColumnIndexOrThrow("status"))

        assertEquals("Física 1", id2)
        assertEquals("Física 1", name2)
        assertEquals("6.0", grade1_2)
        assertEquals(0, isFav2)
        assertEquals("Cursando", status2)

        cursor.close()
        db.close()
    }
}
