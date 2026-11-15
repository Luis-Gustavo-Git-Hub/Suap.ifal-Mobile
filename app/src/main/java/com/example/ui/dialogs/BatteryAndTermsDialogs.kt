package com.example.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    com.example.ui.components.AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otimização da Bateria", fontWeight = FontWeight.Bold) },
        text = {
            Text("Para garantir que você receba notificações de lembrete de aula e novas notas em segundo plano, desative a otimização de bateria para o aplicativo nas configurações do sistema.")
        },
        confirmButton = {
            com.example.ui.components.AppPrimaryButton(onClick = onConfirm) {
                Text("Configurar")
            }
        },
        dismissButton = {
            com.example.ui.components.AppTextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel))
            }
        }
    )
}
