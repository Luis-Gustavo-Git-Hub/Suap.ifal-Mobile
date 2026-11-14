package com.example.ui

import com.example.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject

fun parseGradeXp(gradeStr: String?): Float {
    if (gradeStr.isNullOrBlank() || gradeStr == "-") return 0f
    var cleaned = gradeStr.replace(',', '.')
    if (cleaned.contains(" ")) cleaned = cleaned.trim()
    val grade = cleaned.toFloatOrNull() ?: return 0f
    return if (grade <= 10f && grade > 0f) grade * 10f else if (grade > 10f) grade else 0f
}

data class ProfileRankInfo(
    val currentXp: Int,
    val level: Int,
    val rankName: String,
    val rankShortName: String,
    val badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeColor: Color,
    val prevLevelXp: Int,
    val nextLevelXp: Int,
    val xpRemaining: Int,
    val progress: Float
)

fun calculateProfileRank(subjects: List<Subject>): ProfileRankInfo {
    val totalSubjects = subjects.size.coerceAtLeast(1)

    var totalXp = 0f
    for (subject in subjects) {
        val g1 = parseGradeXp(subject.grade1)
        val g2 = parseGradeXp(subject.grade2)
        val g3 = parseGradeXp(subject.grade3)
        val g4 = parseGradeXp(subject.grade4)
        totalXp += g1 + g2 + g3 + g4
    }

    val currentXp = totalXp.toInt()

    val lvl2 = totalSubjects * 140
    val lvl3 = totalSubjects * 240
    val lvl4 = totalSubjects * 320
    val lvl5 = totalSubjects * 360

    val currentLevel = when {
        currentXp >= lvl5 -> 5
        currentXp >= lvl4 -> 4
        currentXp >= lvl3 -> 3
        currentXp >= lvl2 -> 2
        else -> 1
    }

    val nextLvlXp = when (currentLevel) {
        1 -> lvl2
        2 -> lvl3
        3 -> lvl4
        4 -> lvl5
        else -> currentXp
    }

    val prevLvlXp = when (currentLevel) {
        5 -> lvl5
        4 -> lvl4
        3 -> lvl3
        2 -> lvl2
        else -> 0
    }

    val rankName = when (currentLevel) {
        1 -> "🌱 Calouro Curioso"
        2 -> "📖 Aprendiz Dedicado"
        3 -> "🎓 Estudante de Elite"
        4 -> "🧠 Mestre Acadêmico"
        5 -> "👑 Lenda do Campus"
        else -> "🌱 Calouro Curioso"
    }

    val rankShortName = when (currentLevel) {
        1 -> "Calouro"
        2 -> "Aprendiz"
        3 -> "Elite"
        4 -> "Mestre"
        5 -> "Lenda"
        else -> "Calouro"
    }

    val badgeColor = when (currentLevel) {
        1 -> AcademicRankLevel1
        2 -> AcademicRankLevel2
        3 -> AcademicRankLevel3
        4 -> AcademicRankLevel4
        5 -> AcademicRankLevel5
        else -> Color.Gray
    }

    val badgeIcon = when (currentLevel) {
        1 -> Icons.Filled.LocalFlorist
        2 -> Icons.Filled.MenuBook
        3 -> Icons.Filled.School
        4 -> Icons.Filled.Psychology
        5 -> Icons.Filled.WorkspacePremium
        else -> Icons.Filled.Star
    }

    val progress = if (currentLevel == 5) 1f else {
        val range = (nextLvlXp - prevLvlXp).toFloat()
        val current = (currentXp - prevLvlXp).toFloat()
        if (range > 0) (current / range).coerceIn(0f, 1f) else 0f
    }

    val xpRemaining = if (currentLevel < 5) (nextLvlXp - currentXp).coerceAtLeast(0) else 0

    return ProfileRankInfo(
        currentXp = currentXp,
        level = currentLevel,
        rankName = rankName,
        rankShortName = rankShortName,
        badgeIcon = badgeIcon,
        badgeColor = badgeColor,
        prevLevelXp = prevLvlXp,
        nextLevelXp = nextLvlXp,
        xpRemaining = xpRemaining,
        progress = progress
    )
}

@Composable
fun XpSystemCard(
    subjects: List<Subject>,
    modifier: Modifier = Modifier
) {
    val totalSubjects = subjects.size.coerceAtLeast(1)
    var showRankDialog by remember { mutableStateOf(false) }

    val rankInfo = remember(subjects) { calculateProfileRank(subjects) }

    val lvl2 = totalSubjects * 140
    val lvl3 = totalSubjects * 240
    val lvl4 = totalSubjects * 320
    val lvl5 = totalSubjects * 360

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header Row: Badge Icon + Name & Level + Info button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(rankInfo.badgeColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rankInfo.badgeIcon,
                            contentDescription = "Emblema Atual",
                            tint = rankInfo.badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = rankInfo.rankName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = rankInfo.badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Nv. ${rankInfo.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rankInfo.badgeColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (rankInfo.level < 5) "Faltam ${rankInfo.xpRemaining} XP para Nv. ${rankInfo.level + 1}" else "Nível Máximo Alcançado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                IconButton(
                    onClick = { showRankDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Informações sobre ranks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { rankInfo.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = rankInfo.badgeColor,
                trackColor = rankInfo.badgeColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${rankInfo.currentXp} XP acumulados",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankInfo.badgeColor
                )
                Text(
                    text = "${(rankInfo.progress * 100).toInt()}% do nível",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showRankDialog) {
        AlertDialog(
            onDismissRequest = { showRankDialog = false },
            title = {
                Text("Níveis de Rank Acadêmico", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RankInfoRow(1, "🌱 Calouro Curioso", 0, lvl2 - 1, "< 3.5", AcademicRankLevel1, Icons.Filled.LocalFlorist)
                    RankInfoRow(2, "📖 Aprendiz Dedicado", lvl2, lvl3 - 1, "3.5 a 5.9", AcademicRankLevel2, Icons.Filled.MenuBook)
                    RankInfoRow(3, "🎓 Estudante de Elite", lvl3, lvl4 - 1, "6.0 a 7.9", AcademicRankLevel3, Icons.Filled.School)
                    RankInfoRow(4, "🧠 Mestre Acadêmico", lvl4, lvl5 - 1, "8.0 a 8.9", AcademicRankLevel4, Icons.Filled.Psychology)
                    RankInfoRow(5, "👑 Lenda do Campus", lvl5, totalSubjects * 400, ">= 9.0", AcademicRankLevel5, Icons.Filled.WorkspacePremium)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Valores calculados com base em $totalSubjects matérias. Máximo: ${totalSubjects * 400} XP.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRankDialog = false }) {
                    Text("Fechar")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun RankInfoRow(
    level: Int,
    name: String,
    minXp: Int,
    maxXp: Int,
    avgNeeded: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (level == 5) "$minXp+ XP • Média $avgNeeded" else "$minXp a $maxXp XP • Média $avgNeeded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
