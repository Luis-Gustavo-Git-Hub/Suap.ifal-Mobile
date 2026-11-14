package com.example.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otimização da Bateria", fontWeight = FontWeight.Bold) },
        text = {
            Text("Para garantir que você receba notificações de lembrete de aula e novas notas em segundo plano, desative a otimização de bateria para o aplicativo nas configurações do sistema.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Configurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
