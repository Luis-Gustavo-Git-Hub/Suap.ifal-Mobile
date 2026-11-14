package com.example.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppSnackbarManager {
    var hostState: SnackbarHostState? = null
    var scope: CoroutineScope? = null

    fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        onAction: (() -> Unit)? = null
    ) {
        val hs = hostState ?: return
        val sc = scope ?: CoroutineScope(Dispatchers.Main)
        sc.launch {
            val result = hs.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    /**
     * Shows a detailed sync failure with diagnostic and solution.
     */
    fun showSyncError(
        reason: SyncFailureReason,
        onRetry: (() -> Unit)? = null
    ) {
        val (message, action) = when (reason) {
            SyncFailureReason.NO_CREDENTIALS -> 
                "Informe sua matrícula e senha para sincronizar." to "Entrar"
            SyncFailureReason.INVALID_CREDENTIALS -> 
                "Confira sua matrícula e senha para entrar no SUAP." to "Revisar login"
            SyncFailureReason.CAPTCHA_REQUIRED -> 
                "SUAP solicitou verificação de Captcha. Toque para resolver manualmente." to "Resolver"
            SyncFailureReason.NO_INTERNET -> 
                "Sem conexão com a internet. Verifique sua rede e tente novamente." to "Tentar de novo"
            SyncFailureReason.TIMEOUT ->
                "O SUAP demorou para responder. Tente novamente." to "Tentar de novo"
            SyncFailureReason.SUAP_UNAVAILABLE -> 
                "Servidores do SUAP/IFAL instáveis ou fora do ar no momento." to "Tentar de novo"
            SyncFailureReason.SESSION_EXPIRED ->
                "Sua sessão do SUAP expirou. Entre novamente para atualizar os dados." to "Entrar"
            SyncFailureReason.GENERIC -> 
                "Falha ao conectar ao SUAP. Verifique conexão e credenciais." to "Tentar de novo"
        }
        show(
            message = message,
            actionLabel = action,
            duration = SnackbarDuration.Long,
            onAction = onRetry
        )
    }
}

enum class SyncFailureReason {
    NO_CREDENTIALS,
    INVALID_CREDENTIALS,
    CAPTCHA_REQUIRED,
    NO_INTERNET,
    SUAP_UNAVAILABLE,
    SESSION_EXPIRED,
    TIMEOUT,
    GENERIC
}
