package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CustomThemeData

@Composable
fun ThemeSelectionDialog(
    appTheme: AppTheme,
    customThemes: List<CustomThemeData>,
    isRobinKawaUnlocked: Boolean,
    onSelectTheme: (AppTheme) -> Unit,
    onEditCustomTheme: (CustomThemeData) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onOpenFontDialog: () -> Unit,
    onCreateTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolha o Tema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val availableThemes = AppTheme.values().toMutableList()
                if (isRobinKawaUnlocked) {
                    availableThemes.add(AppTheme.ROBIN_KAWA)
                }
                availableThemes.forEach { theme ->
                    val fontDesc = when (theme) {
                        is AppTheme.SYSTEM -> "Fonte do Sistema"
                        is AppTheme.LIGHT -> "Fonte Plus Jakarta Sans"
                        is AppTheme.DARK -> "Fonte Outfit"
                        is AppTheme.DEEP_BLUE -> "Fonte Plus Jakarta Sans"
                        is AppTheme.NATURE -> "Fonte Nunito"
                        is AppTheme.AMOLED -> "Fonte Cinzel"
                        is AppTheme.DRACULA -> "Fonte Fira Code"
                        is AppTheme.SUNSET -> "Fonte Montserrat"
                        is AppTheme.CYBERPUNK -> "Fonte Space Grotesk"
                        is AppTheme.ROBIN_KAWA -> "Fonte Fredoka"
                        is AppTheme.Custom -> "Fonte ${theme.data.fontFamily.displayName}"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(theme) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appTheme.id == theme.id,
                            onClick = { onSelectTheme(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(theme.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = fontDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (customThemes.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Temas Personalizados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    customThemes.forEach { customTheme ->
                        val theme = AppTheme.Custom(customTheme)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTheme(theme) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                RadioButton(
                                    selected = appTheme.id == theme.id,
                                    onClick = { onSelectTheme(theme) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(theme.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Fonte ${customTheme.fontFamily.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditCustomTheme(customTheme) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteCustomTheme(customTheme.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenFontDialog) {
                    Text("Trocar Fonte")
                }
                TextButton(onClick = onCreateTheme) {
                    Text("Criar Tema")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun FontSelectionDialog(
    currentFontOverride: AppFontFamily?,
    onSelectFont: (AppFontFamily?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fonte da Interface", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Altere a fonte de qualquer tema padrão ou personalizado:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFont(null) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentFontOverride == null,
                        onClick = { onSelectFont(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Padrão do Tema", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Usar a fonte original configurada do tema atual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                AppFontFamily.values().forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFont(font) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFontOverride == font,
                            onClick = { onSelectFont(font) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(font.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = font.styleDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
