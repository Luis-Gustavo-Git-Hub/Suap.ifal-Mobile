package com.example.data

data class ProfessorContact(
    val name: String,
    val email: String,
    val discipline: String,
    val photoUrl: String = "",
    val matricula: String = "",
    /** true quando o e-mail foi estimado a partir do nome (não veio do SUAP). */
    val emailEstimated: Boolean = false,
    /** Link do perfil do professor no SUAP (usado para buscar o e-mail real). */
    val profileUrl: String = ""
)
