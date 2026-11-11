package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppDatabase
import com.example.data.Subject
import com.example.data.SubjectRepository
import kotlinx.coroutines.flow.Flow

class BoletimViewModel(application: Application) : AndroidViewModel(application) {
    private val subjectRepository = SubjectRepository(AppDatabase.getDatabase(application).subjectDao())

    val subjects: Flow<List<Subject>> = subjectRepository.allSubjects
}
