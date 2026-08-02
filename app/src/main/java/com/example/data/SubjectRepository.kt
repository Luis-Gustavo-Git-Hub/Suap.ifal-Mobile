package com.example.data

import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun insertAll(subjects: List<Subject>) = subjectDao.insertSubjects(subjects)
    suspend fun getAllSubjectsSync(): List<Subject> = subjectDao.getAllSubjectsSync()
    
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun clearAll() = subjectDao.deleteAll()
}
