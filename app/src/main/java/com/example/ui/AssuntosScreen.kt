package com.example.ui

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Subject
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private val WHITESPACE_REGEX = Regex("\\s+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssuntosScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onMaterialsClick: (String, String) -> Unit,
    onHistoryClick: (String, String) -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredSubjects = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) {
            subjects
        } else {
            val query = searchQuery.trim().lowercase()
            subjects.filter {
                it.name.orEmpty().lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            com.example.ui.components.AppTopBar(
                title = { com.example.ui.components.AppBarTitle("Disciplinas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            com.example.ui.components.AppTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing8),
                placeholder = { Text("Pesquisar por disciplina...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Pesquisar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.Spacing32),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Nenhuma disciplina encontrada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tente buscar por outro termo ou limpe o campo de busca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = AppDimens.Spacing16, end = AppDimens.Spacing16, top = AppDimens.Spacing4, bottom = AppDimens.Spacing24),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                ) {
                    items(
                        items = filteredSubjects,
                        key = { it.name.orEmpty() },
                        contentType = { "subject_card" }
                    ) { subject ->
                        val urlToUse = remember(subject) {
                            if ((subject.url ?: "").isNotEmpty()) subject.url.orEmpty() else subject.detailUrl.orEmpty()
                        }
                        AssuntoSubjectCard(
                            subject = subject,
                            onMaterialsClick = { onMaterialsClick(subject.name.orEmpty(), urlToUse) },
                            onHistoryClick = { onHistoryClick(subject.name.orEmpty(), urlToUse) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSubjectBadge(
    subjectName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ShapeDefaults.Medium,
    customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    customGradient: List<Color>? = null
) {
    val clean = remember(subjectName) { subjectName.trim().replace(WHITESPACE_REGEX, " ") }
    val gradientColors = remember(clean, customGradient) { customGradient ?: getSubjectGradient(clean) }
    val icon = remember(clean, customIcon) { customIcon ?: getSubjectIcon(clean) }

    val bgBrush = remember(gradientColors) {
        Brush.linearGradient(colors = gradientColors)
    }
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = subjectName,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AssuntoSubjectCard(subject: Subject, onMaterialsClick: () -> Unit, onHistoryClick: () -> Unit) {
    val cleanName = remember(subject.name) {
        subject.name.orEmpty().trim().replace(WHITESPACE_REGEX, " ")
    }
    val gradientColors = remember(cleanName) { getSubjectGradient(cleanName) }
    val primaryColor = gradientColors.first()

    com.example.ui.components.AppContentCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .padding(AppDimens.Spacing16)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedSubjectBadge(
                    subjectName = cleanName,
                    size = 48.dp,
                    iconSize = 24.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Bento style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                com.example.ui.components.AppTonalButton(
                    onClick = onMaterialsClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing4),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Materiais",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                com.example.ui.components.AppTonalButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing4),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aulas",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
fun getSubjectColor(subjectName: String, isDark: Boolean): androidx.compose.ui.graphics.Color =
    com.example.ui.theme.getSubjectColor(subjectName, isDark)

fun getSubjectGradient(subjectName: String): List<androidx.compose.ui.graphics.Color> =
    com.example.ui.theme.getSubjectGradient(subjectName)
