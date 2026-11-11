package com.example.ui

import com.example.ui.theme.*

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.Subject
import com.example.domain.AcademicCalculator
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_M_REGEX = Regex("\\p{M}")
private val WHITESPACE_REGEX = Regex("\\s+")

fun String?.removeAccents(): String {
    if (this.isNullOrBlank()) return ""
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return DIACRITICS_M_REGEX.replace(normalized, "")
}

@Stable
data class BimesterUiInfo(
    val bimesterName: String,
    val displayGrade: String,
    val isRecovered: Boolean,
    val origGradeDisplay: String,
    val classGradeDisplay: String?,
    val isPassing: Boolean,
    val hasGrade: Boolean,
    val gradeBgDark: Color,
    val gradeBgLight: Color,
    val gradeTextColorDark: Color,
    val gradeTextColorLight: Color,
    val borderStrokeColorDark: Color,
    val borderStrokeColorLight: Color
)

@Stable
data class SubjectUiModel(
    val subject: Subject,
    val normalizedName: String,
    val cleanName: String,
    val bimesters: List<BimesterUiInfo>,
    val totalPoints: Double,
    val totalPointsDisplay: String,
    val calculatedMedia: Double,
    val mediaDisplay: String,
    val pointsNeeded: Double,
    val pointsNeededDisplay: String,
    val isGoalReached: Boolean,
    val xp: Int,
    val subjectLevel: Int,
    val badgeColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val safeStatus: String,
    val isApproved: Boolean,
    val isReproved: Boolean,
    val isRecovery: Boolean = false,
    val isOngoing: Boolean = false,
    val isFavorite: Boolean,
    val pointsRatio: Float,
    val subjectIcon: ImageVector,
    val statusText: String
)

private fun parseGradeSafe(input: String?): Double? = AcademicCalculator.parseGrade(input)
private fun formatGradeDisplay(value: Double?): String = AcademicCalculator.formatGrade(value).replace('.', ',')

private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)

