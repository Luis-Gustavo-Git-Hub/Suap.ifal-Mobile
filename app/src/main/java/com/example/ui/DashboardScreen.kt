package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.animation.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Psychology

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import com.example.data.Subject
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FREQUENCY_NUMBER_PATTERN = Regex("\\d+([.,]\\d+)?")

@Composable
fun XpRankHeaderWidget(
    subjects: List<Subject>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val rankInfo = remember(subjects) { calculateProfileRank(subjects) }
    val isMaxLevel = rankInfo.level >= 5
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        onClick = onClick,
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = if (isDark) rankInfo.badgeColor.copy(alpha = 0.16f) else rankInfo.badgeColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, rankInfo.badgeColor.copy(alpha = if (isDark) 0.38f else 0.28f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .widthIn(min = 120.dp, max = 155.dp)
        ) {
            // Top Row: Emblem + Level + Total XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(rankInfo.badgeColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rankInfo.badgeIcon,
                            contentDescription = rankInfo.rankName,
                            tint = rankInfo.badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "Nv. ${rankInfo.level}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankInfo.badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = "${rankInfo.currentXp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        color = rankInfo.badgeColor.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = rankInfo.progress)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    rankInfo.badgeColor.copy(alpha = 0.8f),
                                    rankInfo.badgeColor
                                )
                            ),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Subtitle: Rank Name & XP to next level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rankInfo.rankShortName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankInfo.badgeColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isMaxLevel) "Máx." else "Faltam ${rankInfo.xpRemaining}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    studentName: String,
    campusName: String,
    globalFrequency: String,
    globalAbsences: String,
    totalClasses: String,
    passedClasses: String,
    subjectsCount: Int,
    lastSyncTime: String,
    hasSigaaCredentials: Boolean,
    hasSuapCredentials: Boolean,
    subjects: List<Subject> = emptyList(),
    scheduleItems: List<ScheduleItem> = emptyList(),
    avisos: List<com.example.data.Aviso> = emptyList(),
    professorContacts: List<com.example.data.ProfessorContact> = emptyList(),
    evaluations: List<com.example.data.EvaluationItem> = emptyList(),
    totalXp: Int = 0,
    statistics: com.example.domain.StatisticsSnapshot = com.example.domain.StatisticsSnapshot.Empty,
    onNavigateToBoletim: () -> Unit,
    onNavigateToAssuntos: () -> Unit,
    onNavigateToAlmoco: () -> Unit,
    onNavigateToProfile: (Boolean) -> Unit = {},
    onNavigateToJustificativa: () -> Unit = {},
    onNavigateToSituacao: () -> Unit = {},
    onNavigateToHorarios: () -> Unit = {},
    onNavigateToAvisos: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onSyncClick: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    termsAccepted: Boolean = true
) {
    val isSynced = subjectsCount > 0 && android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") != null
    var showLevelsDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val isWide = com.example.ui.components.isWideScreen()

    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (isWide) 1100.dp else 600.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState(), flingBehavior = rememberAppFlingBehavior()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.example.ui.components.AppPageHeader(
                    eyebrow = campusName,
                    title = "Olá, $studentName",
                    titleMaxLines = 3,
                    subtitle = "Veja seu resumo acadêmico e os próximos compromissos.",
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.example.ui.components.SyncTimestampBadge(rawTimestamp = lastSyncTime)
                            if (!isWide) {
                                XpRankHeaderWidget(
                                    subjects = subjects,
                                    onClick = { showLevelsDialog = true }
                                )
                            }
                        }
                    },
                    trailingContent = if (isWide) {
                        {
                            XpRankHeaderWidget(
                                subjects = subjects,
                                onClick = { showLevelsDialog = true }
                            )
                        }
                    } else null
                )

            // Animated Sync Status Card (Replaces the raw loading spinner)
            AnimatedVisibility(
                visible = isRefreshing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "syncRotation"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronizando com o SUAP...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                            Text(
                                text = "Atualizando notas, faltas e avisos acadêmicos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                        
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Global Search Bar
            com.example.ui.components.DashboardSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (searchQuery.isNotBlank()) {
                com.example.ui.components.DashboardSearchResults(
                    query = searchQuery,
                    subjects = subjects,
                    avisos = avisos,
                    scheduleItems = scheduleItems,
                    professorContacts = professorContacts,
                    onNavigateToSubject = onNavigateToBoletim,
                    onNavigateToAviso = onNavigateToAvisos
                )
            } else if (isWide) {
                // Wide adaptive 2-column Bento Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column (Today, Boletim, Documentos)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.components.DashboardTodayCard(
                            scheduleItems = scheduleItems,
                            avisos = avisos,
                            subjects = subjects,
                            evaluations = evaluations,
                            onNavigateToHorarios = onNavigateToHorarios,
                            onNavigateToAvisos = onNavigateToAvisos,
                            onNavigateToBoletim = onNavigateToBoletim
                        )

                        if (subjectsCount == 0 && isRefreshing) {
                            com.example.ui.components.DashboardBentoSkeleton()
                        }

                        BoletimCard(
                            modifier = Modifier.fillMaxWidth(),
                            subjectsCount = subjectsCount,
                            hasSuapCredentials = hasSuapCredentials,
                            onClick = onNavigateToBoletim,
                            onNavigateToProfile = { onNavigateToProfile(false) }
                        )

                        AssuntosCard(
                            modifier = Modifier.fillMaxWidth(),
                            hasSuapCredentials = hasSuapCredentials,
                            onClick = onNavigateToAssuntos,
                            onNavigateToProfile = { onNavigateToProfile(false) }
                        )
                    }

                    // Right Column (Frequência, Almoço, Horários, Amigos)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FrequenciaCard(
                            modifier = Modifier.fillMaxWidth(),
                            globalFrequency = globalFrequency,
                            globalAbsences = globalAbsences,
                            totalClasses = totalClasses,
                            passedClasses = passedClasses,
                            hasSuapCredentials = hasSuapCredentials,
                            onNavigateToProfile = { onNavigateToProfile(false) },
                            onNavigateToJustificativa = onNavigateToJustificativa,
                            onNavigateToSituacao = onNavigateToSituacao
                        )

                        MarcarAlmocoCard(
                            modifier = Modifier.fillMaxWidth(),
                            hasSigaaCredentials = hasSigaaCredentials,
                            onClick = onNavigateToAlmoco,
                            onNavigateToProfile = onNavigateToProfile
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorariosSquareCard(
                                modifier = Modifier.weight(1f),
                                hasSuapCredentials = hasSuapCredentials,
                                onClick = onNavigateToHorarios,
                                onNavigateToProfile = { onNavigateToProfile(false) }
                            )
                            FriendsSquareCard(
                                modifier = Modifier.weight(1f),
                                enabled = termsAccepted,
                                onClick = onNavigateToFriends
                            )
                        }
                        StatisticsQuickAccessCard(statistics = statistics, onClick = onNavigateToStatistics)
                    }
                }
            } else {
                // Compact single-column Bento Grid
                com.example.ui.components.DashboardTodayCard(
                    scheduleItems = scheduleItems,
                    avisos = avisos,
                    subjects = subjects,
                    evaluations = evaluations,
                    onNavigateToHorarios = onNavigateToHorarios,
                    onNavigateToAvisos = onNavigateToAvisos,
                    onNavigateToBoletim = onNavigateToBoletim
                )

                if (subjectsCount == 0 && isRefreshing) {
                    com.example.ui.components.DashboardBentoSkeleton()
                    com.example.ui.components.DashboardBentoSkeleton()
                }

                BoletimCard(
                    modifier = Modifier.fillMaxWidth(),
                    subjectsCount = subjectsCount,
                    hasSuapCredentials = hasSuapCredentials,
                    onClick = onNavigateToBoletim,
                    onNavigateToProfile = { onNavigateToProfile(false) }
                )
                
                FrequenciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    globalFrequency = globalFrequency,
                    globalAbsences = globalAbsences,
                    totalClasses = totalClasses,
                    passedClasses = passedClasses,
                    hasSuapCredentials = hasSuapCredentials,
                    onNavigateToProfile = { onNavigateToProfile(false) },
                    onNavigateToJustificativa = onNavigateToJustificativa,
                    onNavigateToSituacao = onNavigateToSituacao
                )

                AssuntosCard(
                    modifier = Modifier.fillMaxWidth(),
                    hasSuapCredentials = hasSuapCredentials,
                    onClick = onNavigateToAssuntos,
                    onNavigateToProfile = { onNavigateToProfile(false) }
                )
                
                MarcarAlmocoCard(
                    modifier = Modifier.fillMaxWidth(),
                    hasSigaaCredentials = hasSigaaCredentials,
                    onClick = onNavigateToAlmoco,
                    onNavigateToProfile = onNavigateToProfile
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorariosSquareCard(
                        modifier = Modifier.weight(1f),
                        hasSuapCredentials = hasSuapCredentials,
                        onClick = onNavigateToHorarios,
                        onNavigateToProfile = { onNavigateToProfile(false) }
                    )
                    FriendsSquareCard(
                        modifier = Modifier.weight(1f),
                        enabled = termsAccepted,
                        onClick = onNavigateToFriends
                    )
                }
                StatisticsQuickAccessCard(statistics = statistics, onClick = onNavigateToStatistics)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showLevelsDialog) {
            AcademicLevelsDialog(subjects = subjects, onDismiss = { showLevelsDialog = false })
        }
    }
}
}

