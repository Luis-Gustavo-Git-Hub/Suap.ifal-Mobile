package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AulaHistoryEntity
import com.example.data.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AulasViewModel(application: Application) : AndroidViewModel(application) {
    private val scheduleRepository = ScheduleRepository.getInstance(application)

    private val _aulas = MutableStateFlow<List<AulaHistoryEntity>>(emptyList())
    val aulas: StateFlow<List<AulaHistoryEntity>> = _aulas.asStateFlow()

    fun loadAulasForSubject(url: String) {
        viewModelScope.launch {
            _aulas.value = scheduleRepository.getAulasForSubject(url)
        }
    }
}
