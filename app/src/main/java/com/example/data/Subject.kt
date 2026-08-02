package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
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
)
