package com.example.ui

import com.example.data.Subject
import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectUiModelTest {
    @Test
    fun gradeAndAverageUseBrazilianDecimalSeparator() {
        val model = buildSubjectUiModel(
            Subject(
                id = "matematica",
                name = "Matemática",
                grade1 = "8.5",
                classGrade1 = "9.0"
            )
        )

        assertEquals("8,5", model.bimesters.first().displayGrade)
        assertEquals("9", model.bimesters.first().classGradeDisplay)
        assertEquals("2,1", model.mediaDisplay)
    }
}
