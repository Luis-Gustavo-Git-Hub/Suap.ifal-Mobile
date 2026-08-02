package com.example.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.SubjectRepository

class SuapViewModelFactory(
    private val repository: SubjectRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuapViewModel(repository, sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
