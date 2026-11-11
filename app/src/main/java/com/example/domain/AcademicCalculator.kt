package com.example.domain

import kotlin.math.round

/**
 * Pure domain logic for IFAL academic grade calculations,
 * averages, final exam requirements, and XP tracking.
 */
object AcademicCalculator {

    const val PASSING_AVERAGE: Double = 6.0
    const val FINAL_EXAM_PASSING_AVERAGE: Double = 5.0
    const val MAX_GRADE_SCALE: Double = 10.0

    /**
     * Safely parses any grade string representation.
     * Handles: null, blank, "-", comma decimal separators, whitespace,
     * negative values (clamped to 0.0), and invalid non-finite numbers.
     */
    fun parseGrade(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        if (trimmed == "-" || trimmed == "--" || trimmed == "N/A" || trimmed == "null") return null

        val cleaned = trimmed.replace(',', '.')
        val value = cleaned.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value.isNaN()) return null

        return if (value < 0.0) 0.0 else value
    }

    /**
     * Formats a numeric grade value for display with 1 decimal place if fractional,
     * or as an integer string if whole number. Returns "-" for null.
     */
    fun formatGrade(value: Double?): String {
        if (value == null) return "-"
        val rounded = round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    /**
     * Applies final recovery (recuperação final) grade substitution.
     * In IFAL, if recFinal is greater than the lowest bimester grade,
     * it replaces that lowest grade.
     */
    fun applyRecoveryGrades(
        grade1: Double?,
        grade2: Double?,
        grade3: Double?,
        grade4: Double?,
        recFinal: Double?
    ): List<Double?> {
        var bn1 = grade1
        var bn2 = grade2
        var bn3 = grade3
        var bn4 = grade4

        if (recFinal != null && recFinal > 0.0) {
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
            if (minIdx != -1 && recFinal > minVal) {
                when (minIdx) {
                    0 -> bn1 = recFinal
                    1 -> bn2 = recFinal
                    2 -> bn3 = recFinal
                    3 -> bn4 = recFinal
                }
            }
        }
        return listOf(bn1, bn2, bn3, bn4)
    }

    /**
     * Calculates the weighted average according to IFAL academic regulation:
     * - Anual subjects: N1 (weight 2), N2 (weight 2), N3 (weight 3), N4 (weight 3) -> Total weight: 10
     * - Semestral subjects: N1 (weight 2), N2 (weight 2) -> Total weight: 4
     */
    fun calculateWeightedAverage(
        grade1: Double?,
        grade2: Double?,
        grade3: Double?,
        grade4: Double?,
        isSemestral: Boolean = false
    ): Double {
        val validGrades = listOfNotNull(grade1, grade2, grade3, grade4)
        if (validGrades.isEmpty()) return 0.0

        var weightedSum = 0.0
        var totalWeight = 0.0

        if (grade1 != null) {
            weightedSum += grade1 * 2.0
            totalWeight += 2.0
        }
        if (grade2 != null) {
            weightedSum += grade2 * 2.0
            totalWeight += 2.0
        }
        if (!isSemestral) {
            if (grade3 != null) {
                weightedSum += grade3 * 3.0
                totalWeight += 3.0
            }
            if (grade4 != null) {
                weightedSum += grade4 * 3.0
                totalWeight += 3.0
            }
        }

        return if (totalWeight > 0.0) {
            round((weightedSum / totalWeight) * 10.0) / 10.0
        } else {
            0.0
        }
    }

    /**
     * Calculates the points / grade needed in remaining evaluations to achieve the passing average (6.0).
     */
    fun calculatePointsNeeded(
        grade1: Double?,
        grade2: Double?,
        grade3: Double?,
        grade4: Double?,
        isSemestral: Boolean = false,
        targetAverage: Double = PASSING_AVERAGE
    ): Double {
        val validCount = listOfNotNull(grade1, grade2, grade3, grade4).size
        if (validCount == 0) return targetAverage

        var currentWeightedSum = 0.0
        var currentWeightTotal = 0.0

        if (grade1 != null) {
            currentWeightedSum += grade1 * 2.0
            currentWeightTotal += 2.0
        }
        if (grade2 != null) {
            currentWeightedSum += grade2 * 2.0
            currentWeightTotal += 2.0
        }
        if (!isSemestral) {
            if (grade3 != null) {
                currentWeightedSum += grade3 * 3.0
                currentWeightTotal += 3.0
            }
            if (grade4 != null) {
                currentWeightedSum += grade4 * 3.0
                currentWeightTotal += 3.0
            }
        }

        val totalExpectedWeight = if (isSemestral) 4.0 else 10.0
        val remainingWeight = totalExpectedWeight - currentWeightTotal

        return if (remainingWeight > 0.0) {
            val neededSum = (totalExpectedWeight * targetAverage - currentWeightedSum).coerceAtLeast(0.0)
            if (neededSum == 0.0) {
                0.0
            } else {
                val result = neededSum / remainingWeight
                round(result * 10.0) / 10.0
            }
        } else {
            val currentAverage = calculateWeightedAverage(grade1, grade2, grade3, grade4, isSemestral)
            val needed = (targetAverage - currentAverage).coerceAtLeast(0.0)
            round(needed * 10.0) / 10.0
        }
    }

    /**
     * Calculates the minimum grade required in the Final Exam (Prova Final / Exame Final).
     * IFAL Formula: (Media * 6 + Exame * 4) / 10 >= 5.0
     * Solving for Exame: Exame >= (50 - 6 * Media) / 4
     */
    fun calculateFinalExamNeeded(
        currentMedia: Double,
        targetFinalAverage: Double = FINAL_EXAM_PASSING_AVERAGE
    ): Double {
        if (currentMedia >= PASSING_AVERAGE) return 0.0
        if (currentMedia < 0.0) return 10.0

        val requiredFinalSum = targetFinalAverage * 10.0 // 50.0
        val examNeeded = (requiredFinalSum - (currentMedia * 6.0)) / 4.0
        val clamped = examNeeded.coerceIn(0.0, 10.0)
        return round(clamped * 10.0) / 10.0
    }

    /**
     * Calculates the final consolidated average after taking the Final Exam (Exame Final).
     * Formula: (Media * 6 + Exame * 4) / 10
     */
    fun calculateAverageWithFinalExam(
        currentMedia: Double,
        examGrade: Double
    ): Double {
        val finalAvg = (currentMedia * 6.0 + examGrade * 4.0) / 10.0
        return round(finalAvg * 10.0) / 10.0
    }

    /**
     * Normalizes a grade value for Gamification XP points (0..10 scaled to 0..100, or raw if > 10).
     */
    fun normalizeGradeToXp(grade: Double?): Double {
        if (grade == null || grade <= 0.0) return 0.0
        return if (grade <= 10.0) grade * 10.0 else grade
    }

    /**
     * Calculates subject XP points from the 4 bimester grades.
     */
    fun calculateSubjectXp(
        grade1: Double?,
        grade2: Double?,
        grade3: Double?,
        grade4: Double?
    ): Int {
        val total = normalizeGradeToXp(grade1) +
                normalizeGradeToXp(grade2) +
                normalizeGradeToXp(grade3) +
                normalizeGradeToXp(grade4)
        return total.toInt()
    }

    /**
     * Calculates user level from accumulated subject XP.
     */
    fun calculateLevel(xp: Int): Int {
        return when {
            xp >= 320 -> 5
            xp >= 230 -> 4
            xp >= 140 -> 3
            xp >= 70 -> 2
            else -> 1
        }
    }
}
