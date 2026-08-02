package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EtapaDetail
import com.example.data.Subject
import java.text.Normalizer
import java.util.Locale

private fun String?.removeAccents(): String {
    if (this.isNullOrBlank()) return ""
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{M}"), "")
}

private fun parseGradeSafe(input: String?): Double? {
    if (input.isNullOrBlank()) return null
    val cleaned = input.trim().replace(",", ".")
    val value = cleaned.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value.isNaN()) return null
    val normalized = if (value > 10.0 && value <= 100.0) value / 10.0 else value
    return normalized.coerceIn(0.0, 10.0)
}

private fun formatGradeDisplay(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletimScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onSubjectClick: ((String, String) -> Unit)? = null
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subjectDetails by viewModel.subjectDetails.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var selectedSubjectForDetails by remember { mutableStateOf<Subject?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredSubjects = remember(searchQuery, selectedFilter, subjects) {
        val safeSubjects = subjects.orEmpty()
        var list = safeSubjects
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.removeAccents()
            list = list.filter { (it.name ?: "").removeAccents().contains(query, ignoreCase = true) }
        }
        val safeFilter = selectedFilter.orEmpty()
        when (safeFilter) {
            "Favoritas" -> list.filter { it.isFavorite }
            "Aprovado" -> list.filter { (it.status ?: "").contains("Aprovado", ignoreCase = true) }
            "Em Andamento" -> list.filter { !(it.status ?: "").contains("Aprovado", ignoreCase = true) && !(it.status ?: "").contains("Reprovado", ignoreCase = true) }
            else -> list
        }.sortedWith(compareByDescending<Subject> { it.isFavorite }.thenBy { (it.name ?: "").lowercase() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Boletim Escolar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${subjects.orEmpty().size} disciplinas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.downloadBoletim(context) }) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontWeight = FontWeight.Bold)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Pesquisar disciplinas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas", "Favoritas", "Aprovado", "Em Andamento").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredSubjects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
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

                itemsIndexed(filteredSubjects, key = { _, subject -> subject.id }) { _, subject ->
                    SubjectBimesterCard(
                        subject = subject,
                        onToggleFavorite = { viewModel.toggleFavorite(subject) },
                        onShowDetails = { subj ->
                            selectedSubjectForDetails = subj
                            viewModel.fetchSubjectDetails(subj)
                        },
                        onSubjectClick = onSubjectClick
                    )
                }
            }
        }
    }

    if (selectedSubjectForDetails != null) {
        val subject = selectedSubjectForDetails!!
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

private data class SubjectComputed(
    val bn1: Double?,
    val bn2: Double?,
    val bn3: Double?,
    val bn4: Double?,
    val totalPoints: Double,
    val calculatedMedia: Double,
    val pointsNeeded: Double
)

@Composable
fun SubjectBimesterCard(
    subject: Subject,
    onToggleFavorite: () -> Unit,
    onShowDetails: (Subject) -> Unit,
    onSubjectClick: ((String, String) -> Unit)? = null
) {
    val computed = remember(subject) {
        var bn1 = parseGradeSafe(subject.grade1)
        var bn2 = parseGradeSafe(subject.grade2)
        var bn3 = parseGradeSafe(subject.grade3)
        var bn4 = parseGradeSafe(subject.grade4)

        val rf = parseGradeSafe(subject.recFinal)
        if (rf != null) {
            val grades = listOf(bn1, bn2, bn3, bn4)
            var minVal = Double.MAX_VALUE
            var minIdx = -1
            for (i in grades.indices) {
                val g = grades[i] ?: 0.0
                if (g < minVal) {
                    minVal = g
                    minIdx = i
                }
            }
            if (minIdx != -1 && rf > minVal) {
                when (minIdx) {
                    0 -> bn1 = rf
                    1 -> bn2 = rf
                    2 -> bn3 = rf
                    3 -> bn4 = rf
                }
            }
        }

        val totalPoints = (bn1 ?: 0.0) + (bn2 ?: 0.0) + (bn3 ?: 0.0) + (bn4 ?: 0.0)
        val validGradesCount = listOfNotNull(bn1, bn2, bn3, bn4).size
        val calculatedMedia = if (validGradesCount > 0) totalPoints / validGradesCount else 0.0
        val pointsNeeded = (24.0 - totalPoints).coerceAtLeast(0.0)

        SubjectComputed(bn1, bn2, bn3, bn4, totalPoints, calculatedMedia, pointsNeeded)
    }

    val bn1 = computed.bn1
    val bn2 = computed.bn2
    val bn3 = computed.bn3
    val bn4 = computed.bn4
    val totalPoints = computed.totalPoints
    val calculatedMedia = computed.calculatedMedia
    val pointsNeeded = computed.pointsNeeded

    val safeStatus = subject.status.orEmpty()
    val isApproved = safeStatus.contains("Aprovado", ignoreCase = true)
    val isReproved = safeStatus.contains("Reprovado", ignoreCase = true)
    val statusColor = when {
        isApproved -> Color(0xFF2E7D32)
        isReproved -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (subject.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favoritar",
                            tint = if (subject.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subject.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    contentColor = statusColor
                ) {
                    Text(
                        text = if (safeStatus.isNotBlank()) safeStatus else "Em curso",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Bimesters Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BimesterItem(
                    modifier = Modifier.weight(1f),
                    bimesterName = "1º Bim",
                    gradeStr = subject.grade1.orEmpty(),
                    effectiveGrade = bn1,
                    classGradeStr = subject.classGrade1.orEmpty()
                )
                BimesterItem(
                    modifier = Modifier.weight(1f),
                    bimesterName = "2º Bim",
                    gradeStr = subject.grade2.orEmpty(),
                    effectiveGrade = bn2,
                    classGradeStr = subject.classGrade2.orEmpty()
                )
                BimesterItem(
                    modifier = Modifier.weight(1f),
                    bimesterName = "3º Bim",
                    gradeStr = subject.grade3.orEmpty(),
                    effectiveGrade = bn3,
                    classGradeStr = subject.classGrade3.orEmpty()
                )
                BimesterItem(
                    modifier = Modifier.weight(1f),
                    bimesterName = "4º Bim",
                    gradeStr = subject.grade4.orEmpty(),
                    effectiveGrade = bn4,
                    classGradeStr = subject.classGrade4.orEmpty()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Summary Info: Points, Media, Absences, Details Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pontos: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${formatGradeDisplay(totalPoints)} / 24.0",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPoints >= 24.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (totalPoints < 24.0) {
                        Text(
                            text = "Faltam ${formatGradeDisplay(pointsNeeded)} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val safeAbsences = subject.absences.orEmpty()
                    if (safeAbsences.isNotBlank() && safeAbsences != "0") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = "$safeAbsences faltas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Média: ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatGradeDisplay(if (calculatedMedia > 0) calculatedMedia else parseGradeSafe(subject.finalGrade)),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    OutlinedIconButton(
                        onClick = { onShowDetails(subject) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Detalhes das Notas",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BimesterItem(
    modifier: Modifier = Modifier,
    bimesterName: String,
    gradeStr: String,
    effectiveGrade: Double?,
    classGradeStr: String
) {
    val origGrade = remember(gradeStr) { parseGradeSafe(gradeStr) }
    val classGrade = remember(classGradeStr) { parseGradeSafe(classGradeStr) }

    val displayGrade = formatGradeDisplay(effectiveGrade ?: origGrade)
    val gradeValue = effectiveGrade ?: origGrade ?: -1.0

    val gradeColor = when {
        gradeValue >= 6.0 -> Color(0xFF2E7D32)
        gradeValue in 0.0..5.99 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isRecovered = origGrade != null && effectiveGrade != null && effectiveGrade > origGrade

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = bimesterName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Grade Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(gradeColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayGrade,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = gradeColor
            )
        }

        if (isRecovered) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Era ${formatGradeDisplay(origGrade)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough
            )
        }

        if (classGrade != null && classGrade > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Turma: ${formatGradeDisplay(classGrade)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Detalhamento de notas por avaliação (SUAP)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))


        if (etapaDetails == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Buscando detalhes do SUAP...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(etapaDetails) { etapa ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = etapa.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            etapa.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.width(44.dp)
                                    ) {
                                        Text(
                                            text = item.sigla.ifBlank { "N/A" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.descricao.isNotBlank() && item.descricao != "-") item.descricao else item.tipo,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (item.tipo.isNotBlank() && item.tipo != item.descricao) {
                                            Text(
                                                text = item.tipo,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val gradeVal = parseGradeSafe(item.nota)
                                    Text(
                                        text = item.nota.ifBlank { "-" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            gradeVal != null && gradeVal >= 6.0 -> Color(0xFF2E7D32)
                                            gradeVal != null -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
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
