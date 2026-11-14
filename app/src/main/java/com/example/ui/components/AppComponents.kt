package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Spacing
import com.example.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: @Composable () -> Unit, modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(), scrollBehavior: TopAppBarScrollBehavior? = null) {
    TopAppBar(title = { ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() } }, modifier = modifier,
        navigationIcon = navigationIcon, actions = actions, colors = colors, scrollBehavior = scrollBehavior)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: () -> Unit, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    AppTopBar(title = { Column { Text(title, style = MaterialTheme.typography.titleLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") } }, actions = actions)
}
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(vertical = Spacing.normal), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }; actions()
    }
}
@Composable
fun AppCard(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape, colors, elevation, border, content)
}
@Composable
fun AppCard(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content: @Composable ColumnScope.() -> Unit) {
    Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
}
@Composable
fun SectionHeader(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = Spacing.small)) }
@Composable
fun InfoRow(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) { Surface(modifier.size(8.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {} }
@Composable
fun StatusChip(label: String, color: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        StatusDot(color); Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small, colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = shape, colors = colors,
        elevation = elevation, border = border, contentPadding = contentPadding, content = content)
}
@Composable
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small, colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null, border: BorderStroke? = ButtonDefaults.outlinedButtonBorder,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = shape, colors = colors,
        elevation = elevation, border = border, contentPadding = contentPadding, content = content)
}
@Composable
fun DestructiveButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    PrimaryButton(onClick, modifier, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger.color, contentColor = MaterialTheme.colorScheme.onError), content = content)
}
@Composable
fun SwitchRow(title: String, description: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall)
            description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }; Switch(checked, onCheckedChange)
    }
}
@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Carregando…") {
    Column(modifier.fillMaxSize().padding(Spacing.normal), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(); Spacer(Modifier.height(Spacing.normal)); Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(modifier.fillMaxSize().padding(Spacing.normal), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        actionLabel?.let { Spacer(Modifier.height(Spacing.normal)); SecondaryButton(onAction) { Text(it) } }
    }
}
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) { EmptyState(message, modifier, "Tentar novamente", onRetry) }
@Composable
fun TermsCheckRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onReadTerms: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange)
            Column(Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.accept_terms), style = MaterialTheme.typography.bodyMedium)
                TextButton(onReadTerms) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.read_terms)) }
            }
        }
    }
}
@Composable
fun AppDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier, dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null, title: @Composable (() -> Unit)? = null, text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge, containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconContentColor: Color = MaterialTheme.colorScheme.secondary, titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties()) {
    AlertDialog(onDismissRequest, confirmButton, modifier, dismissButton, icon, title, text, shape, containerColor, iconContentColor, titleContentColor, textContentColor, tonalElevation, properties)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(), content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState, shape = MaterialTheme.shapes.extraLarge, content = content)
}
