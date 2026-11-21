package com.example.ui

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Subject
import com.example.domain.AcademicCalculator
import com.example.domain.AcademicStatus
import com.example.domain.StatisticsCalculator
import com.example.domain.StatisticsSnapshot
import com.example.domain.SubjectStatistics
import com.example.ui.theme.AppSemanticColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val statisticTabs = listOf(
    "Resumo" to Icons.Outlined.Insights,
    "Notas" to Icons.Outlined.BarChart,
    "Frequência" to Icons.Outlined.CalendarMonth,
    "Comparar" to Icons.Outlined.CompareArrows,
    "Simulador" to Icons.Outlined.ShowChart
)

@Composable
fun StatisticsScreen(viewModel: SuapViewModel) {
    val snapshot by viewModel.statisticsSnapshot.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedStage by rememberSaveable { mutableIntStateOf(1) }
    var minimumAttendance by rememberSaveable { mutableFloatStateOf(StatisticsCalculator.DEFAULT_MINIMUM_ATTENDANCE.toFloat()) }
    var targetAverage by rememberSaveable { mutableFloatStateOf(AcademicCalculator.PASSING_AVERAGE.toFloat()) }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        com.example.ui.components.AppPageHeader(
            title = "Estatísticas",
            subtitle = "Seu desempenho acadêmico",
            modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing12)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing4),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            statisticTabs.forEachIndexed { index, (title, icon) ->
                val selected = selectedTab == index
                Surface(
                    onClick = { selectedTab = index },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
        when (selectedTab) {
            0 -> SummaryTab(snapshot)
            1 -> NotesTab(snapshot, selectedStage, { selectedStage = it })
            2 -> FrequencyTab(snapshot, minimumAttendance, { minimumAttendance = it })
            3 -> CompareTab(snapshot, selectedStage, { selectedStage = it })
            else -> SimulatorTab(snapshot, targetAverage, { targetAverage = it })
        }
    }
}

@Composable
private fun SummaryTab(snapshot: StatisticsSnapshot) {
    StatisticsList {
        item { PageHeading(Icons.Outlined.Insights, "Seu resumo acadêmico", "Notas e dados salvos no dispositivo") }
        if (!snapshot.hasData) {
            item { EmptyState("Sincronize para ver suas estatísticas.") }
        } else {
            item {
                MetricGrid(
                    listOf(
                        "Média geral" to gradeText(snapshot.overallAverage),
                        "Média da turma" to gradeText(snapshot.classAverage),
                        "Frequência" to percentText(snapshot.globalFrequency),
                        "Disciplinas acima da turma" to percentText(snapshot.aboveClassPercent)
                    )
                )
            }
            item {
                StatusSummary(snapshot)
            }
            item { SectionHeading("Evolução das médias", "Média das notas disponíveis em cada etapa") }
            item { StageChart(snapshot) }
            item {
                InformationCard(
                    title = "Progresso acadêmico",
                    body = "${snapshot.totalXp} XP · nível ${snapshot.level}. IRA não está disponível nos dados sincronizados."
                )
            }
        }
    }
}

