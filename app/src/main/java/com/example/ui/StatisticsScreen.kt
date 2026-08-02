package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs

import com.example.data.Subject

@Composable
fun StatisticsScreen(viewModel: SuapViewModel) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    
    val scrollState = rememberScrollState()
    val studentAvgStr by viewModel.studentAverage.collectAsStateWithLifecycle()
    val classAvgStr by viewModel.classAverage.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Estatísticas Acadêmicas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (subjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sem dados suficientes para estatísticas.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            val validSubjects = subjects.filter { parseGrade(it.finalGrade) != null || parseGrade(it.grade1) != null }
            
            // Compact General Stats Grid
            GeneralStatsGrid(subjects, validSubjects, studentAvgStr)

            if (validSubjects.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Média por Bimestre
                    Box(modifier = Modifier.weight(1f)) {
                        BimesterPerformanceCompact(subjects)
                    }
                    // Distribuição de Notas
                    Box(modifier = Modifier.weight(1f)) {
                        GradeDistributionCard(validSubjects)
                    }
                }
                
                ComparisonChartCard(validSubjects)

                // Melhores e Piores em formato de lista compacta
                ExtremesCard(validSubjects)
            }

            // Alertas (Faltas e Recuperação) em um único card compacto
            AlertsCard(subjects)

            // Fallback for text-only grades
            if (validSubjects.isEmpty() && subjects.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notas Detalhadas (Texto)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        subjects.forEach { s ->
                            Text("${s.name}: ${s.finalGrade.ifEmpty { s.grade1.ifEmpty { "Sem nota" } }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun GeneralStatsGrid(subjects: List<Subject>, validSubjects: List<Subject>, studentAvgStr: String) {
    val totalAbsences = subjects.sumOf { it.absences.toIntOrNull() ?: 0 }
    val allGrades = validSubjects.mapNotNull { getHighestGrade(it) }
    val calculatedAverage = if (allGrades.isNotEmpty()) allGrades.average() else 0.0
    val approvalRate = if (allGrades.isNotEmpty()) {
        (allGrades.count { it >= 6.0 }.toDouble() / allGrades.size) * 100
    } else 0.0

    val finalStudentAvg = if (studentAvgStr != "-" && studentAvgStr.isNotEmpty()) studentAvgStr else String.format("%.1f", calculatedAverage)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStatCard("Média do Aluno", finalStudentAvg, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            MiniStatCard("Faltas Totais", totalAbsences.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStatCard("Disciplinas", subjects.size.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            MiniStatCard("Aproveitamento", "${approvalRate.toInt()}%", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BimesterPerformanceCompact(subjects: List<Subject>) {
    val b1 = subjects.mapNotNull { parseGrade(it.grade1) }.average().takeIf { !it.isNaN() } ?: 0.0
    val b2 = subjects.mapNotNull { parseGrade(it.grade2) }.average().takeIf { !it.isNaN() } ?: 0.0
    val b3 = subjects.mapNotNull { parseGrade(it.grade3) }.average().takeIf { !it.isNaN() } ?: 0.0
    val b4 = subjects.mapNotNull { parseGrade(it.grade4) }.average().takeIf { !it.isNaN() } ?: 0.0
    val maxVal = maxOf(b1, b2, b3, b4, 10.0)

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Por Bimestre", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                CompactBarChartItem("1º", b1, maxVal)
                CompactBarChartItem("2º", b2, maxVal)
                CompactBarChartItem("3º", b3, maxVal)
                CompactBarChartItem("4º", b4, maxVal)
            }
        }
    }
}

@Composable
fun CompactBarChartItem(label: String, value: Double, maxVal: Double) {
    val heightFraction = if (maxVal > 0) (value / maxVal).toFloat() else 0f
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
        Text(if (value > 0) String.format("%.1f", value) else "-", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(heightFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun GradeDistributionCard(subjects: List<Subject>) {
    val grades = subjects.mapNotNull { getHighestGrade(it) }
    val excellent = grades.count { it >= 9.0 }
    val good = grades.count { it >= 7.0 && it < 9.0 }
    val average = grades.count { it >= 5.0 && it < 7.0 }
    val poor = grades.count { it < 5.0 }
    val total = grades.size.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Distribuição", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistBar("9-10", excellent, total, Color(0xFF4CAF50))
                DistBar("7-8.9", good, total, Color(0xFF8BC34A))
                DistBar("5-6.9", average, total, Color(0xFFFFC107))
                DistBar("< 5", poor, total, Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun DistBar(label: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), modifier = Modifier.width(36.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Text(count.toString(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), modifier = Modifier.width(16.dp).padding(start = 4.dp))
    }
}

@Composable
fun ExtremesCard(subjects: List<Subject>) {
    val sortedSubjects = subjects.sortedByDescending { getHighestGrade(it) ?: 0.0 }
    val best = sortedSubjects.take(2)
    val worst = sortedSubjects.takeLast(2).reversed() // Pior primeiro

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Destaques", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.height(8.dp))
                best.forEach { s ->
                    val g = getHighestGrade(s) ?: 0.0
                    Text(s.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(String.format("%.1f", g), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atenção", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                worst.forEach { s ->
                    val g = getHighestGrade(s) ?: 0.0
                    Text(s.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(String.format("%.1f", g), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun AlertsCard(subjects: List<Subject>) {
    val worstAbsences = subjects.filter { (it.absences.toIntOrNull() ?: 0) > 0 }
        .sortedByDescending { it.absences.toIntOrNull() ?: 0 }
        .take(2)

    fun getMA(s: Subject): Double? {
        val grades = listOfNotNull(parseGrade(s.grade1), parseGrade(s.grade2), parseGrade(s.grade3), parseGrade(s.grade4))
        return if (grades.isNotEmpty()) grades.average() else null
    }

    val atRiskSubjects = subjects.mapNotNull { s ->
        val rawMa = getMA(s)
        if (rawMa != null) {
            val ma = if (rawMa > 10.0) rawMa / 10.0 else rawMa
            if (ma < 6.0) Pair(s, ma) else null
        } else null
    }.take(2)

    if (worstAbsences.isNotEmpty() || atRiskSubjects.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Alertas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (worstAbsences.isNotEmpty()) {
                    Text("Faltas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    worstAbsences.forEach { s ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${s.absences} faltas", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (atRiskSubjects.isNotEmpty()) {
                    Text("Risco de Recuperação (MA < 6.0)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    atRiskSubjects.forEach { (s, ma) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(String.format("MA: %.1f", ma), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}









@Composable
fun ComparisonChartCard(subjects: List<Subject>) {
    var selectedEtapa by remember { androidx.compose.runtime.mutableStateOf(1) } // 1 to 4
    var showClassAverage by remember { androidx.compose.runtime.mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showClassAverage = !showClassAverage },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gráfico de Desempenho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // Dropdown for Etapa
                var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                        Text("Etapa: $selectedEtapa", style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (1..4).forEach { etapa ->
                            DropdownMenuItem(text = { Text("Etapa $etapa") }, onClick = { selectedEtapa = etapa; expanded = false })
                        }
                    }
                }
            }
            
            Text(
                "Toque no gráfico para ${if (showClassAverage) "ocultar" else "mostrar"} a média da turma", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nota do Aluno", style = MaterialTheme.typography.labelSmall)
                if (showClassAverage) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Média da Turma", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val validSubjects = subjects.filter { 
                val grade = when(selectedEtapa) {
                    1 -> it.grade1
                    2 -> it.grade2
                    3 -> it.grade3
                    4 -> it.grade4
                    else -> ""
                }
                parseGrade(grade) != null
            }
            
            if (validSubjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Sem notas para esta etapa.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val maxHeight = if (showClassAverage) 340.dp else 250.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    validSubjects.forEach { subject ->
                        val gradeStr = when(selectedEtapa) {
                            1 -> subject.grade1
                            2 -> subject.grade2
                            3 -> subject.grade3
                            4 -> subject.grade4
                            else -> ""
                        }
                        val grade = parseGrade(gradeStr) ?: 0.0
                        
                        val classGradeStr = when(selectedEtapa) {
                            1 -> subject.classGrade1
                            2 -> subject.classGrade2
                            3 -> subject.classGrade3
                            4 -> subject.classGrade4
                            else -> ""
                        }
                        val classAvg = classGradeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        
                        SubjectComparisonHorizontalBars(
                            subjectName = subject.name.split(" - ").last().trim(),
                            studentGrade = grade,
                            classGrade = classAvg,
                            showClassAverage = showClassAverage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectComparisonHorizontalBars(subjectName: String, studentGrade: Double, classGrade: Double, showClassAverage: Boolean) {
    val maxGrade = 10.0
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            subjectName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((studentGrade / maxGrade).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(String.format(java.util.Locale.US, "%.1f", studentGrade), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        
        if (showClassAverage) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((classGrade / maxGrade).coerceIn(0.0, 1.0).toFloat())
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(String.format(java.util.Locale.US, "%.1f", classGrade), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}
fun parseGrade(gradeStr: String): Double? {
    return gradeStr.replace(",", ".").toDoubleOrNull()
}

fun getHighestGrade(subject: Subject): Double? {
    val gFinal = parseGrade(subject.finalGrade)
    if (gFinal != null) return gFinal
    val grades = listOfNotNull(
        parseGrade(subject.grade1),
        parseGrade(subject.grade2),
        parseGrade(subject.grade3),
        parseGrade(subject.grade4)
    )
    if (grades.isNotEmpty()) return grades.average()
    return null
}
