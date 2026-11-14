package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Stable

@Stable
@Entity(tableName = "academic_events")
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateInfo: String,
    val description: String,
    val color: String,
    val startDateStr: String? = null,
    val endDateStr: String? = null
)
