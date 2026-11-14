package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademicCalculatorTest {

    @Test
    fun `parseGrade handles normal and edge cases`() {
        // Normal cases
        assertEquals(8.5, AcademicCalculator.parseGrade("8.5"))
        assertEquals(8.5, AcademicCalculator.parseGrade("8,5"))
        assertEquals(10.0, AcademicCalculator.parseGrade("10"))
        assertEquals(10.0, AcademicCalculator.parseGrade(" 10.0 "))
        assertEquals(0.0, AcademicCalculator.parseGrade("0"))

        // Edge cases: empty, dash, null, invalid text
        assertNull(AcademicCalculator.parseGrade(null))
        assertNull(AcademicCalculator.parseGrade(""))
        assertNull(AcademicCalculator.parseGrade("   "))
        assertNull(AcademicCalculator.parseGrade("-"))
        assertNull(AcademicCalculator.parseGrade("--"))
        assertNull(AcademicCalculator.parseGrade("N/A"))
        assertNull(AcademicCalculator.parseGrade("abc"))

        // Values above 10 (e.g. 0-100 scale or demo data)
        assertEquals(14.0, AcademicCalculator.parseGrade("14.0"))
        assertEquals(85.0, AcademicCalculator.parseGrade("85,0"))

        // Negative numbers clamped to 0.0
        assertEquals(0.0, AcademicCalculator.parseGrade("-3.5"))
    }

    @Test
    fun `formatGrade formats numbers cleanly`() {
        assertEquals("-", AcademicCalculator.formatGrade(null))
        assertEquals("8", AcademicCalculator.formatGrade(8.0))
        assertEquals("8.5", AcademicCalculator.formatGrade(8.5))
        assertEquals("0", AcademicCalculator.formatGrade(0.0))
        assertEquals("9.2", AcademicCalculator.formatGrade(9.24))
        assertEquals("9.3", AcademicCalculator.formatGrade(9.26))
    }

    @Test
    fun `applyRecoveryGrades replaces lowest grade when higher`() {
        // recFinal (7.0) is higher than min grade N2 (4.0)
        val result = AcademicCalculator.applyRecoveryGrades(
            grade1 = 8.0,
            grade2 = 4.0,
            grade3 = 6.0,
            grade4 = 5.0,
            recFinal = 7.0
        )
        assertEquals(listOf(8.0, 7.0, 6.0, 5.0), result)

        // recFinal (3.0) is lower than min grade N2 (4.0) -> no substitution
        val noChange = AcademicCalculator.applyRecoveryGrades(
            grade1 = 8.0,
            grade2 = 4.0,
            grade3 = 6.0,
            grade4 = 5.0,
            recFinal = 3.0
        )
        assertEquals(listOf(8.0, 4.0, 6.0, 5.0), noChange)

        // null recFinal -> no substitution
        val nullRec = AcademicCalculator.applyRecoveryGrades(
            grade1 = 8.0,
            grade2 = 4.0,
            grade3 = null,
            grade4 = null,
            recFinal = null
        )
        assertEquals(listOf(8.0, 4.0, null, null), nullRec)
    }

    @Test
    fun `calculateWeightedAverage calculates IFAL anual and semestral averages correctly`() {
        // Anual: N1*2 + N2*2 + N3*3 + N4*3 / 10
        // (6*2 + 6*2 + 6*3 + 6*3) / 10 = (12 + 12 + 18 + 18) / 10 = 60 / 10 = 6.0
        assertEquals(6.0, AcademicCalculator.calculateWeightedAverage(6.0, 6.0, 6.0, 6.0), 0.001)

        // Anual with different grades:
        // (8*2 + 7*2 + 5*3 + 6*3) / 10 = (16 + 14 + 15 + 18) / 10 = 63 / 10 = 6.3
        assertEquals(6.3, AcademicCalculator.calculateWeightedAverage(8.0, 7.0, 5.0, 6.0), 0.001)

        // Partial bimesters (N1 and N2 only):
        // (8*2 + 6*2) / 4 = (16 + 12) / 4 = 28 / 4 = 7.0
        assertEquals(7.0, AcademicCalculator.calculateWeightedAverage(8.0, 6.0, null, null), 0.001)

        // Semestral: N1*2 + N2*2 / 4
        // (9*2 + 5*2) / 4 = 28 / 4 = 7.0
        assertEquals(7.0, AcademicCalculator.calculateWeightedAverage(9.0, 5.0, 10.0, 10.0, isSemestral = true), 0.001)

        // All null
        assertEquals(0.0, AcademicCalculator.calculateWeightedAverage(null, null, null, null), 0.001)
    }

    @Test
    fun `calculatePointsNeeded computes required grade for passing average 6_0`() {
        // Already passing with full grades
        assertEquals(0.0, AcademicCalculator.calculatePointsNeeded(7.0, 7.0, 7.0, 7.0), 0.001)

        // Anual: N1=6, N2=6 (sum=24, remaining weight=6). Target=60 -> Needed in N3 and N4: (60 - 24) / 6 = 6.0
        assertEquals(6.0, AcademicCalculator.calculatePointsNeeded(6.0, 6.0, null, null), 0.001)

        // Anual: N1=4, N2=4 (sum=16, remaining weight=6). Target=60 -> Needed: (60 - 16) / 6 = 44 / 6 = 7.33 -> 7.3
        assertEquals(7.3, AcademicCalculator.calculatePointsNeeded(4.0, 4.0, null, null), 0.05)

        // Semestral: N1=4 (sum=8, remaining weight=2). Target=24 -> Needed in N2: (24 - 8) / 2 = 8.0
        assertEquals(8.0, AcademicCalculator.calculatePointsNeeded(4.0, null, null, null, isSemestral = true), 0.001)

        // All 4 bimesters done, failed with 5.0 -> Needed points: (6.0 - 5.0) = 1.0
        assertEquals(1.0, AcademicCalculator.calculatePointsNeeded(5.0, 5.0, 5.0, 5.0), 0.001)
    }

    @Test
    fun `calculateFinalExamNeeded computes required grade in Prova Final correctly`() {
        // IFAL Formula: Exame >= (50 - 6 * Media) / 4

        // If Media is 6.0 -> Already approved, exam needed is 0.0
        assertEquals(0.0, AcademicCalculator.calculateFinalExamNeeded(6.0), 0.001)
        assertEquals(0.0, AcademicCalculator.calculateFinalExamNeeded(7.5), 0.001)

        // If Media is 5.0 -> (50 - 30) / 4 = 20 / 4 = 5.0
        assertEquals(5.0, AcademicCalculator.calculateFinalExamNeeded(5.0), 0.001)

        // If Media is 4.0 -> (50 - 24) / 4 = 26 / 4 = 6.5
        assertEquals(6.5, AcademicCalculator.calculateFinalExamNeeded(4.0), 0.001)

        // If Media is 3.0 -> (50 - 18) / 4 = 32 / 4 = 8.0
        assertEquals(8.0, AcademicCalculator.calculateFinalExamNeeded(3.0), 0.001)

        // If Media is 2.0 -> (50 - 12) / 4 = 38 / 4 = 9.5
        assertEquals(9.5, AcademicCalculator.calculateFinalExamNeeded(2.0), 0.001)

        // If Media is 1.0 -> (50 - 6) / 4 = 44 / 4 = 11.0 -> Clamped to 10.0
        assertEquals(10.0, AcademicCalculator.calculateFinalExamNeeded(1.0), 0.001)

        // If Media is 0.0 -> Clamped to 10.0
        assertEquals(10.0, AcademicCalculator.calculateFinalExamNeeded(0.0), 0.001)
    }

    @Test
    fun `calculateAverageWithFinalExam produces consolidated grade`() {
        // Media 4.0 and Exam 6.5 -> (4.0 * 6 + 6.5 * 4) / 10 = (24 + 26) / 10 = 50 / 10 = 5.0
        assertEquals(5.0, AcademicCalculator.calculateAverageWithFinalExam(4.0, 6.5), 0.001)

        // Media 5.0 and Exam 5.0 -> (30 + 20) / 10 = 5.0
        assertEquals(5.0, AcademicCalculator.calculateAverageWithFinalExam(5.0, 5.0), 0.001)
    }

    @Test
    fun `calculateSubjectXp and calculateLevel handle gamification XP correctly`() {
        // 4 bimesters of 10 -> 100 + 100 + 100 + 100 = 400 XP -> Level 5 (>= 320)
        val xpMax = AcademicCalculator.calculateSubjectXp(10.0, 10.0, 10.0, 10.0)
        assertEquals(400, xpMax)
        assertEquals(5, AcademicCalculator.calculateLevel(xpMax))

        // Level 1 (< 70)
        assertEquals(1, AcademicCalculator.calculateLevel(50))

        // Level 2 (70..139)
        assertEquals(2, AcademicCalculator.calculateLevel(70))
        assertEquals(2, AcademicCalculator.calculateLevel(130))

        // Level 3 (140..229)
        assertEquals(3, AcademicCalculator.calculateLevel(140))

        // Level 4 (230..319)
        assertEquals(4, AcademicCalculator.calculateLevel(230))
    }
}
