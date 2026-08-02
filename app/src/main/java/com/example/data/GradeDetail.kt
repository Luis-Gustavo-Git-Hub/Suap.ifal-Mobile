package com.example.data

data class GradeDetailRow(
    val sigla: String,
    val tipo: String,
    val descricao: String,
    val peso: String,
    val nota: String
)

data class EtapaDetail(
    val title: String,
    val items: List<GradeDetailRow>
)
