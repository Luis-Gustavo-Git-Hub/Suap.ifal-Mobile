package com.example.ui

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CustomThemeData
import com.example.ui.theme.IconPack
import com.example.ui.theme.NavBarStyle
import com.example.ui.theme.ThemeColor
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.getFontFamily
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeCreatorDialog(
    initialTheme: CustomThemeData? = null,
    onDismissRequest: () -> Unit,
    onSaveTheme: (CustomThemeData) -> Unit
) {
    var themeName by remember { mutableStateOf(initialTheme?.name ?: "") }
    var primaryColor by remember { mutableStateOf(initialTheme?.primaryColor ?: ThemeColor.GREEN) }
    var iconColor by remember { mutableStateOf(initialTheme?.iconColor ?: ThemeColor.GREEN) }
    var profileColor by remember { mutableStateOf(initialTheme?.profileColor ?: ThemeColor.GREEN) }
    var isDark by remember { mutableStateOf(initialTheme?.isDark ?: false) }
    var iconPack by remember { mutableStateOf(initialTheme?.iconPack ?: IconPack.DEFAULT) }
    var navBarStyle by remember { mutableStateOf(initialTheme?.navBarStyle ?: NavBarStyle.DEFAULT) }
    var fontFamily by remember { mutableStateOf(initialTheme?.fontFamily ?: AppFontFamily.DEFAULT) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = AppDimens.Spacing16, end = AppDimens.Spacing12, top = AppDimens.Spacing16, bottom = AppDimens.Spacing12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = AppDimens.Spacing8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(AppDimens.Spacing8)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (initialTheme != null) "Editar Tema" else "Criar Tema",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding), color = MaterialTheme.colorScheme.surfaceVariant)

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing16),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    

                    // Name Section
                    SectionTitle(title = "Preview ao Vivo", icon = Icons.Outlined.Visibility)
                    ThemePreview(
                        primaryColor = primaryColor,
                        iconColor = iconColor,
                        isDark = isDark,
                        iconPack = iconPack,
                        navBarStyle = navBarStyle,
                        fontFamily = fontFamily
                    )
                    
                    SectionTitle(title = "Informações Básicas", icon = Icons.Outlined.Info)

                    com.example.ui.components.AppTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("Nome do Tema") },
                        placeholder = { Text("Ex: Meu Estilo Lindo") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Theme Mode Selector
                    SectionTitle(title = "Modo de Cor", icon = Icons.Outlined.Contrast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                    ) {
                        ThemeModeButton(
                            text = "Claro",
                            icon = Icons.Outlined.LightMode,
                            isSelected = !isDark,
                            onClick = { isDark = false },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeModeButton(
                            text = "Escuro",
                            icon = Icons.Outlined.DarkMode,
                            isSelected = isDark,
                            onClick = { isDark = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Font Selector Section
                    SectionTitle(title = "Tipografia & Fonte", icon = Icons.Outlined.TextFields)
                    com.example.ui.components.AppContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Spacing16),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                        ) {
                            Text(
                                "Escolha a Fonte do Tema",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "A fonte escolhida será aplicada em todos os títulos, textos e elementos do app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                            ) {
                                items(
                                    items = AppFontFamily.values().toList(),
                                    key = { it.name },
                                    contentType = { "font_option" }
                                ) { fontOption ->
                                    FontOptionCard(
                                        font = fontOption,
                                        isSelected = fontOption == fontFamily,
                                        onClick = { fontFamily = fontOption }
                                    )
                                }
                            }
                        }
                    }

                    // Colors Selector
                    SectionTitle(title = "Paleta de Cores", icon = Icons.Outlined.ColorLens)
                    com.example.ui.components.AppContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Spacing16),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
                        ) {
                            ColorSelector(
                                title = "Cor Principal",
                                selectedColor = primaryColor,
                                onColorSelected = { primaryColor = it }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            ColorSelector(
                                title = "Cor dos Ícones",
                                selectedColor = iconColor,
                                onColorSelected = { iconColor = it }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            ColorSelector(
                                title = "Fundo do Perfil",
                                selectedColor = profileColor,
                                onColorSelected = { profileColor = it }
                            )
                        }
                    }

                    // Style Selector
                    SectionTitle(title = "Estilo Visual", icon = Icons.Outlined.Brush)
                    com.example.ui.components.AppContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Spacing16),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing24)
                        ) {
                            // Icon Pack
                            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
                                Text(
                                    "Estilo dos Ícones",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                                ) {
                                    items(
                                        items = IconPack.values().toList(),
                                        key = { it.name },
                                        contentType = { "icon_pack" }
                                    ) { pack ->
                                        StyleOptionCard(
                                            title = pack.displayName,
                                            isSelected = pack == iconPack,
                                            icon = getIconForIconPack(pack),
                                            onClick = { iconPack = pack }
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            
                            // Nav Bar Style
                            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
                                Text(
                                    "Estilo da Barra de Navegação",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                                ) {
                                    items(
                                        items = NavBarStyle.values().toList(),
                                        key = { it.name },
                                        contentType = { "navbar_style" }
                                    ) { style ->
                                        StyleOptionCard(
                                            title = style.displayName,
                                            isSelected = style == navBarStyle,
                                            icon = getIconForNavBarStyle(style),
                                            onClick = { navBarStyle = style }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Footer
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing16),
                        horizontalArrangement = Arrangement.End
                    ) {
                        com.example.ui.components.AppTextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.padding(end = AppDimens.Spacing12)
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel), fontWeight = FontWeight.Bold)
                        }
                        com.example.ui.components.AppPrimaryButton(
                            onClick = {
                                if (themeName.isNotBlank()) {
                                    onSaveTheme(
                                        CustomThemeData(
                                            id = initialTheme?.id ?: UUID.randomUUID().toString(),
                                            name = themeName,
                                            primaryColor = primaryColor,
                                            iconColor = iconColor,
                                            profileColor = profileColor,
                                            isDark = isDark,
                                            iconPack = iconPack,
                                            navBarStyle = navBarStyle,
                                            fontFamily = fontFamily
                                        )
                                    )
                                }
                            },
                            enabled = themeName.isNotBlank(),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = AppDimens.Spacing24, vertical = AppDimens.Spacing12)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar Tema", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FontOptionCard(
    font: AppFontFamily,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val targetFontFamily = getFontFamily(font)
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "fontBgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "fontContentColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "fontBorderColor"
    )

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .widthIn(min = 140.dp, max = 165.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing12),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aa",
                    fontFamily = targetFontFamily,
                    
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = font.displayName,
                fontFamily = targetFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = font.styleDescription,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ThemePreview(
    primaryColor: ThemeColor,
    iconColor: ThemeColor,
    isDark: Boolean,
    iconPack: IconPack,
    navBarStyle: NavBarStyle,
    fontFamily: AppFontFamily = AppFontFamily.DEFAULT
) {
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = if (isDark) Color.White else Color.Black
    val pColor = Color(primaryColor.hex)
    val iColor = Color(iconColor.hex)
    val previewFont = getFontFamily(fontFamily)
    
    com.example.ui.components.AppContentCard(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mock App Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppDimens.Spacing16),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preview (${fontFamily.displayName})", 
                    color = onBgColor, 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = previewFont,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = pColor, modifier = Modifier.size(24.dp))
            }
            
            // Mock Content
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
                Surface(
                    color = surfaceColor,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Texto Exemplo",
                            fontFamily = previewFont,
                            
                            fontWeight = FontWeight.SemiBold,
                            color = onBgColor
                        )
                    }
                }
                Surface(
                    color = pColor,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Botão",
                            fontFamily = previewFont,
                            
                            fontWeight = FontWeight.Bold,
                            color = if (Color(primaryColor.hex).luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Mock Nav Bar
            val navShape = when (navBarStyle) {
                NavBarStyle.DEFAULT -> androidx.compose.material3.MaterialTheme.shapes.extraLarge
                NavBarStyle.FLOATING -> MaterialTheme.shapes.large
                NavBarStyle.CUT -> androidx.compose.foundation.shape.CutCornerShape(12.dp)
                NavBarStyle.PILL -> RoundedCornerShape(50)
            }
            val navPadding = if (navBarStyle == NavBarStyle.FLOATING) Modifier.padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing8) else Modifier
            
            Box(modifier = Modifier.fillMaxWidth().then(navPadding)) {
                Surface(
                    color = surfaceColor,
                    shape = navShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icons = listOf(
                            getIconForIconPack(iconPack),
                            if (iconPack == IconPack.TECH) Icons.Outlined.Insights else Icons.Outlined.Timeline,
                            if (iconPack == IconPack.NATURE) Icons.Outlined.LocalFlorist else Icons.Outlined.Notifications
                        )
                        icons.forEachIndexed { index, icon ->
                            val selected = index == 0
                            val color = if (selected) pColor else onBgColor.copy(alpha = 0.5f)
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = AppDimens.Spacing4)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ColorSelector(
    title: String,
    selectedColor: ThemeColor,
    onColorSelected: (ThemeColor) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
            contentPadding = PaddingValues(horizontal = AppDimens.Spacing4, vertical = AppDimens.Spacing4)
        ) {
            items(
                items = ThemeColor.values().toList(),
                key = { it.name },
                contentType = { "theme_color_item" }
            ) { color ->
                ColorItem(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
fun ColorItem(color: ThemeColor, isSelected: Boolean, onClick: () -> Unit) {
    val colorObj = Color(color.hex)
    val borderWidth by animateDpAsState(if (isSelected) 3.dp else 1.dp, label = "borderWidth")
    val size by animateDpAsState(if (isSelected) 56.dp else 48.dp, label = "size")
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colorObj)
            .border(
                width = borderWidth,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (colorObj.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun ThemeModeButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "borderColor"
    )
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = AppDimens.Spacing16, horizontal = AppDimens.Spacing16),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StyleOptionCard(
    title: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "borderColor"
    )

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .widthIn(min = 120.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun getIconForIconPack(pack: IconPack): ImageVector {
    return when (pack) {
        IconPack.DEFAULT -> Icons.Outlined.Apps
        IconPack.NATURE -> Icons.Outlined.Park
        IconPack.TECH -> Icons.Outlined.Terminal
        IconPack.MINIMAL -> Icons.Outlined.CropSquare
        IconPack.OCEAN -> Icons.Outlined.Water
        IconPack.SUNSET -> Icons.Outlined.WbSunny
    }
}

fun getIconForNavBarStyle(style: NavBarStyle): ImageVector {
    return when (style) {
        NavBarStyle.DEFAULT -> Icons.Outlined.Menu
        NavBarStyle.FLOATING -> Icons.Outlined.SpaceDashboard
        NavBarStyle.CUT -> Icons.Outlined.DashboardCustomize
        NavBarStyle.PILL -> Icons.Outlined.LinearScale
    }
}