fun buildSubjectUiModel(subject: Subject): SubjectUiModel {
    val cleanName = subject.name.orEmpty().trim().replace(WHITESPACE_REGEX, " ")
    val normalizedName = cleanName.removeAccents().lowercase(Locale.ROOT)

    val origBn1 = parseGradeSafe(subject.grade1)
    val origBn2 = parseGradeSafe(subject.grade2)
    val origBn3 = parseGradeSafe(subject.grade3)
    val origBn4 = parseGradeSafe(subject.grade4)
    val rf = parseGradeSafe(subject.recFinal)

    val effectiveGrades = AcademicCalculator.applyRecoveryGrades(origBn1, origBn2, origBn3, origBn4, rf)
    val bn1 = effectiveGrades[0]
    val bn2 = effectiveGrades[1]
    val bn3 = effectiveGrades[2]
    val bn4 = effectiveGrades[3]

    fun makeBimester(name: String, orig: Double?, eff: Double?, classGradeStr: String?): BimesterUiInfo {
        val classGrade = parseGradeSafe(classGradeStr)
        val effGrade = eff ?: orig
        val isRec = orig != null && eff != null && eff > orig
        val isPass = (effGrade ?: -1.0) >= 6.0
        val hasG = effGrade != null && effGrade >= 0.0

        val (gBgDark, gBgLight, gTxtDark, gTxtLight, bStrDark, bStrLight) = when {
            isPass -> Tuple6(
                DarkSemanticColors.successContainer.copy(alpha = 0.55f), LightSemanticColors.successContainer.copy(alpha = 0.8f),
                DarkSemanticColors.success, LightSemanticColors.success,
                DarkSemanticColors.success.copy(alpha = 0.40f), LightSemanticColors.success.copy(alpha = 0.30f)
            )
            hasG -> Tuple6(
                DarkSemanticColors.dangerContainer.copy(alpha = 0.55f), LightSemanticColors.dangerContainer.copy(alpha = 0.8f),
                DarkSemanticColors.danger, LightSemanticColors.danger,
                DarkSemanticColors.danger.copy(alpha = 0.40f), LightSemanticColors.danger.copy(alpha = 0.30f)
            )
            else -> Tuple6(
                Color.White.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.53f),
                Color.Transparent, Color.Transparent
            )
        }

        return BimesterUiInfo(
            bimesterName = name,
            displayGrade = formatGradeDisplay(effGrade),
            isRecovered = isRec,
            origGradeDisplay = formatGradeDisplay(orig),
            classGradeDisplay = if (classGrade != null && classGrade > 0.0) formatGradeDisplay(classGrade) else null,
            isPassing = isPass,
            hasGrade = hasG,
            gradeBgDark = gBgDark,
            gradeBgLight = gBgLight,
            gradeTextColorDark = gTxtDark,
            gradeTextColorLight = gTxtLight,
            borderStrokeColorDark = bStrDark,
            borderStrokeColorLight = bStrLight
        )
    }

    val bimesters = listOf(
        makeBimester("1º", origBn1, bn1, subject.classGrade1),
        makeBimester("2º", origBn2, bn2, subject.classGrade2),
        makeBimester("3º", origBn3, bn3, subject.classGrade3),
        makeBimester("4º", origBn4, bn4, subject.classGrade4)
    )

    val totalPoints = (bn1 ?: 0.0) + (bn2 ?: 0.0) + (bn3 ?: 0.0) + (bn4 ?: 0.0)
    val validGradesCount = listOfNotNull(bn1, bn2, bn3, bn4).size
    val isSemestral = subject.status.contains("Consolidado", ignoreCase = true) || 
                      subject.status.contains("Aprovado", ignoreCase = true) || 
                      subject.status.contains("Reprovado", ignoreCase = true) || 
                      (bn3 == null && bn4 == null && validGradesCount == 2 && !subject.status.contains("Cursando", ignoreCase = true))

    // No boletim, a média anual corresponde à soma das quatro notas bimestrais
    // (já considerando recuperação) dividida pelos quatro bimestres.
    val calculatedMedia = totalPoints / 4.0
    val mediaVal = calculatedMedia

    val pointsNeeded = AcademicCalculator.calculatePointsNeeded(bn1, bn2, bn3, bn4, isSemestral, 6.0)
    val xpVal = AcademicCalculator.calculateSubjectXp(bn1, bn2, bn3, bn4)
    val lvl = AcademicCalculator.calculateLevel(xpVal)

    val bColor = when (lvl) {
        1 -> AcademicRankLevel1
        2 -> AcademicRankLevel2
        3 -> AcademicRankLevel3
        4 -> AcademicRankLevel4
        5 -> AcademicRankLevel5
        else -> Color.Gray
    }

    val subjectGradient = getSubjectGradient(cleanName)
    val primaryColor = subjectGradient.first()
    val secondaryColor = subjectGradient.getOrElse(1) { primaryColor }
    val subjectIcon = getSubjectIcon(cleanName)

    val safeStatus = subject.status.orEmpty().trim()
    val hasReprovedStatus = safeStatus.contains("Reprovad", ignoreCase = true)
    val hasApprovedStatus = safeStatus.contains("Aprovad", ignoreCase = true)

    val isReproved = hasReprovedStatus || (
        safeStatus.contains("Consolidado", ignoreCase = true) && mediaVal < 6.0 && validGradesCount > 0
    )

    val isPointsApproved = if (isSemestral) totalPoints >= 12.0 else totalPoints >= 24.0

    val isApproved = if (isReproved || hasReprovedStatus) {
        false
    } else {
        hasApprovedStatus || 
        isPointsApproved ||
        (safeStatus.contains("Consolidado", ignoreCase = true) && mediaVal >= 6.0) ||
        (validGradesCount == (if (isSemestral) 2 else 4) && mediaVal >= 6.0)
    }

    val isRecovery = !isApproved && !isReproved && (
        safeStatus.contains("Final", ignoreCase = true) ||
        safeStatus.contains("Recupera", ignoreCase = true) ||
        safeStatus.contains("Exame", ignoreCase = true) ||
        subject.recFinal.isNotBlank() ||
        (!isSemestral && validGradesCount == 4 && mediaVal in 2.0..<6.0)
    )

    val isOngoing = !isApproved && !isReproved && !isRecovery && (
        safeStatus.contains("Cursando", ignoreCase = true) || 
        safeStatus.contains("Matriculado", ignoreCase = true) || 
        safeStatus.contains("Em curso", ignoreCase = true) || 
        safeStatus.contains("Andamento", ignoreCase = true) ||
        (safeStatus.isBlank() && validGradesCount < (if (isSemestral) 2 else 4))
    )
    val isGoalReached = mediaVal >= 6.0
    val pointsRatio = (mediaVal / 10.0).toFloat().coerceIn(0f, 1f)

    val statusText = when {
        isApproved -> "Aprovado"
        isReproved -> "Reprovado"
        isRecovery || safeStatus.contains("Final", ignoreCase = true) || (!isApproved && totalPoints > 0 && pointsNeeded > 10) -> "Prova Final"
        else -> if (safeStatus.isNotBlank()) safeStatus else "Em curso"
    }

    return SubjectUiModel(
        subject = subject,
        normalizedName = normalizedName,
        cleanName = cleanName,
        bimesters = bimesters,
        totalPoints = totalPoints,
        totalPointsDisplay = formatGradeDisplay(totalPoints),
        calculatedMedia = mediaVal,
        mediaDisplay = formatGradeDisplay(mediaVal),
        pointsNeeded = pointsNeeded,
        pointsNeededDisplay = formatGradeDisplay(pointsNeeded),
        isGoalReached = isGoalReached,
        xp = xpVal,
        subjectLevel = lvl,
        badgeColor = bColor,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        safeStatus = safeStatus,
        isApproved = isApproved,
        isReproved = isReproved,
        isRecovery = isRecovery,
        isOngoing = isOngoing,
        isFavorite = subject.isFavorite,
        pointsRatio = pointsRatio,
        subjectIcon = subjectIcon,
        statusText = statusText
    )
}
