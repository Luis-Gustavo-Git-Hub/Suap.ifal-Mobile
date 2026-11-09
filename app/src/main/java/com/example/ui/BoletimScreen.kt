package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior
import com.example.ui.theme.AppSemanticColors
import com.example.ui.theme.AppDimens
import com.example.ui.theme.getSubjectColor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EtapaDetail
import com.example.data.Subject
import com.example.domain.AcademicCalculator
import java.text.Normalizer
import java.util.Locale



private fun parseGradeSafe(input: String?): Double? = AcademicCalculator.parseGrade(input)

private fun formatGradeDisplay(value: Double?): String = AcademicCalculator.formatGrade(value).replace('.', ',')

@Composable
fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgColor = if (selected) selectedContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val contentColor = if (selected) selectedColor else unselectedColor
    val borderColor = if (selected) selectedColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(40.dp)
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoletimScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onSubjectClick: ((String, String) -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val subjectDetails by viewModel.subjectDetails.collectAsStateWithLifecycle()
    val subjectUiModels by viewModel.subjectUiModels.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("Todas") }
    var selectedSubjectForDetails by remember { mutableStateOf<Subject?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LaunchedEffect(subjectUiModels) {
        val currentId = selectedSubjectForDetails?.id
        if (currentId != null) {
            val updated = subjectUiModels.find { it.subject.id == currentId }?.subject
            if (updated != null) {
                selectedSubjectForDetails = updated
            }
        }
    }

    val currentSubjectForDetails = remember(selectedSubjectForDetails, subjectUiModels) {
        val currentId = selectedSubjectForDetails?.id
        if (currentId != null) {
            subjectUiModels.find { it.subject.id == currentId }?.subject ?: selectedSubjectForDetails
        } else {
            null
        }
    }
    val isWide = com.example.ui.components.isWideScreen()

    val filteredSubjects = remember(searchQuery, selectedFilter, subjectUiModels) {
        var list = subjectUiModels
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.removeAccents().lowercase(Locale.ROOT)
            list = list.filter { it.normalizedName.contains(query) }
        }
        val safeFilter = selectedFilter.orEmpty()
        when (safeFilter) {
            "Favoritas" -> list.filter { it.isFavorite }
            "Aprovado" -> list.filter { it.isApproved }
            "Reprovado" -> list.filter { it.isReproved }
            "Recuperação" -> list.filter { it.isRecovery }
            "Em Andamento" -> list.filter { it.isOngoing }
            else -> list
        }.sortedWith(compareByDescending<SubjectUiModel> { it.isFavorite }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.cleanName })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Boletim Escolar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${subjectUiModels.size} disciplinas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
                            if (lastSyncTime.isNotBlank()) {
                                Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                com.example.ui.components.SyncTimestampBadge(rawTimestamp = lastSyncTime)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.Terminal, contentDescription = "Ver Logs do Sistema")
                    }
                    IconButton(onClick = { viewModel.downloadBoletim(context) }) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar PDF")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isWide) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val totalXp = remember(subjectUiModels) {
                        subjectUiModels.sumOf { it.xp }
                    }

                    val listState = rememberLazyListState(
                        cacheWindow = LazyLayoutCacheWindow(ahead = 1000.dp, behind = 400.dp)
                    )

                    LazyColumn(
                        state = listState,
                        flingBehavior = rememberAppFlingBehavior(),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = if (isWide) 800.dp else 600.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                // 1. XP Hero Card
                if (subjectUiModels.isNotEmpty()) {
                    item(key = "xp_card", contentType = "xp_card") {
                        val overviewGradient = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                Brush.linearGradient(overviewGradient),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Desempenho Geral",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Progresso e pontuação acumulada",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "$totalXp",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "XP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Search Bar
                item(key = "search_bar", contentType = "search_bar") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        placeholder = { Text("Pesquisar disciplinas...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar", modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar", modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = {
                                if (searchQuery.equals("femboy", ignoreCase = true)) {
                                    viewModel.setAppTheme(context, com.example.ui.theme.AppTheme.ROBIN_KAWA)
                                    com.example.util.AppSnackbarManager.show("✨ Tema Robin (Kawa) Desbloqueado! ✨")
                                    searchQuery = "" // Clear after unlock
                                }
                            }
                        )
                    )
                }

                // 3. Filter Chips Sticky Header
                stickyHeader(key = "filter_chips", contentType = "filter_header") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(
                                Triple("Todas", "Todas", Icons.Default.Book),
                                Triple("Favoritas", "Favoritas", Icons.Default.Star),
                                Triple("Aprovado", "Aprovado", Icons.Default.Check),
                                Triple("Em Andamento", "Em curso", Icons.Default.Warning),
                                Triple("Recuperação", "Recuperação", Icons.Default.Info),
                                Triple("Reprovado", "Reprovado", Icons.Default.Close)
                            )
                            filters.forEach { (label, value, icon) ->
                                CustomFilterChip(
                                    selected = selectedFilter == label,
                                    onClick = { selectedFilter = label },
                                    label = label,
                                    icon = icon
                                )
                            }
                        }
                    }
                }

                // 4. Empty State or Subject Cards
                if (filteredSubjects.isEmpty()) {
                    if (isRefreshing) {
                        items(
                            count = 5,
                            key = { "skeleton_$it" },
                            contentType = { "skeleton" }
                        ) {
                            com.example.ui.components.SubjectCardSkeleton()
                        }
                    } else {
                        item(key = "empty_state", contentType = "empty_state") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 24.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Nenhuma disciplina encontrada para \"$searchQuery\"." else "Nenhuma disciplina carregada no boletim.\nSincronize sua conta SUAP na tela inicial.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                items(
                    items = filteredSubjects,
                    key = { it.subject.id },
                    contentType = { "subject_card" }
                ) { model ->
                    SubjectBimesterCard(
                        model = model,
                        isDark = isDark,
                        onToggleFavorite = { viewModel.toggleFavorite(model.subject) },
                        onShowDetails = { subj ->
                            selectedSubjectForDetails = subj
                            viewModel.fetchSubjectDetails(subj)
                        },
                        onSubjectClick = onSubjectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }
            }
        }

        if (isWide) {
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                if (currentSubjectForDetails != null) {
                    SubjectDetailsSheet(
                        subject = currentSubjectForDetails,
                        etapaDetails = subjectDetails[currentSubjectForDetails.name ?: ""],
                        onClose = { selectedSubjectForDetails = null },
                        onSubjectClick = onSubjectClick
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Selecione uma disciplina",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Toque em uma disciplina ao lado para visualizar médias, notas bimestrais, faltas e simulação de nota final.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
}

    if (!isWide && currentSubjectForDetails != null) {
        val subject = currentSubjectForDetails
        ModalBottomSheet(
            onDismissRequest = { selectedSubjectForDetails = null },
            sheetState = sheetState
        ) {
            SubjectDetailsSheet(
                subject = subject,
                etapaDetails = subjectDetails[subject.name ?: ""],
                onClose = { selectedSubjectForDetails = null },
                onSubjectClick = onSubjectClick
            )
        }
    }
}

@Composable
fun SubjectBimesterCard(
    model: SubjectUiModel,
    isDark: Boolean,
    onToggleFavorite: () -> Unit,
    onShowDetails: (Subject) -> Unit,
    onSubjectClick: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val subject = model.subject
    var bimestersExpanded by rememberSaveable(subject.id) { mutableStateOf(false) }

    val cardSurface = MaterialTheme.colorScheme.surface
    val cardBgBrush = remember(model.primaryColor, model.secondaryColor, isDark, cardSurface) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    model.primaryColor.copy(alpha = 0.16f),
                    cardSurface,
                    model.secondaryColor.copy(alpha = 0.09f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    model.primaryColor.copy(alpha = 0.08f),
                    cardSurface,
                    model.secondaryColor.copy(alpha = 0.04f)
                )
            )
        }
    }

    val cardBorderBrush = remember(model.primaryColor, model.secondaryColor, isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    model.primaryColor.copy(alpha = 0.50f),
                    model.secondaryColor.copy(alpha = 0.30f),
                    model.primaryColor.copy(alpha = 0.10f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    model.primaryColor.copy(alpha = 0.35f),
                    model.secondaryColor.copy(alpha = 0.20f),
                    model.primaryColor.copy(alpha = 0.10f)
                )
            )
        }
    }

    val semantic = AppSemanticColors
    val (statusBg, statusTextColor) = remember(model.isApproved, model.isReproved, model.isRecovery, model.safeStatus, model.primaryColor, semantic) {
        when {
            model.isApproved -> Pair(semantic.successContainer, semantic.success)
            model.isReproved -> Pair(semantic.dangerContainer, semantic.danger)
            model.isRecovery || model.safeStatus.contains("Final", ignoreCase = true) || (!model.isApproved && model.totalPoints > 0 && model.pointsNeeded > 10) -> Pair(semantic.warningContainer, semantic.warning)
            else -> Pair(model.primaryColor.copy(alpha = 0.15f), model.primaryColor)
        }
    }

    val goalProgressBrush = remember(model.isGoalReached, model.primaryColor, model.secondaryColor) {
        Brush.horizontalGradient(
            colors = if (model.isGoalReached) {
                listOf(semantic.success, semantic.success.copy(alpha = 0.8f))
            } else {
                listOf(model.primaryColor, model.secondaryColor)
            }
        )
    }

    Box(
        modifier = modifier
            .background(cardBgBrush, MaterialTheme.shapes.large)
            .border(BorderStroke(1.dp, cardBorderBrush), MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedSubjectBadge(
                        subjectName = model.cleanName,
                        size = 32.dp,
                        iconSize = 18.dp,
                        customIcon = model.subjectIcon
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = model.cleanName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val labelSmallStyle = MaterialTheme.typography.labelSmall
                        val levelXpText = remember(model.subjectLevel, model.xp, model.badgeColor, isDark, onSurfaceVariantColor, labelSmallStyle) {
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = model.badgeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = labelSmallStyle.fontSize,
                                        background = model.badgeColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                                    )
                                ) {
                                    append(" Nv ${model.subjectLevel} ")
                                }
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = labelSmallStyle.fontSize,
                                        color = onSurfaceVariantColor
                                    )
                                ) {
                                    append("${model.xp} XP")
                                }
                            }
                        }
                        Text(
                            text = levelXpText,
                            style = labelSmallStyle,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .background(statusBg, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (model.isApproved) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = statusTextColor
                                )
                            }
                            Text(
                                text = model.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusTextColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { bimestersExpanded = !bimestersExpanded },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                stateDescription = if (bimestersExpanded) "Expandido" else "Recolhido"
                            }
                    ) {
                        Icon(
                            imageVector = if (bimestersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (bimestersExpanded) "Ocultar notas por bimestre" else "Mostrar notas por bimestre"
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (subject.isFavorite) Icons.Filled.Star else Icons.Default.Star,
                            contentDescription = if (subject.isFavorite) "Remover ${model.cleanName} dos favoritos" else "Adicionar ${model.cleanName} aos favoritos",
                            tint = if (subject.isFavorite) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = bimestersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Notas por bimestre",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        model.bimesters.forEach { bInfo ->
                            BimesterItem(
                                modifier = Modifier.weight(1f),
                                info = bInfo,
                                isDark = isDark
                            )
                        }
                    }
                }
            }

            // Mini Point Goal Progress Line (Progress towards 24.0)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(model.primaryColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = model.pointsRatio)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(brush = goalProgressBrush)
                    )
                }
            }

            // Footer Summary Info: Points, Media, Absences, Details Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f),
                        MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 6.dp, vertical = 2.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Pontos:", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${model.totalPointsDisplay}/24",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (model.isGoalReached) semantic.success else model.primaryColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!model.isGoalReached) {
                        Text(
                            text = "(-${model.pointsNeededDisplay})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val safeAbsences = subject.absences.orEmpty()
                    if (safeAbsences.isNotBlank() && safeAbsences != "0") {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 3.5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${safeAbsences}F",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    val mediaColor = if (model.calculatedMedia >= 6.0) semantic.success else semantic.danger

                    Box(
                        modifier = Modifier
                            .background(mediaColor.copy(alpha = if (isDark) 0.20f else 0.12f), MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Média", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = mediaColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = model.mediaDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = mediaColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .background(model.primaryColor.copy(alpha = if (isDark) 0.22f else 0.14f), MaterialTheme.shapes.extraSmall)
                            .clickable(role = Role.Button) { onShowDetails(subject) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Abrir detalhes de ${model.cleanName}",
                                tint = model.primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = model.primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BimesterItem(
    modifier: Modifier = Modifier,
    info: BimesterUiInfo,
    isDark: Boolean = false
) {
    val gradeBg = if (isDark) info.gradeBgDark else info.gradeBgLight
    val gradeTextColor = if (isDark) info.gradeTextColorDark else info.gradeTextColorLight
    val borderStrokeColor = if (isDark) info.borderStrokeColorDark else info.borderStrokeColorLight

    Column(
        modifier = modifier
            .background(gradeBg, MaterialTheme.shapes.small)
            .border(BorderStroke(0.8.dp, borderStrokeColor), MaterialTheme.shapes.small)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = info.bimesterName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = info.displayGrade,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = gradeTextColor,
            maxLines = 1,
            softWrap = false
        )

        if (info.isRecovered) {
            Text(
                text = "Era ${info.origGradeDisplay}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                softWrap = false
            )
        } else if (info.classGradeDisplay != null) {
            Text(
                text = "T: ${info.classGradeDisplay}",
                style = MaterialTheme.typography.labelSmall,
                color = gradeTextColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailsSheet(
    subject: Subject,
    etapaDetails: List<EtapaDetail>?,
    onClose: () -> Unit,
    onSubjectClick: ((String, String) -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val subjectColor = getSubjectColor(subject.name.orEmpty(), isDark)
    val subjectIcon = getSubjectIcon(subject.name.orEmpty())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                AnimatedSubjectBadge(
                    subjectName = subject.name.orEmpty(),
                    size = 40.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Avaliações e Notas Detalhadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subject.status.orEmpty().ifBlank { "Em curso" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val finalG = parseGradeSafe(subject.finalGrade)
                val semanticColors = AppSemanticColors
                if (finalG != null) {
                    Text(
                        text = "Média: ${formatGradeDisplay(finalG)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (finalG >= 6.0) semanticColors.success else semanticColors.danger,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val abs = subject.absences.orEmpty()
                if (abs.isNotBlank() && abs != "0") {
                    Text(
                        text = "Faltas: $abs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (etapaDetails == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Buscando detalhes do SUAP...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                repeat(3) {
                    com.example.ui.components.SubjectCardSkeleton()
                }
            }
        } else if (etapaDetails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum detalhamento individual encontrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(
                    items = etapaDetails,
                    key = { it.title },
                    contentType = { "etapa_detail" }
                ) { etapa ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val cleanTitle = etapa.title
                                .replace(" - Média Aritmética", "")
                                .replace("Etapa ", "")
                                .let { if (it.firstOrNull()?.isDigit() == true && !it.contains("º")) "${it}º Bimestre" else it }

                            Text(
                                text = cleanTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            etapa.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.defaultMinSize(minWidth = 32.dp)
                                    ) {
                                        Text(
                                            text = item.sigla.ifBlank { "AV" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.descricao.isNotBlank() && item.descricao != "-") item.descricao else item.tipo,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            lineHeight = 14.5.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val subInfo = buildString {
                                            if (item.tipo.isNotBlank() && item.tipo != item.descricao) {
                                                append(item.tipo)
                                            }
                                            if (item.peso.isNotBlank() && item.peso != "-") {
                                                if (isNotEmpty()) append(" • ")
                                                append("Peso ${item.peso}")
                                            }
                                        }
                                        if (subInfo.isNotBlank()) {
                                            Text(
                                                text = subInfo,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    val gradeVal = parseGradeSafe(item.nota)
                                    val itemSemantic = AppSemanticColors
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = when {
                                            gradeVal != null && gradeVal >= 6.0 -> itemSemantic.successContainer
                                            gradeVal != null && gradeVal > 0.0 -> itemSemantic.dangerContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = item.nota.ifBlank { "-" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = when {
                                                gradeVal != null && gradeVal >= 6.0 -> itemSemantic.success
                                                gradeVal != null && gradeVal > 0.0 -> itemSemantic.danger
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
