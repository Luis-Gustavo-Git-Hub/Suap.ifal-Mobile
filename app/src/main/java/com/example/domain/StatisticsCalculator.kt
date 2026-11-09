package com.example.domain

import com.example.data.Subject

data class SubjectStatistics(
    val subject: Subject,
    val grades: List<Double?>,
    val classGrades: List<Double?>,
    val average: Double?,
    val isSemestral: Boolean,
    val absences: Int,
    val status: AcademicStatus,
    val neededAverage: Double?,
    val bestCaseAverage: Double?,
    val worstCaseAverage: Double?,
    val xp: Int
)

enum class AcademicStatus { APPROVED, ONGOING, AT_RISK, RECOVERY, OTHER }

data class StageStatistics(val stage: Int, val studentAverage: Double?, val classAverage: Double?)

data class StatisticsSnapshot(
    val subjects: List<SubjectStatistics>,
    val stages: List<StageStatistics>,
    val overallAverage: Double?,
    val classAverage: Double?,
    val globalFrequency: Double?,
    val globalAbsences: Int?,
    val totalClasses: Int?,
    val passedClasses: Int?,
    val approvedCount: Int,
    val ongoingCount: Int,
    val atRiskCount: Int,
    val recoveryCount: Int,
    val aboveClassPercent: Double?,
    val totalXp: Int,
    val level: Int
) {
    val hasData: Boolean get() = subjects.isNotEmpty()

    companion object {
        val Empty = StatisticsSnapshot(
            emptyList(), emptyList(), null, null, null, null, null, null,
            0, 0, 0, 0, null, 0, 1
        )
    }
}

/** Pure calculations shared by the offline statistics screen and unit tests. */
object StatisticsCalculator {
    const val DEFAULT_MINIMUM_ATTENDANCE = 75.0
    private val numberPattern = Regex("\\d+(?:[.,]\\d+)?")

    fun calculate(
        subjects: List<Subject>,
        globalFrequency: String? = null,
        globalAbsences: String? = null,
        totalClasses: String? = null,
        passedClasses: String? = null
    ): StatisticsSnapshot {
        val rows = subjects.map { subject ->
            val rawGrades = listOf(subject.grade1, subject.grade2, subject.grade3, subject.grade4)
                .map(::grade)
            val grades = AcademicCalculator.applyRecoveryGrades(
                rawGrades[0], rawGrades[1], rawGrades[2], rawGrades[3], grade(subject.recFinal)
            ).map { it?.takeIf { value -> value <= AcademicCalculator.MAX_GRADE_SCALE } }
            val classGrades = listOf(subject.classGrade1, subject.classGrade2, subject.classGrade3, subject.classGrade4).map(::grade)
            val semestral = isSemestral(subject, grades)
            val average = if (grades.any { it != null }) {
                AcademicCalculator.calculateWeightedAverage(grades[0], grades[1], grades[2], grades[3], semestral)
            } else null
            val completed = if (semestral) grades.take(2).all { it != null } else grades.all { it != null }
            val status = classify(subject.status, average, completed)
            val needed = if (!completed) {
                AcademicCalculator.calculatePointsNeeded(grades[0], grades[1], grades[2], grades[3], semestral)
            } else null
            val best = projectedAverage(grades, semestral, 10.0)
            val worst = projectedAverage(grades, semestral, 0.0)
            val xp = AcademicCalculator.calculateSubjectXp(grades[0], grades[1], grades[2], grades[3])
            SubjectStatistics(
                subject = subject,
                grades = grades,
                classGrades = classGrades,
                average = average,
                isSemestral = semestral,
                absences = grade(subject.absences)?.toInt() ?: 0,
                status = status,
                neededAverage = needed,
                bestCaseAverage = best,
                worstCaseAverage = worst,
                xp = xp
            )
        }
        val graded = rows.mapNotNull { it.average }
        val classAverages = rows.mapNotNull { row -> row.classGrades.filterNotNull().averageOrNull() }
        val stageRows = (1..4).map { stage ->
            val stageGrades = rows.mapNotNull { it.grades[stage - 1] }
            val stageClassGrades = rows.mapNotNull { it.classGrades[stage - 1] }
            StageStatistics(stage, stageGrades.averageOrNull(), stageClassGrades.averageOrNull())
        }
        val studentClassPairs = rows.flatMap { row ->
            row.grades.indices.mapNotNull { i ->
                val student = row.grades[i]
                val classroom = row.classGrades[i]
                if (student != null && classroom != null) student to classroom else null
            }
        }
        val freq = parsePercent(globalFrequency) ?: deriveFrequency(globalAbsences, passedClasses)
        val absences = globalAbsences?.toIntOrNull()?.takeIf { it >= 0 }
        val xp = rows.sumOf { it.xp }
        return StatisticsSnapshot(
            subjects = rows,
            stages = stageRows,
            overallAverage = graded.averageOrNull(),
            classAverage = classAverages.averageOrNull(),
            globalFrequency = freq,
            globalAbsences = absences,
            totalClasses = totalClasses?.toIntOrNull()?.takeIf { it > 0 },
            passedClasses = passedClasses?.toIntOrNull()?.takeIf { it > 0 },
            approvedCount = rows.count { it.status == AcademicStatus.APPROVED },
            ongoingCount = rows.count { it.status == AcademicStatus.ONGOING },
            atRiskCount = rows.count { it.status == AcademicStatus.AT_RISK },
            recoveryCount = rows.count { it.status == AcademicStatus.RECOVERY },
            aboveClassPercent = studentClassPairs.takeIf { it.isNotEmpty() }
                ?.let { pairs -> pairs.count { it.first > it.second }.toDouble() * 100.0 / pairs.size },
            totalXp = xp,
            level = AcademicCalculator.calculateLevel(xp)
        )
    }