@Composable
private fun NotesTab(snapshot: StatisticsSnapshot, selectedStage: Int, onStageSelected: (Int) -> Unit) {
    val rows = snapshot.subjects.filter { it.grades.any { grade -> grade != null } }
    StatisticsList {
        item { PageHeading(Icons.Outlined.BarChart, "Notas", "Evolução por etapa e disciplina") }
        if (!snapshot.hasData) {
            item { EmptyState("Sincronize para ver a evolução das notas.") }
        } else {
            item { StagePicker(selectedStage, onStageSelected) }
            item {
                val stage = snapshot.stages.getOrNull(selectedStage - 1)
                MetricGrid(
                    listOf(
                        "Média nesta etapa" to gradeText(stage?.studentAverage),
                        "Média da turma" to gradeText(stage?.classAverage)
                    )
                )
            }
            item { StageChart(snapshot) }
            if (rows.isEmpty()) {
                item { EmptyState("Ainda não há notas por disciplina para esta etapa.") }
            } else {
                item { SectionHeading("Notas por disciplina", "A recuperação segue a regra do boletim") }
                items(rows, key = { it.subject.id.ifBlank { it.subject.name } }) { row ->
                    NoteSubjectCard(row, selectedStage)
                }
                item { DistributionCard(rows) }
                item {
                    val highest = rows.maxByOrNull { it.average ?: -1.0 }
                    val lowest = rows.filter { it.average != null }.minByOrNull { it.average!! }
                    InformationCard(
                        title = "Destaques",
                        body = "Melhor média: ${highest?.subject?.name ?: "—"} (${gradeText(highest?.average)}). Menor média: ${lowest?.subject?.name ?: "—"} (${gradeText(lowest?.average)})."
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyTab(snapshot: StatisticsSnapshot, minimumAttendance: Float, onMinimumChange: (Float) -> Unit) {
    val rows = snapshot.subjects.filter { it.absences > 0 || it.subject.absences.isNotBlank() }
        .sortedByDescending { it.absences }
    StatisticsList {
        item { PageHeading(Icons.Outlined.CalendarMonth, "Frequência", "Faltas registradas no SUAP") }
        if (!snapshot.hasData) {
            item { EmptyState("Sincronize para ver suas faltas e frequência.") }
        } else {
            item {
                MetricGrid(
                    listOf(
                        "Frequência global" to percentText(snapshot.globalFrequency),
                        "Faltas globais" to (snapshot.globalAbsences?.toString() ?: "—"),
                        "Aulas dadas" to (snapshot.passedClasses?.toString() ?: "—")
                    )
                )
            }
            item {
                CardPanel {
                    Text("Referência mínima de frequência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Ajuste conforme o regulamento do IFAL. O valor inicial de 75% é uma proposta, não uma regra confirmada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${minimumAttendance.roundToInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Slider(value = minimumAttendance, onValueChange = onMinimumChange, valueRange = 50f..100f, steps = 9)
                    snapshot.globalFrequency?.let { frequency ->
                        val enough = frequency >= minimumAttendance
                        Text(
                            if (enough) "Acima da referência configurada" else "Abaixo da referência configurada",
                            color = if (enough) AppSemanticColors.success else AppSemanticColors.danger,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val total = snapshot.totalClasses
                    val absences = snapshot.globalAbsences
                    if (total != null && absences != null) {
                        val maxAbsences = (total * (1.0 - minimumAttendance / 100.0)).toInt()
                        val remaining = (maxAbsences - absences).coerceAtLeast(0)
                        Text(
                            "Estimativa: até $remaining falta(s) adicional(is) pelo limite configurado, considerando a carga total salva no SUAP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { SectionHeading("Faltas por disciplina", "A quantidade está salva; o SUAP não fornece o total de aulas de cada matéria") }
            if (rows.isEmpty()) item { EmptyState("Não há dados de faltas por disciplina.") }
            else items(rows, key = { it.subject.id.ifBlank { it.subject.name } }) { AbsenceCard(it) }
            item {
                InformationCard(
                    title = "Histórico diário indisponível",
                    body = "O app ainda não recebe datas de falta do SUAP. Por isso, não exibe mapa de calor nem distribuição por dia da semana."
                )
            }
        }
    }
}

@Composable
private fun CompareTab(snapshot: StatisticsSnapshot, selectedStage: Int, onStageSelected: (Int) -> Unit) {
    val rows = snapshot.subjects.filter { it.grades.getOrNull(selectedStage - 1) != null }
    val stage = snapshot.stages.getOrNull(selectedStage - 1)
    val stageComparisons = snapshot.subjects.mapNotNull { row ->
        val student = row.grades.getOrNull(selectedStage - 1)
        val classroom = row.classGrades.getOrNull(selectedStage - 1)
        if (student != null && classroom != null) student to classroom else null
    }
    val aboveClassPercent = stageComparisons.takeIf { it.isNotEmpty() }
        ?.let { pairs -> pairs.count { it.first > it.second }.toDouble() * 100.0 / pairs.size }
    StatisticsList {
        item { PageHeading(Icons.Outlined.CompareArrows, "Comparar", "Você e a média da turma") }
        if (!snapshot.hasData) {
            item { EmptyState("Sincronize suas notas para comparar com a turma.") }
        } else {
            item { StagePicker(selectedStage, onStageSelected) }
            item {
                InformationCard(
                    title = "Etapa ${selectedStage}ª",
                    body = "Sua média: ${gradeText(stage?.studentAverage)} · média da turma: ${gradeText(stage?.classAverage)} · ${percentText(aboveClassPercent)} das comparações disponíveis acima da turma."
                )
            }
            val previous = snapshot.stages.getOrNull(selectedStage - 2)
            if (previous != null) {
                val currentAverage = stage?.studentAverage
                val previousAverage = previous.studentAverage
                val change = if (currentAverage != null && previousAverage != null) currentAverage - previousAverage else null
                item {
                    InformationCard(
                        title = "Etapa atual x anterior",
                        body = if (change == null) "Ainda não há notas suficientes nas duas etapas para comparar."
                        else "Variação da sua média: ${signedText(change)} (${gradeText(previousAverage)} → ${gradeText(currentAverage)})."
                    )
                }
            }
            item { SectionHeading("Por disciplina", "Valores ausentes não entram na comparação") }
            if (rows.isEmpty()) item { EmptyState("Ainda não há notas nesta etapa.") }
            else items(rows, key = { it.subject.id.ifBlank { it.subject.name } }) { CompareSubjectCard(it, selectedStage) }
        }
    }
}

@Composable
private fun SimulatorTab(snapshot: StatisticsSnapshot, targetAverage: Float, onTargetChange: (Float) -> Unit) {
    val rows = snapshot.subjects.filter { it.neededAverage != null }
        .sortedBy { it.neededAverage }
    StatisticsList {
        item { PageHeading(Icons.Outlined.ShowChart, "Simulador", "Cenários calculados pela regra acadêmica existente") }
        if (!snapshot.hasData) {
            item { EmptyState("Sincronize para simular suas médias.") }
        } else {
            item {
                CardPanel {
                    Text("Média final desejada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(gradeText(targetAverage.toDouble()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Slider(value = targetAverage, onValueChange = onTargetChange, valueRange = 5f..10f, steps = 9)
                    Text("O cálculo considera os pesos do AcademicCalculator e as notas ainda não lançadas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (rows.isEmpty()) item { EmptyState("Não há disciplinas com etapas pendentes para simular.") }
            else {
                item { SectionHeading("Quanto falta", "Média necessária nas etapas restantes") }
                items(rows, key = { it.subject.id.ifBlank { it.subject.name } }) { row ->
                    SimulatorSubjectCard(row, targetAverage.toDouble())
                }
            }
        }
    }
}

@Composable
private fun StatisticsList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
        content = content
    )
}

@Composable
private fun PageHeading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Box(Modifier.size(44.dp).clip(androidx.compose.material3.MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StagePicker(selectedStage: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        (1..4).forEach { stage ->
            com.example.ui.components.AppFilterChip(selected = selectedStage == stage, onClick = { onSelect(stage) }, label = { Text("${stage}ª etapa") })
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        metrics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                row.forEach { (title, value) ->
                    CardPanel(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusSummary(snapshot: StatisticsSnapshot) {
    CardPanel {
        Text("Situação das disciplinas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
            StatusPill("Aprovadas", snapshot.approvedCount, AppSemanticColors.success, Modifier.weight(1f))
            StatusPill("Em curso", snapshot.ongoingCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
            StatusPill("Em risco", snapshot.atRiskCount, AppSemanticColors.danger, Modifier.weight(1f))
            StatusPill("Recuperação", snapshot.recoveryCount, AppSemanticColors.warning, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusPill(label: String, count: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = androidx.compose.material3.MaterialTheme.shapes.large, color = accent.copy(alpha = 0.12f)) {
        Column(Modifier.padding(AppDimens.Spacing8)) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun StageChart(snapshot: StatisticsSnapshot) {
    val values = snapshot.stages.map { it.studentAverage }
    val color = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    CardPanel {
        Text("Notas por etapa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Canvas(
            modifier = Modifier.fillMaxWidth().height(150.dp)
                .semantics { contentDescription = "Gráfico de médias por etapa: ${snapshot.stages.joinToString { "etapa ${it.stage}: ${gradeText(it.studentAverage)}" }}" }
        ) {
            val left = 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 24.dp.toPx()
            val usableHeight = bottom - top
            val xStep = (size.width - left * 2) / 3f
            val points = values.mapIndexedNotNull { index, value ->
                value?.let { androidx.compose.ui.geometry.Offset(left + xStep * index, bottom - usableHeight * (it.coerceIn(0.0, 10.0) / 10.0).toFloat()) }
            }
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(size.width - left, bottom), strokeWidth = 1.dp.toPx())
            for (grade in 0..10 step 5) {
                val y = bottom - usableHeight * grade / 10f
                drawLine(gridColor.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(size.width - left, y), strokeWidth = 1.dp.toPx())
            }
            if (points.size > 1) {
                val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
            }
            points.forEach { drawCircle(color, 5.dp.toPx(), it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..4).forEach { Text("${it}ª", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun NoteSubjectCard(row: SubjectStatistics, selectedStage: Int) {
    val grade = row.grades[selectedStage - 1]
    val previous = if (selectedStage > 1) row.grades[selectedStage - 2] else null
    val change = if (grade != null && previous != null) grade - previous else null
    CardPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text(row.subject.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(gradeText(grade), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text("Média calculada: ${gradeText(row.average)} · ${statusLabel(row.status)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            row.grades.forEachIndexed { index, value ->
                StageGradeChip(index + 1, value, Modifier.weight(1f))
            }
        }
        if (change != null) Text("Variação desde a etapa anterior: ${signedText(change)}", style = MaterialTheme.typography.labelMedium, color = if (change >= 0) AppSemanticColors.success else AppSemanticColors.danger)
    }
}

@Composable
private fun StageGradeChip(stage: Int, grade: Double?, modifier: Modifier = Modifier) {
    Surface(modifier, shape = androidx.compose.material3.MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(vertical = AppDimens.Spacing8, horizontal = AppDimens.Spacing4), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("B$stage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(gradeText(grade), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DistributionCard(rows: List<SubjectStatistics>) {
    val grades = rows.flatMap { it.grades.filterNotNull() }
    CardPanel {
        Text("Distribuição das notas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        listOf("9–10" to grades.count { it >= 9 }, "7–8,9" to grades.count { it >= 7 && it < 9 }, "5–6,9" to grades.count { it >= 5 && it < 7 }, "0–4,9" to grades.count { it < 5 }).forEach { (range, count) ->
            Text("$range: $count", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AbsenceCard(row: SubjectStatistics) {
    CardPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Frequência por disciplina não foi fornecida pelo SUAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${row.absences} faltas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (row.absences > 0) AppSemanticColors.warning else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CompareSubjectCard(row: SubjectStatistics, stage: Int) {
    val student = row.grades[stage - 1]
    val classroom = row.classGrades[stage - 1]
    val difference = if (student != null && classroom != null) student - classroom else null
    CardPanel {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Text(row.subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8), verticalAlignment = Alignment.CenterVertically) {
                Text("Você ${gradeText(student)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Turma ${gradeText(classroom)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Text(
            difference?.let { if (it == 0.0) "Na média da turma" else "${if (it > 0) "Acima" else "Abaixo"} da turma em ${gradeText(abs(it))} ponto(s)" }
                ?: "Média da turma indisponível",
            style = MaterialTheme.typography.bodySmall,
            color = difference?.let { if (it >= 0) AppSemanticColors.success else AppSemanticColors.danger } ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimulatorSubjectCard(row: SubjectStatistics, target: Double) {
    val needed = remember(row, target) { StatisticsCalculator.requiredAverage(row.subject, target) }
    CardPanel {
        Text(row.subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            needed?.let { if (it > 10) "Meta acima da escala: revise as notas lançadas." else "Precisa de média ${gradeText(it)} nas etapas restantes." }
                ?: "Todas as etapas já têm nota; média atual ${gradeText(row.average)}.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            ForecastPill("Pior cenário", gradeText(row.worstCaseAverage), AppSemanticColors.danger, Modifier.weight(1f))
            ForecastPill("Melhor cenário", gradeText(row.bestCaseAverage), AppSemanticColors.success, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ForecastPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = androidx.compose.material3.MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f)) {
        Column(Modifier.padding(AppDimens.Spacing8)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CardPanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    com.example.ui.components.AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.ItemSpacing), content = content)
    }
}

@Composable
private fun InformationCard(title: String, body: String) {
    CardPanel {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(message: String) {
    com.example.ui.components.AppEmptyState(message)

}

private fun statusLabel(status: AcademicStatus): String = when (status) {
    AcademicStatus.APPROVED -> "Aprovada"
    AcademicStatus.ONGOING -> "Em andamento"
    AcademicStatus.AT_RISK -> "Em risco"
    AcademicStatus.RECOVERY -> "Em recuperação"
    AcademicStatus.OTHER -> "Sem situação"
}

private fun gradeText(value: Double?): String = value?.let { String.format(Locale("pt", "BR"), "%.1f", it) } ?: "—"
private fun percentText(value: Double?): String = value?.let { "${it.roundToInt()}%" } ?: "—"
private fun signedText(value: Double): String = (if (value > 0) "+" else "") + gradeText(value)
