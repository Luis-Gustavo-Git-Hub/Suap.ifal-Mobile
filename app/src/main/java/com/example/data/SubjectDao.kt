package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsSync(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): Subject?

    @Upsert
    suspend fun upsertSubjects(subjects: List<Subject>)

    @Upsert
    suspend fun upsertSubject(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Query("UPDATE subjects SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE subjects SET classGrade1 = :grade1, classGrade2 = :grade2, classGrade3 = :grade3, classGrade4 = :grade4 WHERE id = :id")
    suspend fun updateClassAveragesById(id: String, grade1: String, grade2: String, grade3: String, grade4: String)

    @Transaction
    suspend fun updateClassAverages(subjects: List<Subject>) {
        subjects.forEach {
            updateClassAveragesById(it.id, it.classGrade1, it.classGrade2, it.classGrade3, it.classGrade4)
        }
    }

    @Query("UPDATE subjects SET url = :url WHERE id = :id")
    suspend fun updateSubjectUrl(id: String, url: String)

    @Transaction
    suspend fun updateSubjectUrls(subjects: List<Subject>) {
        subjects.forEach { updateSubjectUrl(it.id, it.url) }
    }

    @Query("DELETE FROM subjects WHERE id NOT IN (:validIds)")
    suspend fun deleteSubjectsNotIn(validIds: List<String>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()

    @Transaction
    suspend fun syncSubjects(newSubjects: List<Subject>) {
        if (newSubjects.isEmpty()) {
            return
        }

        // 1. Ensure all new subjects have stable IDs
        val normalizedNewSubjects = newSubjects.map { subject ->
            if (subject.id.isBlank()) {
                subject.copy(id = Subject.generateStableId(subject.name, subject.url))
            } else {
                subject
            }
        }

        // 2. Fetch existing subjects to preserve isFavorite and other local state
        val existingSubjects = getAllSubjectsSync()
        val existingMap = existingSubjects.associateBy { it.id }
        val existingByName = existingSubjects.associateBy { it.name.trim().lowercase() }

        // 3. Merge: keep isFavorite from existing record if available
        val mergedSubjects = normalizedNewSubjects.map { newSub ->
            val existing = existingMap[newSub.id]
                ?: existingMap[Subject.generateStableId(newSub.name, newSub.url)]
                ?: existingByName[newSub.name.trim().lowercase()]

            if (existing != null) {
                newSub.copy(
                    isFavorite = existing.isFavorite,
                    url = newSub.url.ifBlank { existing.url },
                    detailUrl = newSub.detailUrl.ifBlank { existing.detailUrl },
                    classGrade1 = newSub.classGrade1.ifBlank { existing.classGrade1 },
                    classGrade2 = newSub.classGrade2.ifBlank { existing.classGrade2 },
                    classGrade3 = newSub.classGrade3.ifBlank { existing.classGrade3 },
                    classGrade4 = newSub.classGrade4.ifBlank { existing.classGrade4 }
                )
            } else {
                newSub
            }
        }

        // 4. Upsert all new/updated subjects
        val changedSubjects = mergedSubjects.filter { existingMap[it.id] != it }
        if (changedSubjects.isNotEmpty()) upsertSubjects(changedSubjects)

        // 5. Remove subjects that are no longer in the incoming list
        val validIds = mergedSubjects.map { it.id }.toSet()
        if (existingSubjects.any { it.id !in validIds }) {
            deleteSubjectsNotIn(validIds.toList())
        }
    }
}
