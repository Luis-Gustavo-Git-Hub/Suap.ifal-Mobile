package com.example.data

import android.content.Context
import com.example.network.NetworkResult
import com.example.network.SuapAuthRepository
import com.example.util.CredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository private constructor(
    private val context: Context,
    private val credentialStore: CredentialStore = CredentialStore.getInstance(context),
    private val suapAuthRepository: SuapAuthRepository = SuapAuthRepository.getInstance(context)
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val savedUser = credentialStore.getSuapUsername()
        if (savedUser.isNotBlank()) {
            _authState.value = AuthState.Authenticated(savedUser)
        }
    }

    fun getUsername(): String = credentialStore.getSuapUsername()
    fun getPassword(): String = credentialStore.getSuapPassword()

    suspend fun login(username: String, pass: String): NetworkResult<Boolean> {
        _authState.value = AuthState.Loading
        if (!credentialStore.setSuapCredentials(username, pass)) {
            val message = "Não foi possível proteger suas credenciais neste dispositivo."
            _authState.value = AuthState.Error(message)
            return NetworkResult.Error(message)
        }
        val result = suapAuthRepository.login()
        when (result) {
            is NetworkResult.Success -> {
                _authState.value = AuthState.Authenticated(username)
            }
            is NetworkResult.Error -> {
                _authState.value = AuthState.Error(result.message)
            }
        }
        return result
    }

    fun logout() {
        credentialStore.setSuapCredentials("", "")
        _authState.value = AuthState.Idle
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
