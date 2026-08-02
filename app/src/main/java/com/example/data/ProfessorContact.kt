package com.example.data

data class ProfessorContact(
    val name: String,
    val email: String,
    val discipline: String,
    val photoUrl: String = "",
    val matricula: String = ""
)
