package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable
    data object Login : NavRoute

    @Serializable
    data object Dashboard : NavRoute

    @Serializable
    data object Boletim : NavRoute

    @Serializable
    data object Horarios : NavRoute

    @Serializable
    data object Avisos : NavRoute

    @Serializable
    data object Friends : NavRoute

    @Serializable
    data object Almoco : NavRoute

    @Serializable
    data class Aulas(val title: String = "", val url: String = "") : NavRoute

    @Serializable
    data object Assuntos : NavRoute

    @Serializable
    data class SubjectTopics(val name: String = "", val detailUrl: String = "") : NavRoute

    @Serializable
    data class SuapWebView(val title: String = "", val url: String = "") : NavRoute

    @Serializable
    data class PdfViewer(val title: String = "", val url: String = "") : NavRoute

    @Serializable
    data object Credits : NavRoute

    @Serializable
    data object Logs : NavRoute

    @Serializable
    data class SigaaLogin(val next: String = "") : NavRoute

    @Serializable
    data object SigaaAlmoco : NavRoute

    @Serializable
    data object SigaaCertificado : NavRoute

    @Serializable
    data object SituacaoJustificativa : NavRoute

    @Serializable
    data class DetalhesJustificativa(val id: String = "") : NavRoute

    @Serializable
    data class ReposicaoJustificativa(val id: String = "") : NavRoute

    @Serializable
    data object JustificativaFalta : NavRoute

    @Serializable
    data class Statistics(val placeholder: String = "") : NavRoute

    @Serializable
    data class Profile(val scrollToSigaa: Boolean = false, val scrollToSuap: Boolean = false) : NavRoute
}

