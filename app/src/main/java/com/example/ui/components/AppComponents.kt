package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.AppDimens

/** Consistent feedback and recovery action for tabs showing unavailable or cached data. */
@Composable
fun AppFeedbackCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    AppCard(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(actionLabel) }
        }
    }
}

/**
 * Standard App Top Bar with filled back button and title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Standard App Card: solid surfaceContainer color, subtle border, large shape (16.dp), no gradient.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = accent?.copy(alpha = 0.35f)
        ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(shape)
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            content = content
        )
    }
}

/**
 * Standard section header with optional action button or counter.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Spacing8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        action?.invoke()
    }
}

/** Shared header for the main tabs: consistent hierarchy, spacing, and card treatment. */
@Composable
fun AppPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleMaxLines: Int? = 2,
    eyebrow: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            leadingContent?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = titleMaxLines ?: Int.MAX_VALUE,
                    overflow = if (titleMaxLines == null) TextOverflow.Clip else TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                supportingContent?.invoke()
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * Standard chip component for filters and tags.
 */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color? = null
) {
    val activeColor = accentColor ?: MaterialTheme.colorScheme.primary
    val containerColor = if (selected) {
        activeColor.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) activeColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

enum class AppButtonVariant {
    Primary,
    Secondary,
    Tertiary
}

/**
 * Unified Button:
 * Primary -> Button
 * Secondary -> FilledTonalButton
 * Tertiary -> TextButton
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                ButtonInnerContent(text = text, icon = icon)
            }
        }
        AppButtonVariant.Secondary -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                ButtonInnerContent(text = text, icon = icon)
            }
        }
        AppButtonVariant.Tertiary -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                ButtonInnerContent(text = text, icon = icon)
            }
        }
    }
}

@Composable
private fun ButtonInnerContent(
    text: String,
    icon: ImageVector?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