    fun requiredAverage(subject: Subject, target: Double = AcademicCalculator.PASSING_AVERAGE): Double? {
        val originalGrades = listOf(subject.grade1, subject.grade2, subject.grade3, subject.grade4).map(::grade)
        val grades = AcademicCalculator.applyRecoveryGrades(
            originalGrades[0], originalGrades[1], originalGrades[2], originalGrades[3], grade(subject.recFinal)
        ).map { it?.takeIf { value -> value <= AcademicCalculator.MAX_GRADE_SCALE } }
        if (grades.none { it != null }) return target.coerceIn(0.0, AcademicCalculator.MAX_GRADE_SCALE)
        val semestral = isSemestral(subject, grades)
        val completed = if (semestral) grades.take(2).all { it != null } else grades.all { it != null }
        if (completed) return null
        return AcademicCalculator.calculatePointsNeeded(grades[0], grades[1], grades[2], grades[3], semestral, target)
    }

    private fun projectedAverage(grades: List<Double?>, semestral: Boolean, remainingGrade: Double): Double =
        AcademicCalculator.calculateWeightedAverage(
            grades[0] ?: remainingGrade,
            grades[1] ?: remainingGrade,
            if (semestral) null else grades[2] ?: remainingGrade,
            if (semestral) null else grades[3] ?: remainingGrade,
            semestral
        )

    private fun classify(status: String, average: Double?, completed: Boolean): AcademicStatus {
        val value = status.lowercase()
        return when {
            value.contains("recuper") || value.contains("exame") -> AcademicStatus.RECOVERY
            value.contains("aprov") || (completed && average != null && average >= AcademicCalculator.PASSING_AVERAGE) -> AcademicStatus.APPROVED
            value.contains("reprov") -> AcademicStatus.AT_RISK
            average != null && average < AcademicCalculator.PASSING_AVERAGE && !completed -> AcademicStatus.AT_RISK
            average != null && !completed -> AcademicStatus.ONGOING
            else -> AcademicStatus.OTHER
        }
    }

    private fun isSemestral(subject: Subject, grades: List<Double?>): Boolean =
        subject.status.contains("Consolidado", ignoreCase = true) ||
            subject.status.contains("Aprovado", ignoreCase = true) ||
            subject.status.contains("Reprovado", ignoreCase = true) ||
            (grades[2] == null && grades[3] == null && grades.take(2).count { it != null } == 2 &&
                !subject.status.contains("Cursando", ignoreCase = true))

    private fun grade(value: String?): Double? = AcademicCalculator.parseGrade(value)
        ?.takeIf { it <= AcademicCalculator.MAX_GRADE_SCALE }

    private fun parsePercent(value: String?): Double? = value?.let(numberPattern::find)?.value
        ?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }

    private fun deriveFrequency(absences: String?, passedClasses: String?): Double? {
        val total = passedClasses?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        val missed = absences?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return ((total - missed).coerceAtLeast(0).toDouble() / total * 100.0).coerceIn(0.0, 100.0)
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
