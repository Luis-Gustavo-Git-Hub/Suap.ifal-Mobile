package com.example.domain

import com.example.data.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsCalculatorTest {
    @Test
    fun `empty data does not fabricate averages`() {
        val result = StatisticsCalculator.calculate(emptyList())
        assertTrue(result.subjects.isEmpty())
        assertNull(result.overallAverage)
        assertNull(result.globalFrequency)
        assertEquals(6.0, StatisticsCalculator.requiredAverage(Subject())!!, 0.0)
    }

    @Test
    fun `invalid and out of scale grades are ignored`() {
        val result = StatisticsCalculator.calculate(
            listOf(Subject(name = "Química", grade1 = "11", grade2 = "x", grade3 = "-"))
        )
        assertNull(result.subjects.single().average)
        assertEquals(0, result.totalXp)
    }

    @Test
    fun `one bimester remains an ongoing subject and simulator uses academic rule`() {
        val subject = Subject(name = "Matemática", grade1 = "8,0", status = "Cursando")
        val result = StatisticsCalculator.calculate(listOf(subject))
        assertEquals(AcademicStatus.ONGOING, result.subjects.single().status)
        assertEquals(8.0, result.stages.first().studentAverage!!, 0.0)
        assertEquals(5.5, StatisticsCalculator.requiredAverage(subject)!!, 0.0)
    }

    @Test
    fun `recovery grade follows AcademicCalculator recovery substitution`() {
        val result = StatisticsCalculator.calculate(
            listOf(Subject(name = "História", grade1 = "4", grade2 = "7", recFinal = "8", status = "Aprovado"))
        )
        val expected = AcademicCalculator.applyRecoveryGrades(4.0, 7.0, null, null, 8.0).first()
        assertEquals(expected!!, result.subjects.single().grades.first()!!, 0.0)
    }

    @Test
    fun `class comparisons omit missing classroom values`() {
        val result = StatisticsCalculator.calculate(
            listOf(
                Subject(name = "Física", grade1 = "8", classGrade1 = "6"),
                Subject(name = "Biologia", grade1 = "7", classGrade1 = "-")
            )
        )
        assertEquals(100.0, result.aboveClassPercent!!, 0.0)
        assertEquals(6.0, result.stages.first().classAverage!!, 0.0)
    }

    @Test
    fun `recovery status and attendance data are reported`() {
        val result = StatisticsCalculator.calculate(
            listOf(Subject(name = "Física", grade1 = "4", status = "Em recuperação")),
            globalFrequency = "72,5%",
            globalAbsences = "11",
            totalClasses = "80",
            passedClasses = "40"
        )
        assertEquals(AcademicStatus.RECOVERY, result.subjects.single().status)
        assertEquals(72.5, result.globalFrequency!!, 0.0)
        assertEquals(11, result.globalAbsences)
    }
}
