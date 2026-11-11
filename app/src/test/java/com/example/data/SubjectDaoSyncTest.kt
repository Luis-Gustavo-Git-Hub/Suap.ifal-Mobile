package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubjectDaoSyncTest {

    private lateinit var database: AppDatabase
    private lateinit var subjectDao: SubjectDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        subjectDao = database.subjectDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun partialSync_preservesPreviouslyCollectedDetailsAndClassAverages() = runBlocking {
        val subject = Subject(
            id = "/disciplina/1/", name = "Matemática", url = "/disciplina/1/",
            grade1 = "80", classGrade1 = "75", classGrade2 = "70",
            detailUrl = "/notas/1/", isFavorite = true
        )
        subjectDao.syncSubjects(listOf(subject))
        subjectDao.syncSubjects(listOf(Subject(name = subject.name, url = subject.url, grade1 = "90")))
        val updated = subjectDao.getSubjectById(subject.id)!!
        assertEquals("90", updated.grade1)
        assertEquals("75", updated.classGrade1)
        assertEquals("70", updated.classGrade2)
        assertEquals("/notas/1/", updated.detailUrl)
        assertTrue(updated.isFavorite)
    }

    @Test
    fun favoriteAndBackgroundUpdates_doNotOverwriteNewerGradesOrEachOther() = runBlocking {
        val staleSubject = Subject(id = "math", name = "Matemática", grade1 = "70")
        subjectDao.syncSubjects(listOf(staleSubject))
        subjectDao.syncSubjects(listOf(staleSubject.copy(grade1 = "95")))

        subjectDao.toggleFavorite(staleSubject.id)
        subjectDao.updateClassAverages(listOf(staleSubject.copy(classGrade1 = "80")))
        subjectDao.updateSubjectUrls(listOf(staleSubject.copy(url = "/disciplina/math/")))

        val updated = subjectDao.getSubjectById(staleSubject.id)!!
        assertEquals("95", updated.grade1)
        assertEquals("80", updated.classGrade1)
        assertEquals("/disciplina/math/", updated.url)
        assertTrue(updated.isFavorite)
        subjectDao.toggleFavorite(staleSubject.id)
        assertFalse(subjectDao.getSubjectById(staleSubject.id)!!.isFavorite)
    }

    @Test
    fun lateBackgroundUpdates_doNotRecreateRemovedSubjects() = runBlocking {
        val subject = Subject(id = "math", name = "Matemática")
        subjectDao.syncSubjects(listOf(subject))
        subjectDao.deleteSubjectById(subject.id)
        subjectDao.updateClassAverages(listOf(subject.copy(classGrade1 = "80")))
        subjectDao.updateSubjectUrls(listOf(subject.copy(url = "/disciplina/math/")))
        subjectDao.toggleFavorite(subject.id)
        assertTrue(subjectDao.getAllSubjectsSync().isEmpty())
    }

    @Test
    fun syncSubjects_preservesFavoritesAndDeletesMissingWithinTransaction() = runBlocking {
        // Initial subjects
        val initialList = listOf(
            Subject(
                id = "/edu/disciplina/1/",
                name = "Matemática",
                url = "/edu/disciplina/1/",
                grade1 = "7.0",
                isFavorite = true
            ),
            Subject(
                id = "/edu/disciplina/2/",
                name = "História",
                url = "/edu/disciplina/2/",
                grade1 = "6.0",
                isFavorite = false
            ),
            Subject(
                id = "/edu/disciplina/3/",
                name = "Química",
                url = "/edu/disciplina/3/",
                grade1 = "5.0",
                isFavorite = true
            )
        )

        subjectDao.syncSubjects(initialList)

        var current = subjectDao.getAllSubjectsSync()
        assertEquals(3, current.size)
        assertTrue(current.find { it.id == "/edu/disciplina/1/" }!!.isFavorite)
        assertFalse(current.find { it.id == "/edu/disciplina/2/" }!!.isFavorite)
        assertTrue(current.find { it.id == "/edu/disciplina/3/" }!!.isFavorite)

        // Incoming update from SUAP scraping:
        // - Matemática updated with new grade (isFavorite from scraper is default false, but local was true)
        // - História was removed / completed (not present in incoming list)
        // - Física is brand new
        val incomingUpdate = listOf(
            Subject(
                id = "",
                name = "Matemática",
                url = "/edu/disciplina/1/",
                grade1 = "9.5",
                isFavorite = false
            ),
            Subject(
                id = "",
                name = "Química",
                url = "/edu/disciplina/3/",
                grade1 = "8.0",
                isFavorite = false
            ),
            Subject(
                id = "",
                name = "Física",
                url = "/edu/disciplina/4/",
                grade1 = "10.0",
                isFavorite = false
            )
        )

        subjectDao.syncSubjects(incomingUpdate)

        val updated = subjectDao.getAllSubjects().first()
        assertEquals(3, updated.size)

        // Matemática: grade updated to 9.5 and isFavorite preserved as true
        val mat = updated.find { it.name == "Matemática" }!!
        assertEquals("/edu/disciplina/1/", mat.id)
        assertEquals("9.5", mat.grade1)
        assertTrue("isFavorite should be preserved for Matemática", mat.isFavorite)

        // Química: grade updated to 8.0 and isFavorite preserved as true
        val qui = updated.find { it.name == "Química" }!!
        assertEquals("8.0", qui.grade1)
        assertTrue("isFavorite should be preserved for Química", qui.isFavorite)

        // História: should be deleted because it was not in incoming list
        val hist = updated.find { it.name == "História" }
        assertEquals(null, hist)

        // Física: added successfully
        val fis = updated.find { it.name == "Física" }!!
        assertEquals("/edu/disciplina/4/", fis.id)
        assertEquals("10.0", fis.grade1)
        assertFalse(fis.isFavorite)
    }
}
