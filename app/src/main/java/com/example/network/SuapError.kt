package com.example.network

import com.example.util.SyncFailureReason

sealed class SuapError(val reason: SyncFailureReason, val message: String, val requiresUserAction: Boolean = false) {
    data object NoCredentials : SuapError(SyncFailureReason.NO_CREDENTIALS, "Informe suas credenciais do SUAP.", true)
    data object InvalidCredentials : SuapError(SyncFailureReason.INVALID_CREDENTIALS, "Confira sua matrícula e senha.", true)
    data object VerificationRequired : SuapError(SyncFailureReason.CAPTCHA_REQUIRED, "O SUAP solicitou uma verificação.", true)
    data class SessionExpired(val userAction: Boolean = false) : SuapError(SyncFailureReason.SESSION_EXPIRED, "Sessão do SUAP expirada. Entre novamente para atualizar.", userAction)
    data object NoInternet : SuapError(SyncFailureReason.NO_INTERNET, "Sem internet. Verifique sua conexão.")
    data object Timeout : SuapError(SyncFailureReason.TIMEOUT, "O SUAP demorou para responder. Tente novamente.")
    data object SuapUnavailable : SuapError(SyncFailureReason.SUAP_UNAVAILABLE, "O SUAP está indisponível no momento.")
    data object ParseError : SuapError(SyncFailureReason.GENERIC, "Não foi possível ler os dados do SUAP.")
    data object Unknown : SuapError(SyncFailureReason.GENERIC, "Não foi possível concluir a atualização.")

    val isTransient: Boolean get() = this == NoInternet || this == Timeout || this == SuapUnavailable

    companion object {
        fun fromException(error: Throwable): SuapError = when (error) {
            is SuapRequestException -> error.error
            is java.net.SocketTimeoutException -> Timeout
            is java.net.UnknownHostException, is java.net.ConnectException -> NoInternet
            else -> Unknown
        }
        fun fromReason(reason: SyncFailureReason): SuapError = when (reason) {
            SyncFailureReason.NO_CREDENTIALS -> NoCredentials
            SyncFailureReason.INVALID_CREDENTIALS -> InvalidCredentials
            SyncFailureReason.CAPTCHA_REQUIRED -> VerificationRequired
            SyncFailureReason.SESSION_EXPIRED -> SessionExpired(true)
            SyncFailureReason.NO_INTERNET -> NoInternet
            SyncFailureReason.TIMEOUT -> Timeout
            SyncFailureReason.SUAP_UNAVAILABLE -> SuapUnavailable
            else -> Unknown
        }
    }
}

class SuapRequestException(val error: SuapError) : java.io.IOException(error.message)
