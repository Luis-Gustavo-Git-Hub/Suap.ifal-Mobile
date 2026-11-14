package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Aviso
import com.example.data.AvisosRepository
import com.example.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AvisosViewModel(application: Application) : AndroidViewModel(application) {
    private val avisosRepository = AvisosRepository.getInstance(application)

    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())
    val avisos: StateFlow<List<Aviso>> = _avisos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAvisos() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = avisosRepository.fetchAvisos()) {
                is NetworkResult.Success -> {
                    _avisos.value = result.data
                }
                is NetworkResult.Error -> {
                    // Fail gracefully
                }
            }
            _isLoading.value = false
        }
    }
}
