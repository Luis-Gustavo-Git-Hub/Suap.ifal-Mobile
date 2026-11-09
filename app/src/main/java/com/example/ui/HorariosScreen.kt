package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EvaluationItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(
    viewModel: SuapViewModel,
    academicEvents: List<com.example.ui.AcademicEvent>,
    onNavigateBack: () -> Unit
) {
    val scheduleItems by viewModel.scheduleItems.collectAsStateWithLifecycle()
    val diarioItems by viewModel.diarioItems.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val isLoadingSchedule by viewModel.isLoadingSchedule.collectAsStateWithLifecycle()
    val isLoadingCalendar by viewModel.isLoadingCalendar.collectAsStateWithLifecycle()
    val scheduleError by viewModel.scheduleError.collectAsStateWithLifecycle()
    val calendarError by viewModel.calendarError.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Grade Semanal, 1: Calendário do Mês

    LaunchedEffect(Unit) {
        viewModel.fetchScheduleFromSuap()
        viewModel.fetchAcademicCalendar()
    }

    val isRefreshing = if (selectedTab == 0) isLoadingSchedule else isLoadingCalendar
    val errorMessage = if (selectedTab == 0) scheduleError else calendarError
    val hasSavedData = if (selectedTab == 0) scheduleItems.isNotEmpty() || diarioItems.isNotEmpty() else academicEvents.isNotEmpty()
    val refreshCurrentTab: () -> Unit = {
        if (selectedTab == 0) viewModel.fetchScheduleFromSuap() else viewModel.fetchAcademicCalendar()
    }

    val daysOfWeek = remember { listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta") }
    
    val currentCalendarDay = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) }
    val defaultDayIndex = remember(currentCalendarDay) {
        when (currentCalendarDay) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            else -> 0
        }
    }
    var selectedDayIndex by rememberSaveable { mutableIntStateOf(defaultDayIndex) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Horário de Aulas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isRefreshing || errorMessage != null || !hasSavedData) MaterialTheme.colorScheme.outline else AppSemanticColors.success, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isRefreshing -> "Atualizando com o SUAP…"
                                    errorMessage != null && hasSavedData -> "Exibindo dados salvos"
                                    hasSavedData -> "Dados salvos no dispositivo"
                                    else -> "Nenhum dado sincronizado"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = refreshCurrentTab, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar esta aba")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (errorMessage != null && !isRefreshing) {
                    com.example.ui.components.AppFeedbackCard(
                        title = "Atualização indisponível",
                        message = errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        actionLabel = "Tentar novamente",
                        onAction = refreshCurrentTab
                    )
                }

                // Animated Bento Segmented Tab Selector
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.5f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.ViewWeek else Icons.Outlined.ViewWeek,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Grade Semanal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Calendário do Mês", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        )
                    }
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) + slideInHorizontally(tween(220)) { width -> if (targetState > initialState) width / 2 else -width / 2 } togetherWith
                        fadeOut(animationSpec = tween(180))
                    },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        0 -> DayViewContent(
                            scheduleItems = scheduleItems,
                            diarioItems = diarioItems,
                            daysOfWeek = daysOfWeek,
                            selectedDayIndex = selectedDayIndex,
                            defaultDayIndex = defaultDayIndex,
                            onSelectDayIndex = { selectedDayIndex = it }
                        )
                        1 -> MonthCalendarContent(
                            academicEvents = academicEvents,
                            evaluations = evaluations
                        )
                    }
                }
            }
        }
    }
}

private fun cleanSubjectName(name: String): String {
    return name.replace(Regex("(?i)^INT[\\.\\s0-9]*\\s*-\\s*"), "").replace(Regex("(?i)^INT[\\.\\s0-9]*"), "").trim()
}

private fun parseStartTime(timeSlot: String): String {
    val parts = timeSlot.split("-", "às", "–")
    return parts.firstOrNull()?.trim() ?: timeSlot
}

private fun parseEndTime(timeSlot: String): String {
    val parts = timeSlot.split("-", "às", "–")
    return parts.lastOrNull()?.trim() ?: timeSlot
}

