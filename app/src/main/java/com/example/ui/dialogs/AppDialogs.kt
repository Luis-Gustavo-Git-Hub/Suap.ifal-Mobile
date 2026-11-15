package com.example.ui.dialogs

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    com.example.ui.components.AppDialog(
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
                            .padding(vertical = AppDimens.Spacing8, horizontal = AppDimens.Spacing4),
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
                    HorizontalDivider(modifier = Modifier.padding(vertical = AppDimens.Spacing8))
                    Text("Temas Personalizados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(AppDimens.Spacing8))
                    customThemes.forEach { customTheme ->
                        val theme = AppTheme.Custom(customTheme)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTheme(theme) }
                                .padding(vertical = AppDimens.Spacing8, horizontal = AppDimens.Spacing4),
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
                                    Icon(Icons.Outlined.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteCustomTheme(customTheme.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Excluir", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                com.example.ui.components.AppTextButton(onClick = onOpenFontDialog) {
                    Text("Trocar Fonte")
                }
                com.example.ui.components.AppTextButton(onClick = onCreateTheme) {
                    Text("Criar Tema")
                }
            }
        },
        dismissButton = {
            com.example.ui.components.AppTextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_close))
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
    com.example.ui.components.AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fonte da Interface", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Altere a fonte de qualquer tema padrão ou personalizado:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = AppDimens.Spacing8)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFont(null) }
                        .padding(vertical = AppDimens.Spacing8, horizontal = AppDimens.Spacing4),
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

                HorizontalDivider(modifier = Modifier.padding(vertical = AppDimens.Spacing4))

                AppFontFamily.values().forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFont(font) }
                            .padding(vertical = AppDimens.Spacing8, horizontal = AppDimens.Spacing4),
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
            com.example.ui.components.AppTextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_close)) }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
