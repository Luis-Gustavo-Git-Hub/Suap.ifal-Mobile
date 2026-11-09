package com.example.data

import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun syncSubjects(subjects: List<Subject>) = subjectDao.syncSubjects(subjects)
    suspend fun insertAll(subjects: List<Subject>) = subjectDao.syncSubjects(subjects)
    suspend fun upsertSubjects(subjects: List<Subject>) = subjectDao.upsertSubjects(subjects)
    suspend fun getAllSubjectsSync(): List<Subject> = subjectDao.getAllSubjectsSync()
    suspend fun getSubjectById(id: String): Subject? = subjectDao.getSubjectById(id)
    suspend fun upsertSubject(subject: Subject) = subjectDao.upsertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun toggleFavorite(id: String) = subjectDao.toggleFavorite(id)
    suspend fun updateClassAverages(subjects: List<Subject>) = subjectDao.updateClassAverages(subjects)
    suspend fun updateSubjectUrls(subjects: List<Subject>) = subjectDao.updateSubjectUrls(subjects)
    suspend fun deleteSubjectById(id: String) = subjectDao.deleteSubjectById(id)
    suspend fun clearAll() = subjectDao.deleteAll()
}