private fun mergeScheduleGroup(group: List<ScheduleItem>): ScheduleItem {
    if (group.size <= 1) return group.first()
    val first = group.first()
    val last = group.last()

    val startTime = parseStartTime(first.timeSlot)
    val endTime = parseEndTime(last.timeSlot)

    val mergedTimeSlot = "$startTime - $endTime"
    return first.copy(timeSlot = mergedTimeSlot)
}

private fun groupConsecutiveScheduleItems(items: List<ScheduleItem>): List<ScheduleItem> {
    if (items.isEmpty()) return emptyList()
    val result = mutableListOf<ScheduleItem>()
    var currentGroup = mutableListOf<ScheduleItem>()

    for (item in items) {
        if (currentGroup.isEmpty()) {
            currentGroup.add(item)
        } else {
            val prev = currentGroup.last()
            
            val isSameSubject = cleanSubjectName(prev.subjectName).equals(cleanSubjectName(item.subjectName), ignoreCase = true) &&
                    prev.teacher.equals(item.teacher, ignoreCase = true) &&
                    prev.dayOfWeek.equals(item.dayOfWeek, ignoreCase = true) &&
                    prev.shift.equals(item.shift, ignoreCase = true)
            
            val prevEndTime = parseEndTime(prev.timeSlot)
            val currentStartTime = parseStartTime(item.timeSlot)
            
            val prevEndNum = prevEndTime.replace(":", "").toIntOrNull() ?: 0
            val currentStartNum = currentStartTime.replace(":", "").toIntOrNull() ?: 0
            
            val isGap = currentStartNum > prevEndNum && prevEndNum != 0 && currentStartNum != 0
            val isConsecutive = prevEndNum == currentStartNum

            if (isSameSubject && isConsecutive && !item.subjectName.equals("Intervalo", ignoreCase = true)) {
                currentGroup.add(item)
            } else {
                result.add(mergeScheduleGroup(currentGroup))
                
                if (isGap && !prev.subjectName.equals("Intervalo", ignoreCase = true) && !item.subjectName.equals("Intervalo", ignoreCase = true)) {
                    result.add(ScheduleItem(
                        dayOfWeek = item.dayOfWeek,
                        timeSlot = "$prevEndTime - $currentStartTime",
                        shift = item.shift,
                        subjectCode = "",
                        subjectName = "Intervalo",
                        teacher = ""
                    ))
                }
                
                currentGroup = mutableListOf(item)
            }
        }
    }
    if (currentGroup.isNotEmpty()) {
        result.add(mergeScheduleGroup(currentGroup))
    }
    return result
}