enum class ModuleIconAnimType {
    PULSE_GLOW,
    FLOAT_BOUNCE,
    GENTLE_TILT,
    CLOCK_SWAY,
    SPARKLE_RING
}

@Composable
fun AnimatedModuleBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    animType: ModuleIconAnimType = ModuleIconAnimType.PULSE_GLOW,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    iconTint: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "moduleIconAnim")

    val scale by when (animType) {
        ModuleIconAnimType.PULSE_GLOW, ModuleIconAnimType.SPARKLE_RING -> {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.09f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
        }
        ModuleIconAnimType.FLOAT_BOUNCE -> {
            infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatScale"
            )
        }
        else -> remember { mutableStateOf(1f) }
    }

    val rotation by when (animType) {
        ModuleIconAnimType.GENTLE_TILT -> {
            infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "tilt"
            )
        }
        ModuleIconAnimType.CLOCK_SWAY -> {
            infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "clockSway"
            )
        }
        else -> remember { mutableStateOf(0f) }
    }

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val offsetY by when (animType) {
        ModuleIconAnimType.FLOAT_BOUNCE -> {
            infiniteTransition.animateFloat(
                initialValue = 1.5f,
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )
        }
        else -> remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Dynamic Glowing Aura ring
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .graphicsLayer {
                    scaleX = scale * 1.08f
                    scaleY = scale * 1.08f
                    alpha = glowAlpha
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.7f),
                            gradientColors.last().copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = MaterialTheme.shapes.large
                )
        )

        // Main Icon Container with 3D Glossy Multi-Stop Gradient & Specular Border
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    translationY = offsetY
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                Color.White.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.15f)
                            )
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top highlight reflection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.48f)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            )

            // Vector Icon
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun BoletimCard(
    modifier: Modifier = Modifier, 
    subjectsCount: Int, 
    hasSuapCredentials: Boolean, 
    onClick: () -> Unit, 
    onNavigateToProfile: () -> Unit
) {
    val isSynced = subjectsCount > 0 && android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") != null
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (hasSuapCredentials) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.clickable {
            if (hasSuapCredentials) {
                onClick()
            } else {
                com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para ver suas notas, médias e situação do boletim.")
                onNavigateToProfile()
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedModuleBadge(
                    icon = Icons.Default.School,
                    contentDescription = "Boletim",
                    gradientColors = if (hasSuapCredentials) {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    } else {
                        listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                    },
                    animType = ModuleIconAnimType.FLOAT_BOUNCE,
                    size = 40.dp,
                    iconSize = 22.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Boletim Escolar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasSuapCredentials) {
                            "Notas, médias e faltas"
                        } else {
                            "Entre no SUAP para ver"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = if (hasSuapCredentials) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                contentDescription = if (hasSuapCredentials) "Abrir boletim" else "Bloqueado: entre no SUAP para ver",
                tint = contentColor.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FrequenciaCard(
    modifier: Modifier = Modifier,
    globalFrequency: String,
    globalAbsences: String,
    totalClasses: String,
    passedClasses: String,
    hasSuapCredentials: Boolean = true,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToJustificativa: () -> Unit = {},
    onNavigateToSituacao: () -> Unit = {},
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (hasSuapCredentials) {
        AppSemanticColors.warningContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (hasSuapCredentials) {
        AppSemanticColors.onWarningContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val freqFloat = run {
        val numberMatch = FREQUENCY_NUMBER_PATTERN.find(globalFrequency)
        val parsed = numberMatch?.value?.replace(",", ".")?.toFloatOrNull()
        if (parsed != null && parsed > 0f) {
            parsed.coerceIn(0f, 100f)
        } else if (parsed == 0f && globalFrequency.contains("0")) {
            0f
        } else {
            val total = passedClasses.toIntOrNull() ?: totalClasses.toIntOrNull() ?: 0
            val faltas = globalAbsences.toIntOrNull() ?: 0
            if (total > 0) {
                ((total - faltas).coerceAtLeast(0).toFloat() / total.toFloat()) * 100f
            } else if (faltas == 0 && hasSuapCredentials) {
                100f
            } else {
                0f
            }
        }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = (freqFloat / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "global_frequency_progress"
    )

    val progressColor = if (freqFloat < 75f && freqFloat > 0f) {
        MaterialTheme.colorScheme.error
    } else {
        AppSemanticColors.warning
    }
    
    Card(
        modifier = modifier.clickable {
            if (!hasSuapCredentials) {
                com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para acompanhar sua frequência global e faltas.")
                onNavigateToProfile()
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Icon + Titles + Circular Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedModuleBadge(
                        icon = Icons.Default.EventAvailable,
                        contentDescription = "Frequência",
                        gradientColors = if (hasSuapCredentials) {
                            listOf(AppSemanticColors.warning, MaterialTheme.colorScheme.tertiary)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                        },
                        animType = ModuleIconAnimType.PULSE_GLOW,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frequência Global",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusText = if (!hasSuapCredentials) {
                            "Entre no SUAP para ver"
                        } else if (freqFloat >= 75f || (freqFloat == 0f && (globalAbsences.toIntOrNull() ?: 0) == 0)) {
                            "Frequência regular (≥ 75%)"
                        } else {
                            "Atenção: abaixo de 75%"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (freqFloat < 75f && freqFloat > 0f) MaterialTheme.colorScheme.error else contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                    if (hasSuapCredentials) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = progressColor,
                            trackColor = contentColor.copy(alpha = 0.15f),
                            strokeWidth = 3.5.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        val displayText = run {
                            val numberMatch = FREQUENCY_NUMBER_PATTERN.find(globalFrequency)
                            val parsed = numberMatch?.value?.replace(",", ".")?.toFloatOrNull()
                            when {
                                parsed != null -> {
                                    if (parsed % 1f == 0f) "${parsed.toInt()}%" else String.format(java.util.Locale.US, "%.1f%%", parsed).replace(".", ",")
                                }
                                freqFloat > 0f -> {
                                    if (freqFloat % 1f == 0f) "${freqFloat.toInt()}%" else String.format(java.util.Locale.US, "%.1f%%", freqFloat).replace(".", ",")
                                }
                                (globalAbsences.toIntOrNull() ?: 0) == 0 && hasSuapCredentials -> {
                                    "100%"
                                }
                                else -> "-"
                            }
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = progressColor,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado: entre no SUAP para ver",
                            tint = contentColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Metrics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentColor.copy(alpha = 0.08f), MaterialTheme.shapes.small)
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = globalAbsences.ifEmpty { "0" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if ((globalAbsences.toIntOrNull() ?: 0) > 0) MaterialTheme.colorScheme.error else contentColor,
                        maxLines = 1
                    )
                    Text(
                        text = "Faltas",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(contentColor.copy(alpha = 0.25f))
                )
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = passedClasses.ifEmpty { "-" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        maxLines = 1
                    )
                    Text(
                        text = "Aulas dadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(contentColor.copy(alpha = 0.25f))
                )
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalClasses.ifEmpty { "-" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        maxLines = 1
                    )
                    Text(
                        text = "Carga total",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
            
            // Quick action buttons
            if (hasSuapCredentials) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToJustificativa,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppSemanticColors.warning,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Justificar Falta", 
                            fontWeight = FontWeight.ExtraBold, 
                            maxLines = 1, 
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToSituacao,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor
                        ),
                        border = BorderStroke(1.2.dp, contentColor.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Situação", 
                            fontWeight = FontWeight.ExtraBold, 
                            maxLines = 1, 
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsQuickAccessCard(
    statistics: com.example.domain.StatisticsSnapshot,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Estatísticas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Resumo do seu desempenho acadêmico", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Abrir Estatísticas", tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            if (statistics.hasData) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatisticsPreviewMetric("Média", statistics.overallAverage?.let { String.format(java.util.Locale("pt", "BR"), "%.1f", it) } ?: "—", Modifier.weight(1f))
                    StatisticsPreviewMetric("Frequência", statistics.globalFrequency?.let { "${it.toInt()}%" } ?: "—", Modifier.weight(1f))
                    StatisticsPreviewMetric("Em risco", statistics.atRiskCount.toString(), Modifier.weight(1f))
                }
            } else {
                Text(
                    "Sincronize para ver suas estatísticas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun StatisticsPreviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
    }
}

@Composable
fun AssuntosCard(modifier: Modifier = Modifier, hasSuapCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: () -> Unit) {
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
    val gradientColors = if (hasSuapCredentials) {
        listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
    } else {
        listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = modifier.clickable {
            if (hasSuapCredentials) {
                onClick()
            } else {
                com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para ver conteúdos e materiais das aulas.")
                onNavigateToProfile()
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    AnimatedModuleBadge(
                        icon = Icons.Default.LibraryBooks,
                        contentDescription = "Materiais e Aulas",
                        gradientColors = if (hasSuapCredentials) {
                            listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                        },
                        animType = ModuleIconAnimType.FLOAT_BOUNCE,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Materiais e Aulas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasSuapCredentials) "Conteúdos e arquivos" else "Entre no SUAP para ver",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (hasSuapCredentials) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                    contentDescription = if (hasSuapCredentials) "Abrir materiais" else "Bloqueado: entre no SUAP para ver",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MarcarAlmocoCard(modifier: Modifier = Modifier, hasSigaaCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: (Boolean) -> Unit) {
    val containerColor = if (hasSigaaCredentials) AppSemanticColors.warning else MaterialTheme.colorScheme.surfaceVariant
    val gradientColors = if (hasSigaaCredentials) {
        listOf(
            AppSemanticColors.warning, // Vibrant Amber Yellow
            AppSemanticColors.onWarningContainer  // Golden Amber Yellow
        )
    } else {
        listOf(Color.Gray, Color.DarkGray)
    }

    androidx.compose.material3.Card(
        modifier = modifier.clickable { 
            if (hasSigaaCredentials) {
                onClick()
            } else {
                com.example.util.AppSnackbarManager.show("Entre no SIGAA no Perfil para agendar suas refeições no refeitório.")
                onNavigateToProfile(true)
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    AnimatedModuleBadge(
                        icon = Icons.Default.LunchDining,
                        contentDescription = "Refeitório",
                        gradientColors = if (hasSigaaCredentials) {
                            listOf(AppSemanticColors.warning, AppSemanticColors.warningContainer)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                        },
                        animType = ModuleIconAnimType.GENTLE_TILT,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Text(
                            text = "Refeitório (SIGAA)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        androidx.compose.material3.Text(
                            text = if (hasSigaaCredentials) "Marque seus almoços" else "Entre no SIGAA para ver",
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                androidx.compose.material3.Icon(
                    imageVector = if (hasSigaaCredentials) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                    contentDescription = if (hasSigaaCredentials) "Abrir almoço" else "Bloqueado: entre no SIGAA para ver",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HorariosSquareCard(
    modifier: Modifier = Modifier,
    hasSuapCredentials: Boolean = true,
    onClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val gradientColors = if (hasSuapCredentials) {
        listOf(
            MaterialTheme.colorScheme.primary, // Vibrant Teal
            MaterialTheme.colorScheme.primaryContainer  // Deep Emerald Teal
        )
    } else {
        listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = modifier.clickable { 
            if (hasSuapCredentials) {
                onClick()
            } else {
                com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para visualizar sua grade de horários.")
                onNavigateToProfile()
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedModuleBadge(
                        icon = Icons.Default.CalendarToday,
                        contentDescription = "Horário das Aulas",
                        gradientColors = if (hasSuapCredentials) {
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                        },
                        animType = ModuleIconAnimType.CLOCK_SWAY,
                        size = 38.dp,
                        iconSize = 20.dp
                    )
                    Icon(
                        imageVector = if (hasSuapCredentials) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                        contentDescription = if (hasSuapCredentials) "Abrir horários" else "Bloqueado: entre no SUAP para ver",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasSuapCredentials) "Horário das aulas" else "Entre no SUAP para ver",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FriendsSquareCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val gradientColors = if (enabled) {
        listOf(
            MaterialTheme.colorScheme.secondary, // Vibrant Indigo
            MaterialTheme.colorScheme.secondaryContainer  // Deep Indigo / Purple
        )
    } else {
        listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedModuleBadge(
                        icon = Icons.Default.Group,
                        contentDescription = "Amigos",
                        gradientColors = if (enabled) {
                            listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outline)
                        },
                        animType = ModuleIconAnimType.SPARKLE_RING,
                        size = 38.dp,
                        iconSize = 20.dp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Amigos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (enabled) "Rede acadêmica" else "Termos pendentes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AcademicLevelsDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit
) {
    val totalSubjects = subjects.size.coerceAtLeast(1)
    val lvl2 = totalSubjects * 140
    val lvl3 = totalSubjects * 240
    val lvl4 = totalSubjects * 320
    val lvl5 = totalSubjects * 360

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AppSemanticColors.warning,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Níveis de Progresso Acadêmico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Informational Warning Card explaining cumulative XP vs current bimester average
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Como funciona o sistema?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seu nível é baseado no XP total acumulado (soma das notas de todos os bimesteres multiplicadas por 10). Ao longo do ano letivo, conforme novas notas são lançadas pelos professores, seu XP aumenta e você progride para novos ranks!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                LevelDialogInfoRow(
                    level = 1,
                    name = "Calouro Curioso",
                    xpRange = "0 - ${lvl2 - 1} XP",
                    avgGrade = "Média Anual < 3.50",
                    color = AppSemanticColors.success,
                    icon = androidx.compose.material.icons.Icons.Filled.LocalFlorist
                )
                LevelDialogInfoRow(
                    level = 2,
                    name = "Aprendiz Dedicado",
                    xpRange = "$lvl2 - ${lvl3 - 1} XP",
                    avgGrade = "Média Anual 3.50 a 5.9",
                    color = AppSemanticColors.info,
                    icon = androidx.compose.material.icons.Icons.Filled.MenuBook
                )
                LevelDialogInfoRow(
                    level = 3,
                    name = "Estudante de Elite",
                    xpRange = "$lvl3 - ${lvl4 - 1} XP",
                    avgGrade = "Média Anual 6.00 a 7.9",
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = androidx.compose.material.icons.Icons.Filled.School
                )
                LevelDialogInfoRow(
                    level = 4,
                    name = "Mestre Acadêmico",
                    xpRange = "$lvl4 - ${lvl5 - 1} XP",
                    avgGrade = "Média Anual 8.00 a 8.9",
                    color = AppSemanticColors.warning,
                    icon = androidx.compose.material.icons.Icons.Filled.Psychology
                )
                LevelDialogInfoRow(
                    level = 5,
                    name = "Lenda do Campus",
                    xpRange = "$lvl5+ XP",
                    avgGrade = "Média Anual ≥ 9.00",
                    color = AppSemanticColors.warning,
                    icon = androidx.compose.material.icons.Icons.Filled.WorkspacePremium
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Valores calculados dinamicamente para $totalSubjects ${if (totalSubjects == 1) "disciplina" else "disciplinas"}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun LevelDialogInfoRow(
    level: Int,
    name: String,
    xpRange: String,
    avgGrade: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = xpRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(modifier = Modifier.size(3.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape))
                    Text(
                        text = avgGrade,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {
                Text(
                    text = "Lvl $level",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
