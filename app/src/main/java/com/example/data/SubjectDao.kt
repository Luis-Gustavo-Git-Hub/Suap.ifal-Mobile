package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsSync(): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    
    @Update
    suspend fun updateSubject(subject: Subject)
    
    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
