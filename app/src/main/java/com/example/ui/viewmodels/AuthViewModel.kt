package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.example.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.getInstance(application)

    val authState: StateFlow<AuthState> = authRepository.authState

    fun login(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.login(username, pass)) {
                is NetworkResult.Success -> onResult(true, null)
                is NetworkResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
