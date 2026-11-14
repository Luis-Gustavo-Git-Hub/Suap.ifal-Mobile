package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationItem(
    val id: String = "",
    val subjectName: String,
    val title: String,
    val date: String,
    val timeOrDetail: String = "",
    val type: String = "Avaliação"
)
