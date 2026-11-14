package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Stable

@Stable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val url: String = "",
    val grade1: String = "",
    val grade2: String = "",
    val rec1: String = "",
    val grade3: String = "",
    val grade4: String = "",
    val classGrade1: String = "",
    val classGrade2: String = "",
    val classGrade3: String = "",
    val classGrade4: String = "",
    val rec2: String = "",
    val recFinal: String = "",
    val finalGrade: String = "",
    val absences: String = "",
    val status: String = "",
    val isFavorite: Boolean = false,
    val detailUrl: String = ""
) {
    companion object {
        fun generateStableId(name: String, url: String = ""): String {
            val trimmedUrl = url.trim()
            if (trimmedUrl.isNotEmpty()) return trimmedUrl
            return name.trim()
        }
    }
}
data class AulaLink(
    val title: String,
    val url: String
)

data class AulaHistory(
    val data: String,
    val etapa: String,
    val numAulas: String,
    val numFaltas: String,
    val conteudo: String,
    val professor: String,
    val attachmentUrl: String = "",
    val attachmentTitle: String = "",
    val rawHtml: String = "",
    val links: List<AulaLink> = emptyList()
)