@Composable
private fun DayViewContent(
    scheduleItems: List<ScheduleItem>,
    diarioItems: List<DiarioItem>,
    daysOfWeek: List<String>,
    selectedDayIndex: Int,
    defaultDayIndex: Int,
    onSelectDayIndex: (Int) -> Unit
) {
    val selectedDayName = daysOfWeek[selectedDayIndex]
    val isTodaySelected = selectedDayIndex == defaultDayIndex
    
    val rawDayClasses = scheduleItems.filter { item ->
        val day = item.dayOfWeek.lowercase()
        val selected = selectedDayName.lowercase()
        when (selected) {
            "segunda" -> day.contains("segunda") || day.contains("2a") || day.contains("2ª") || day.startsWith("seg")
            "terça" -> day.contains("terça") || day.contains("3a") || day.contains("3ª") || day.startsWith("ter")
            "quarta" -> day.contains("quarta") || day.contains("4a") || day.contains("4ª") || day.startsWith("qua")
            "quinta" -> day.contains("quinta") || day.contains("5a") || day.contains("5ª") || day.startsWith("qui")
            "sexta" -> day.contains("sexta") || day.contains("6a") || day.contains("6ª") || day.startsWith("sex")
            else -> day.contains(selected)
        }
    }

    val dayClasses = remember(rawDayClasses) {
        groupConsecutiveScheduleItems(rawDayClasses)
    }

    val actualClassesCount = remember(dayClasses) {
        dayClasses.count { !it.subjectName.equals("Intervalo", ignoreCase = true) }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Summary Bento Banner with vibrant styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    if (isDark) {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    MaterialTheme.shapes.extraLarge
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$selectedDayName-feira",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (isTodaySelected) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Text(
                                            text = "HOJE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (actualClassesCount > 0) "$actualClassesCount disciplinas agendadas" else "Nenhuma aula para este dia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.7f else 0.9f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$actualClassesCount Aulas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Days Selector Row with Modern Pill Design
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            daysOfWeek.forEachIndexed { index, dayName ->
                val isSelected = index == selectedDayIndex
                val isToday = index == defaultDayIndex
                val shortName = when (dayName) {
                    "Segunda" -> "SEG"
                    "Terça" -> "TER"
                    "Quarta" -> "QUA"
                    "Quinta" -> "QUI"
                    "Sexta" -> "SEX"
                    else -> dayName.take(3).uppercase()
                }

                val pillBg = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else if (isToday) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }

                val pillTextColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    onClick = { onSelectDayIndex(index) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = pillBg,
                    contentColor = pillTextColor,
                    border = if (!isSelected) {
                        BorderStroke(
                            1.dp,
                            if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    } else null
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (isToday && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        if (dayClasses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BeachAccess,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sem aulas em $selectedDayName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aproveite o dia livre para descansar, praticar exercícios ou revisar matérias!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Timeline Class Schedule List
            LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = dayClasses,
                    key = { "${it.dayOfWeek}_${it.timeSlot}_${it.subjectName}" },
                    contentType = { "schedule_item" }
                ) { item ->
                    val matchingDiario = remember(item, diarioItems) {
                        diarioItems.find { diario ->
                            diario.component.contains(item.subjectCode, ignoreCase = true) ||
                            (item.subjectName.isNotEmpty() && diario.component.contains(item.subjectName.take(8), ignoreCase = true))
                        }
                    }

                    val displayName = remember(item.subjectName) { cleanSubjectName(item.subjectName) }

                    if (item.subjectName.equals("Intervalo", ignoreCase = true)) {
                        // Glassmorphic Coffee Break Bento Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            AppSemanticColors.warning.copy(alpha = if (isDark) 0.15f else 0.12f),
                                            AppSemanticColors.warning.copy(alpha = if (isDark) 0.08f else 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        AppSemanticColors.warningContainer.copy(alpha = if (isDark) 0.35f else 0.45f)
                                    ),
                                    MaterialTheme.shapes.large
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(AppSemanticColors.warning.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FreeBreakfast,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = AppSemanticColors.warning
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Intervalo / Recreio",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Momento para lanchar e descansar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = AppSemanticColors.warning.copy(alpha = 0.18f),
                                    border = BorderStroke(0.8.dp, AppSemanticColors.warningContainer.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = item.timeSlot,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppSemanticColors.onWarningContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Class Schedule Timeline Bento Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(
                                    if (isDark) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f)
                                    ),
                                    MaterialTheme.shapes.extraLarge
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Header Row: Time slot pill & Location
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = item.timeSlot,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        if (item.shift.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = item.shift,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    matchingDiario?.room?.takeIf { it.isNotEmpty() }?.let { room ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = room,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Main Content Row with Subject Icon Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AnimatedSubjectBadge(
                                        subjectName = displayName,
                                        size = 42.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.subjectCode.isNotBlank()) {
                                            Text(
                                                text = item.subjectCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Teacher Footer Pill
                                if (item.teacher.isNotEmpty()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = item.teacher,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
}

data class CalendarDisplayItem(
    val title: String,
    val subtitle: String,
    val dateInfo: String,
    val color: String,
    val isExam: Boolean = false,
    val startDate: java.time.LocalDate? = null,
    val endDate: java.time.LocalDate? = null
)

private fun resolveEventRange(
    dateInfo: String,
    rawStart: java.time.LocalDate?,
    rawEnd: java.time.LocalDate?,
    targetYear: Int
): Pair<java.time.LocalDate, java.time.LocalDate>? {
    if (rawStart != null && rawEnd != null) {
        val start = if (rawStart.year < 2025) rawStart.withYear(targetYear) else rawStart
        val end = if (rawEnd.year < 2025) rawEnd.withYear(targetYear) else rawEnd
        return Pair(start, end)
    }

    val clean = dateInfo.trim()
    if (clean.isBlank()) return null

    val rangeRegex = Regex("""(\d{1,2})(?:/(\d{1,2}))?(?:/(\d{2,4}))?\s*(?:a|à|-|até)\s*(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?""", RegexOption.IGNORE_CASE)
    val rangeMatch = rangeRegex.find(clean)
    if (rangeMatch != null) {
        try {
            val d1 = rangeMatch.groupValues[1].toInt()
            val m1 = rangeMatch.groupValues[2].takeIf { it.isNotBlank() }?.toInt()
            val y1 = rangeMatch.groupValues[3].takeIf { it.isNotBlank() }?.toInt()?.let { if (it < 100) it + 2000 else it } ?: targetYear

            val d2 = rangeMatch.groupValues[4].toInt()
            val m2 = rangeMatch.groupValues[5].toInt()
            val y2 = rangeMatch.groupValues[6].takeIf { it.isNotBlank() }?.toInt()?.let { if (it < 100) it + 2000 else it } ?: targetYear

            val finalM1 = m1 ?: m2
            val start = java.time.LocalDate.of(y1, finalM1, d1)
            val end = java.time.LocalDate.of(y2, m2, d2)
            return Pair(start, end)
        } catch (e: Exception) {
            // continue
        }
    }

    val singleRegex = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?""")
    val singleMatch = singleRegex.find(clean)
    if (singleMatch != null) {
        try {
            val d = singleMatch.groupValues[1].toInt()
            val m = singleMatch.groupValues[2].toInt()
            val y = singleMatch.groupValues[3].takeIf { it.isNotBlank() }?.toInt()?.let { if (it < 100) it + 2000 else it } ?: targetYear
            val date = java.time.LocalDate.of(y, m, d)
            return Pair(date, date)
        } catch (e: Exception) {
            // continue
        }
    }

    val isoMatch = Regex("""\d{4}-\d{2}-\d{2}""").find(clean)?.value
    if (isoMatch != null) {
        try {
            val d = java.time.LocalDate.parse(isoMatch)
            return Pair(d, d)
        } catch (e: Exception) {
            // continue
        }
    }

    return null
}

@Composable
fun MonthCalendarContent(
    academicEvents: List<com.example.ui.AcademicEvent>,
    evaluations: List<EvaluationItem> = emptyList()
) {
    val currentMonth = remember { java.time.YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf<java.time.LocalDate?>(null) }
    
    val daysInMonth = remember(selectedMonth) { selectedMonth.lengthOfMonth() }
    val firstDayOfMonth = remember(selectedMonth) { selectedMonth.atDay(1) }
    val startDayOfWeek = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value % 7 }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Show only events supplied by the academic calendar and evaluations.
    val combinedEvents = remember(academicEvents, evaluations, selectedMonth) {
        val list = mutableListOf<CalendarDisplayItem>()

        // 1. Add scheduled exams / evaluations (marked with isExam = true, prominent color #FF4081)
        evaluations.forEach { ev ->
            val range = resolveEventRange(ev.date, null, null, selectedMonth.year)
            list.add(
                CalendarDisplayItem(
                    title = cleanSubjectName(ev.subjectName),
                    subtitle = ev.title.ifBlank { ev.type },
                    dateInfo = ev.date,
                    color = "#FF4081",
                    isExam = true,
                    startDate = range?.first,
                    endDate = range?.second
                )
            )
        }

        // 2. Add academic calendar events
        academicEvents.forEach { ev ->
            val range = resolveEventRange(ev.dateInfo, ev.startDate, ev.endDate, selectedMonth.year)
            val isExam = ev.color == "#FF4081" || ev.color == "#FF80AB" || 
                         ev.description.contains("Prova", ignoreCase = true) || 
                         ev.description.contains("Avaliaç", ignoreCase = true) ||
                         ev.description.contains("Exame", ignoreCase = true)
            list.add(
                CalendarDisplayItem(
                    title = if (isExam) "Avaliação / Prova" else "Calendário Acadêmico",
                    subtitle = ev.description,
                    dateInfo = ev.dateInfo,
                    color = if (isExam) "#FF4081" else ev.color,
                    isExam = isExam,
                    startDate = range?.first ?: ev.startDate,
                    endDate = range?.second ?: ev.endDate
                )
            )
        }

        list.distinctBy { (it.startDate?.toString() ?: it.dateInfo) + it.subtitle }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), flingBehavior = rememberAppFlingBehavior())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Navigation Card (Premium Bento Style)
        var expanded by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        selectedMonth = selectedMonth.minusMonths(1) 
                        selectedDay = null // Reset selected day on month change
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Mês anterior",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Box(contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${selectedMonth.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.2).sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        val months = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
                        months.forEachIndexed { index, monthName ->
                            DropdownMenuItem(
                                text = { Text(monthName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedMonth = java.time.YearMonth.of(selectedMonth.year, index + 1)
                                    selectedDay = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                IconButton(
                    onClick = { 
                        selectedMonth = selectedMonth.plusMonths(1) 
                        selectedDay = null
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Próximo mês",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Calendar Container (Ultra-Premium Redesigned Grid)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Days of Week Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Calendar Grid
                val totalCells = startDayOfWeek + daysInMonth
                val rows = Math.ceil(totalCells / 7.0).toInt()

                for (i in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (j in 0 until 7) {
                            val cellIndex = i * 7 + j
                            val dayNumber = cellIndex - startDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = selectedMonth.atDay(dayNumber)
                                val isToday = date == java.time.LocalDate.now()
                                val isSelected = selectedDay == date
                                val dayItems = combinedEvents.filter { item ->
                                    val start = item.startDate
                                    val end = item.endDate
                                    start != null && end != null && !date.isBefore(start) && !date.isAfter(end)
                                }
                                val hasExams = dayItems.any { it.isExam }
                                val hasEvents = dayItems.isNotEmpty()
                                
                                val firstEventColor = remember(dayItems) {
                                    try {
                                        if (hasExams) Color(0xFFFF4081)
                                        else dayItems.firstOrNull { it.color.isNotBlank() && it.color != "#E0E0E0" }?.color?.let {
                                            Color(android.graphics.Color.parseColor(it))
                                        }
                                    } catch (e: Exception) { null }
                                }

                                val todayGradient = Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )

                                // Interactive cell colors
                                val cellBgModifier = when {
                                    isToday -> Modifier.background(todayGradient)
                                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                                    hasExams -> Modifier.background(Color(0xFFFF4081).copy(alpha = if (isDark) 0.22f else 0.15f))
                                    hasEvents -> Modifier.background(firstEventColor?.copy(alpha = if (isDark) 0.16f else 0.12f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                }

                                val cellText = when {
                                    isToday -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    hasExams -> Color(0xFFFF4081)
                                    hasEvents -> firstEventColor ?: MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.0f)
                                        .padding(4.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .then(cellBgModifier)
                                        .clickable {
                                            selectedDay = if (isSelected) null else date
                                        }
                                        .then(
                                            if (isToday) Modifier.shadow(6.dp, MaterialTheme.shapes.medium)
                                            else if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                            else if (hasExams) Modifier.border(1.5.dp, Color(0xFFFF4081).copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                                            else if (hasEvents) Modifier.border(1.dp, (firstEventColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                                            else Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isToday || isSelected || hasExams) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = cellText
                                        )
                                        
                                        // Dots Indicator for Day Events & Exams
                                        if (hasEvents) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                dayItems.take(3).forEach { ev ->
                                                    val dotColor = if (isToday) {
                                                        Color.White
                                                    } else if (ev.isExam) {
                                                        Color(0xFFFF4081)
                                                    } else {
                                                        try {
                                                            Color(android.graphics.Color.parseColor(ev.color))
                                                        } catch (e: Exception) {
                                                            firstEventColor ?: Color.Gray
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (ev.isExam) 5.5.dp else 4.5.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Provas do Mês Bento Spotlight
        val monthExams = remember(selectedMonth, combinedEvents) {
            combinedEvents.filter { item ->
                item.isExam && item.startDate != null && 
                !item.startDate.isAfter(selectedMonth.atEndOfMonth()) && 
                !item.endDate!!.isBefore(selectedMonth.atDay(1))
            }.sortedBy { it.startDate }
        }

        if (selectedDay == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(0xFFFF4081).copy(alpha = if (isDark) 0.12f else 0.08f),
                border = BorderStroke(1.2.dp, Color(0xFFFF4081).copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFF4081).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4081),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Provas de ${selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("pt", "BR")).replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (monthExams.isNotEmpty()) "${monthExams.size} avaliação(ões) agendada(s)" else "Nenhuma prova agendada este mês",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFFFF4081).copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, Color(0xFFFF4081).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${monthExams.size} Prova(s)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF4081),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (monthExams.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFFF4081).copy(alpha = 0.2f))
                        
                        monthExams.forEach { exam ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        exam.startDate?.let { selectedDay = it }
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, Color(0xFFFF4081).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = exam.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = exam.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(0xFFFF4081).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = exam.dateInfo,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFF4081),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Academic Events Bento Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (selectedDay != null) {
                                "Eventos em ${selectedDay!!.dayOfMonth}/${selectedDay!!.monthValue}"
                            } else "Eventos do Mês",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (selectedDay != null) {
                        Surface(
                            onClick = { selectedDay = null },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "Ver Todos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                
                val filteredEvents = remember(selectedMonth, selectedDay, combinedEvents) {
                    combinedEvents.filter { item ->
                        val start = item.startDate
                        val end = item.endDate
                        if (start != null && end != null) {
                            if (selectedDay != null) {
                                !selectedDay!!.isBefore(start) && !selectedDay!!.isAfter(end)
                            } else {
                                !start.isAfter(selectedMonth.atEndOfMonth()) && !end.isBefore(selectedMonth.atDay(1))
                            }
                        } else false
                    }.sortedWith(compareBy({ it.startDate }, { !it.isExam }))
                }

                if (filteredEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedDay != null) "Nenhum evento agendado para este dia" else "Nenhum evento acadêmico este mês",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredEvents.forEach { item ->
                            val rawColor = remember(item) {
                                try {
                                    if (item.isExam) Color(0xFFFF4081)
                                    else if (item.color.isNotBlank() && item.color != "#E0E0E0") {
                                        Color(android.graphics.Color.parseColor(item.color))
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) { null }
                            }

                            val eventColor = rawColor ?: MaterialTheme.colorScheme.primary

                            // Premium Bento Event Card with gorgeous styling
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = if (item.isExam) {
                                    Color(0xFFFF4081).copy(alpha = if (isDark) 0.12f else 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.25f else 0.35f)
                                },
                                border = BorderStroke(
                                    if (item.isExam) 1.2.dp else 1.dp,
                                    if (item.isExam) Color(0xFFFF4081).copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Accent color bar on the left side
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight()
                                            .align(Alignment.CenterVertically)
                                            .background(eventColor)
                                            .size(width = 5.dp, height = if (item.isExam) 80.dp else 70.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (item.isExam) {
                                                Surface(
                                                    shape = MaterialTheme.shapes.small,
                                                    color = Color(0xFFFF4081).copy(alpha = 0.18f),
                                                    border = BorderStroke(0.8.dp, Color(0xFFFF4081).copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Assignment,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFF4081),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "PROVA / AVALIAÇÃO",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFFF4081)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Date Indicator Badge
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = eventColor.copy(alpha = 0.12f),
                                                border = BorderStroke(0.8.dp, eventColor.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = item.dateInfo,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = eventColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (item.isExam && item.title.isNotBlank()) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = item.subtitle,
                                            style = if (item.isExam) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                                            fontWeight = if (item.isExam) FontWeight.Medium else FontWeight.Bold,
                                            color = if (item.isExam) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
