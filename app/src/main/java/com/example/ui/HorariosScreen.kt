package com.example.ui

import com.example.ui.theme.Spacing

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(
    viewModel: SuapViewModel,
    onNavigateBack: () -> Unit
) {
    val scheduleItems by viewModel.scheduleItems.collectAsState()
    val diarioItems by viewModel.diarioItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Por Dia, 1: Grade Completa, 2: Diários & Locais

    LaunchedEffect(Unit) {
        viewModel.fetchScheduleFromSuap()
    }

    // Days of week (weekdays only)
    val daysOfWeek = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")
    
    // Automatically pick current day of week (1=Sunday, 2=Monday, ...)
    val currentCalendarDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
    val defaultDayIndex = when (currentCalendarDay) {
        java.util.Calendar.MONDAY -> 0
        java.util.Calendar.TUESDAY -> 1
        java.util.Calendar.WEDNESDAY -> 2
        java.util.Calendar.THURSDAY -> 3
        java.util.Calendar.FRIDAY -> 4
        else -> 0
    }
    var selectedDayIndex by rememberSaveable { mutableIntStateOf(defaultDayIndex) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(
                            text = "Horário das Aulas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acesso Offline Ativo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Por Dia", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Grade Completa", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> DayViewContent(
                    scheduleItems = scheduleItems,
                    diarioItems = diarioItems,
                    daysOfWeek = daysOfWeek,
                    selectedDayIndex = selectedDayIndex,
                    onSelectDayIndex = { selectedDayIndex = it }
                )
                1 -> FullGridViewContent(
                    scheduleItems = scheduleItems,
                    diarioItems = diarioItems
                )
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
            
            // Check if it's the same subject and teacher
            val isSameSubject = cleanSubjectName(prev.subjectName).equals(cleanSubjectName(item.subjectName), ignoreCase = true) &&
                    prev.teacher.equals(item.teacher, ignoreCase = true) &&
                    prev.dayOfWeek.equals(item.dayOfWeek, ignoreCase = true) &&
                    prev.shift.equals(item.shift, ignoreCase = true)
            
            // Check for time gap (interval)
            val prevEndTime = parseEndTime(prev.timeSlot)
            val currentStartTime = parseStartTime(item.timeSlot)
            
            // Convert times to comparable numbers (HHmm)
            val prevEndNum = prevEndTime.replace(":", "").toIntOrNull() ?: 0
            val currentStartNum = currentStartTime.replace(":", "").toIntOrNull() ?: 0
            
            val isGap = currentStartNum > prevEndNum && prevEndNum != 0 && currentStartNum != 0
            val isConsecutive = prevEndNum == currentStartNum

            if (isSameSubject && isConsecutive && !item.subjectName.equals("Intervalo", ignoreCase = true)) {
                currentGroup.add(item)
            } else {
                result.add(mergeScheduleGroup(currentGroup))
                
                // Inject interval if there's a gap
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
    onSelectDayIndex: (Int) -> Unit
) {
    val selectedDayName = daysOfWeek[selectedDayIndex]
    
    // Filter items for the selected day and merge consecutive classes for same subject
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Day selector row (fits all 5 days on screen without scrolling)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small, horizontal = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            daysOfWeek.forEachIndexed { index, dayName ->
                val isSelected = index == selectedDayIndex
                val shortName = when (dayName) {
                    "Segunda" -> "Seg"
                    "Terça" -> "Ter"
                    "Quarta" -> "Qua"
                    "Quinta" -> "Qui"
                    "Sexta" -> "Sex"
                    else -> dayName
                }

                Surface(
                    selected = isSelected,
                    onClick = { onSelectDayIndex(index) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = Spacing.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (dayClasses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.extraLarge),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sem aulas cadastradas para $selectedDayName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(dayClasses) { item ->
                    // Find corresponding diario for room info if matching
                    val matchingDiario = diarioItems.find { diario ->
                        diario.component.contains(item.subjectCode, ignoreCase = true) ||
                        (item.subjectName.isNotEmpty() && diario.component.contains(item.subjectName.take(8), ignoreCase = true))
                    }

                    val displayName = cleanSubjectName(item.subjectName)

                    if (item.subjectName.equals("Intervalo", ignoreCase = true)) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${item.timeSlot} - Intervalo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        return@items
                    }

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.medium)) {
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
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.timeSlot,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    if (item.shift.isNotEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = item.shift,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                                            )
                                        }
                                    }
                                }

                                matchingDiario?.room?.takeIf { it.isNotEmpty() }?.let { room ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = room,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            if (item.teacher.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.teacher,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

@Composable
private fun FullGridViewContent(
    scheduleItems: List<ScheduleItem>,
    diarioItems: List<DiarioItem>
) {
    val shifts = listOf("Matutino", "Vespertino", "Noturno")

    val mergedScheduleItems = remember(scheduleItems) {
        scheduleItems.groupBy { it.dayOfWeek }
            .flatMap { (_, items) -> groupConsecutiveScheduleItems(items) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(shifts) { shift ->
            val shiftItems = mergedScheduleItems.filter { 
                it.shift.contains(shift, ignoreCase = true) ||
                (shift == "Matutino" && (it.timeSlot.startsWith("0") || it.timeSlot.startsWith("10") || it.timeSlot.startsWith("11"))) ||
                (shift == "Vespertino" && (it.timeSlot.startsWith("13") || it.timeSlot.startsWith("14") || it.timeSlot.startsWith("15") || it.timeSlot.startsWith("16")))
            }

            if (shiftItems.isNotEmpty()) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(Spacing.medium)) {
                        Text(
                            text = "Turno $shift",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val groupedByTime = shiftItems.groupBy { it.timeSlot }
                        groupedByTime.forEach { (timeSlot, items) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(Spacing.small)) {
                                    Text(
                                        text = timeSlot,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val hasInterval = items.any { it.subjectName.equals("Intervalo", ignoreCase = true) }
                                    if (hasInterval && items.all { it.subjectName.equals("Intervalo", ignoreCase = true) }) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Intervalo",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        items.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = Spacing.xs),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${item.dayOfWeek}:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.width(64.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = cleanSubjectName(item.subjectName),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (item.subjectName.equals("Intervalo", ignoreCase = true)) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (item.teacher.isNotEmpty()) {
                                                        Text(
                                                            text = item.teacher,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}
