package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Aviso
import com.example.data.EvaluationItem
import com.example.data.ProfessorContact
import com.example.data.Subject
import com.example.ui.ScheduleItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val SUBJECT_CODE_PREFIX_REGEX = Regex("(?i)^INT[\\.\\s0-9]*\\s*-\\s*")

/**
 * Card "Hoje" ultra-compacto exibindo:
 * 1. Próxima aula
 * 2. As 2 avaliações mais recentes marcadas com data (SUAP Calendário de Avaliações)
 * 3. Aviso mais recente e Última nota lado a lado
 */
@Composable
fun DashboardTodayCard(
    scheduleItems: List<ScheduleItem>,
    avisos: List<Aviso>,
    subjects: List<Subject>,
    evaluations: List<EvaluationItem> = emptyList(),
    onNavigateToHorarios: () -> Unit,
    onNavigateToAvisos: () -> Unit,
    onNavigateToBoletim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ptBr = remember { Locale.forLanguageTag("pt-BR") }
    val today = remember { LocalDate.now() }
    val dayOfWeekName = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, ptBr)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBr) else it.toString() }
    }
    val formattedDate = remember(today) {
        "${today.dayOfMonth} de ${today.month.getDisplayName(TextStyle.FULL, ptBr)}"
    }

    // 1. Next class resolution
    val todayClassInfo = remember(scheduleItems, today) {
        val todayDow = today.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val matchKeywords = when (todayDow) {
            1 -> listOf("segunda", "2a", "2ª", "seg")
            2 -> listOf("terça", "terca", "3a", "3ª", "ter")
            3 -> listOf("quarta", "4a", "4ª", "qua")
            4 -> listOf("quinta", "5a", "5ª", "qui")
            5 -> listOf("sexta", "6a", "6ª", "sex")
            6 -> listOf("sábado", "sabado", "sab")
            else -> emptyList()
        }
        val classesToday = scheduleItems.filter { item ->
            val d = item.dayOfWeek.lowercase()
            matchKeywords.any { kw -> d.contains(kw) }
        }
        if (classesToday.isNotEmpty()) {
            val first = classesToday.first()
            "Hoje • ${first.timeSlot}" to "${first.subjectName}${if (first.teacher.isNotBlank()) " (${first.teacher})" else ""}"
        } else if (scheduleItems.isNotEmpty()) {
            val first = scheduleItems.first()
            "${first.dayOfWeek} • ${first.timeSlot}" to "${first.subjectName}${if (first.teacher.isNotBlank()) " (${first.teacher})" else ""}"
        } else {
            "Sem aulas cadastradas" to "Consulte sua grade horária completa"
        }
    }

    // 2. Evaluations: top 2 most recent marked with date (excluding any old mock demo data)
    val recentEvaluations = remember(evaluations) {
        evaluations.filter { it.date.isNotBlank() && !it.id.startsWith("eval_demo_") }.take(2)
    }

    // 3. Newest Notice resolution
    val newestAviso = remember(avisos) {
        avisos.firstOrNull { !it.isRead } ?: avisos.firstOrNull()
    }

    // 4. Latest grade resolution: find the most recently posted evaluation across all subjects
    val latestGradeInfo = remember(subjects) {
        data class GradeEntry(val subjectName: String, val stage: String, val grade: String, val priority: Int)
        val allPostedGrades = mutableListOf<GradeEntry>()

        for (s in subjects) {
            val cleanName = s.name.replace(SUBJECT_CODE_PREFIX_REGEX, "").trim()
            fun isValid(g: String) = g.isNotBlank() && g != "-" && g != "--" && g != "Sem Nota"

            if (isValid(s.recFinal)) allPostedGrades.add(GradeEntry(cleanName, "Final", s.recFinal, 7))
            if (isValid(s.grade4)) allPostedGrades.add(GradeEntry(cleanName, "4º Bim", s.grade4, 6))
            if (isValid(s.rec2)) allPostedGrades.add(GradeEntry(cleanName, "Rec. 2", s.rec2, 5))
            if (isValid(s.grade3)) allPostedGrades.add(GradeEntry(cleanName, "3º Bim", s.grade3, 4))
            if (isValid(s.grade2)) allPostedGrades.add(GradeEntry(cleanName, "2º Bim", s.grade2, 3))
            if (isValid(s.rec1)) allPostedGrades.add(GradeEntry(cleanName, "Rec. 1", s.rec1, 2))
            if (isValid(s.grade1)) allPostedGrades.add(GradeEntry(cleanName, "1º Bim", s.grade1, 1))
        }

        val topGrade = allPostedGrades.maxByOrNull { it.priority }
        if (topGrade != null) {
            Triple(topGrade.subjectName, topGrade.grade, topGrade.stage)
        } else {
            Triple("Notas", "Nenhuma nota recente", "")
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Cabeçalho compacto: "Hoje" + dia e data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Hoje",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$dayOfWeekName, $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Próxima Aula (Linha compacta)
            TodayClassCompactRow(
                headline = todayClassInfo.second,
                subtitle = todayClassInfo.first,
                onClick = onNavigateToHorarios
            )

            // 2. As duas avaliações mais recentes marcadas com data (apenas reais coletadas do SUAP)
            if (recentEvaluations.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentEvaluations.forEach { eval ->
                        TodayEvaluationBadgeCard(
                            evaluation = eval,
                            onClick = onNavigateToBoletim,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (recentEvaluations.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 3. Aviso e Nota lado a lado com datas reais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodayCompactMicroItem(
                    icon = Icons.Default.Notifications,
                    category = "Aviso",
                    headline = newestAviso?.subject ?: "Nenhum aviso novo",
                    date = newestAviso?.date?.takeIf { it.isNotBlank() },
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToAvisos,
                    modifier = Modifier.weight(1f)
                )

                TodayCompactMicroItem(
                    icon = Icons.Default.Grade,
                    category = "Nota",
                    headline = if (latestGradeInfo.third.isNotBlank()) "${latestGradeInfo.first}: ${latestGradeInfo.second}" else latestGradeInfo.second,
                    date = latestGradeInfo.third.takeIf { it.isNotBlank() },
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToBoletim,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Cartão de Avaliação com data destacada em badge
 */
@Composable
private fun TodayEvaluationBadgeCard(
    evaluation: EvaluationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.defaultMinSize(minHeight = 44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge com a data em destaque
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = evaluation.date,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = evaluation.subjectName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = evaluation.title.ifBlank { evaluation.type },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Linha de aula compacta
 */
@Composable
private fun TodayClassCompactRow(
    headline: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Aula",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "Aula",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = headline,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Micro item compacto para Aviso ou Nota com suporte a data/etapa
 */
@Composable
private fun TodayCompactMicroItem(
    icon: ImageVector,
    category: String,
    headline: String,
    date: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.defaultMinSize(minHeight = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(badgeColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!date.isNullOrBlank()) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = headline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Global Search Bar and Results View for Dashboard.
 */
@Composable
fun DashboardSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        shape = MaterialTheme.shapes.large,
        placeholder = {
            Text(
                text = "Buscar disciplinas, avisos e professores...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar busca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun DashboardSearchResults(
    query: String,
    subjects: List<Subject>,
    avisos: List<Aviso>,
    scheduleItems: List<ScheduleItem>,
    professorContacts: List<ProfessorContact>,
    onNavigateToSubject: () -> Unit,
    onNavigateToAviso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanQuery = remember(query) { query.trim().lowercase() }

    // Normalize once when data changes, instead of allocating lowercase strings
    // for every item on each keystroke.
    val searchableSubjects = remember(subjects) { subjects.map { it to it.name.lowercase() } }
    val searchableAvisos = remember(avisos) {
        avisos.map { it to (it.subject.lowercase() to it.sender.lowercase()) }
    }
    val searchableProfessors = remember(professorContacts) {
        professorContacts.map { it to listOf(it.name.lowercase(), it.discipline.lowercase(), it.email.lowercase()) }
    }
    val searchableSchedule = remember(scheduleItems) { scheduleItems.map { it to it.teacher.lowercase() } }

    val matchedSubjects = remember(cleanQuery, searchableSubjects) {
        if (cleanQuery.isBlank()) emptyList()
        else searchableSubjects.filter { (_, normalizedName) -> normalizedName.contains(cleanQuery) }.map { it.first }
    }

    val matchedAvisos = remember(cleanQuery, searchableAvisos) {
        if (cleanQuery.isBlank()) emptyList()
        else searchableAvisos.filter { (_, fields) -> fields.first.contains(cleanQuery) || fields.second.contains(cleanQuery) }.map { it.first }
    }

    val matchedProfessors = remember(cleanQuery, searchableProfessors, searchableSchedule) {
        if (cleanQuery.isBlank()) emptyList()
        else {
            val contactMatches = searchableProfessors.filter { (_, fields) -> fields.any { it.contains(cleanQuery) } }
                .map { (contact, _) -> contact.name to "${contact.discipline} • ${contact.email}" }

            val scheduleMatches = searchableSchedule
                .filter { (item, normalizedTeacher) -> normalizedTeacher.contains(cleanQuery) && item.teacher.isNotBlank() }
                .map { (item, _) -> item.teacher to item.subjectName }
                .distinctBy { it.first }

            (contactMatches + scheduleMatches).distinctBy { it.first }
        }
    }

    val totalMatches = matchedSubjects.size + matchedAvisos.size + matchedProfessors.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Resultados da busca ($totalMatches)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (totalMatches == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Nenhum resultado para \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Matched Disciplines
                if (matchedSubjects.isNotEmpty()) {
                    Text(
                        text = "Disciplinas (${matchedSubjects.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    matchedSubjects.take(4).forEach { subject ->
                        SearchResultItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = subject.name,
                            subtitle = "Frequência: ${subject.absences} faltas • Situação: ${subject.status}",
                            onClick = onNavigateToSubject
                        )
                    }
                }

                // Matched Notices
                if (matchedAvisos.isNotEmpty()) {
                    Text(
                        text = "Avisos (${matchedAvisos.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    matchedAvisos.take(4).forEach { aviso ->
                        SearchResultItem(
                            icon = Icons.Default.Notifications,
                            title = aviso.subject,
                            subtitle = "${aviso.sender} • ${aviso.date}",
                            onClick = onNavigateToAviso
                        )
                    }
                }

                // Matched Professors
                if (matchedProfessors.isNotEmpty()) {
                    Text(
                        text = "Professores (${matchedProfessors.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    matchedProfessors.take(4).forEach { prof ->
                        SearchResultItem(
                            icon = Icons.Default.Person,
                            title = prof.first,
                            subtitle = prof.second,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
