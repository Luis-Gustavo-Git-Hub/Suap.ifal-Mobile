package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CustomThemeData
import com.example.ui.theme.ThemeColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeCreatorDialog(
    onDismissRequest: () -> Unit,
    onSaveTheme: (CustomThemeData) -> Unit
) {
    var themeName by remember { mutableStateOf("") }
    var primaryColor by remember { mutableStateOf(ThemeColor.GREEN) }
    var iconColor by remember { mutableStateOf(ThemeColor.GREEN) }
    var profileColor by remember { mutableStateOf(ThemeColor.GREEN) }
    var isDark by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Criar Tema",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                HorizontalDivider()

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("Nome do Tema") },
                        placeholder = { Text("Ex: Meu Estilo Lindo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Theme Mode Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ThemeModeButton(
                                text = "Claro",
                                icon = Icons.Default.LightMode,
                                isSelected = !isDark,
                                onClick = { isDark = false },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeButton(
                                text = "Escuro",
                                icon = Icons.Default.DarkMode,
                                isSelected = isDark,
                                onClick = { isDark = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Colors Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Cor da Interface",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ColorPickerGrid(
                                selectedColor = primaryColor,
                                onColorSelected = { primaryColor = it }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "Cor do Ícone do App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ColorPickerGrid(
                                selectedColor = iconColor,
                                onColorSelected = { iconColor = it }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Cor do Fundo do Perfil",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ColorPickerGrid(
                                selectedColor = profileColor,
                                onColorSelected = { profileColor = it }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (themeName.isNotBlank()) {
                                onSaveTheme(
                                    CustomThemeData(
                                        id = UUID.randomUUID().toString(),
                                        name = themeName,
                                        primaryColor = primaryColor,
                                        iconColor = iconColor,
                                        profileColor = profileColor,
                                        isDark = isDark
                                    )
                                )
                            }
                        },
                        enabled = themeName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ColorPickerGrid(
    selectedColor: ThemeColor,
    onColorSelected: (ThemeColor) -> Unit
) {
    val colors = ThemeColor.values()
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.heightIn(max = 220.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(colors) { color ->
            val colorObj = Color(color.hex)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(colorObj)
                    .border(
                        width = if (selectedColor == color) 3.dp else 1.dp,
                        color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            ) {
                if (selectedColor == color) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (colorObj.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                }
            }
        }
    }
}
