package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
private val WORDS_SPLIT_REGEX = Regex("[^a-z0-9]+")

private val subjectColorCache = ConcurrentHashMap<String, Color>()

/**
 * Unified subject palette for SUAP academic subjects across the application.
 */
fun getSubjectColor(subjectName: String, isDark: Boolean = false): Color {
    if (subjectName.isBlank()) return if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
    return subjectColorCache.getOrPut(subjectName) {
        computeSubjectColor(subjectName)
    }
}

private fun computeSubjectColor(subjectName: String): Color {
    val name = Normalizer.normalize(subjectName, Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase()

    val words = name.split(WORDS_SPLIT_REGEX)

    return when {
        // AGROPECUARIA
        words.contains("dtcr") || name.contains("desenho tecnico") || name.contains("construcoes rurais") -> Color(0xFFE65100) // Deep Orange
        words.contains("extdr") || name.contains("extensao rural") -> Color(0xFFF57C00) // Orange
        words.contains("geag") || name.contains("geociencias") || name.contains("agroecologia") -> Color(0xFF689F38) // Light Green
        words.contains("gepr") || name.contains("gestao da producao") -> Color(0xFF455A64) // Blue Grey
        words.contains("gerr") || name.contains("recursos renovaveis") || name.contains("meio ambiente") || name.contains("ecologia") || name.contains("saneamento") || name.contains("ambiental") || name.contains("residuo") -> Color(0xFF388E3C) // Green
        words.contains("gtec") || name.contains("geotecnologias") || name.contains("topografia") -> Color(0xFF8D6E63) // Brown
        words.contains("irdr") || name.contains("irrigacao") || name.contains("drenagem") -> Color(0xFF0277BD) // Light Blue
        words.contains("mapd") || name.contains("agrimplementos") || name.contains("maquinas") || name.contains("agricultura") || name.contains("agro") -> Color(0xFFF57C00) // Orange
        words.contains("meca") || name.contains("mecanizacao") || name.contains("conservacao do solo") || name.contains("mecanica") || name.contains("manutencao") || name.contains("usinagem") || name.contains("motores") -> Color(0xFF455A64) // Blue Grey
        words.contains("osppmr") || name.contains("sistemas de producao") || name.contains("manejo de recursos") -> Color(0xFF00796B) // Teal
        words.contains("prag") || name.contains("fitossanidade") || name.contains("pragas") -> Color(0xFFAFB42B) // Lime
        words.contains("spa") || name.contains("sistema de producao animal") || name.contains("producao animal") -> Color(0xFF795548) // Brown
        words.contains("ssag") || name.contains("saude e seguranca") || name.contains("seguranca na agropecuaria") || name.contains("sistemas agropecuarios") -> Color(0xFF0097A7) // Cyan
        name.contains("ti agp") || words.contains("tia") || name.contains("tecnologia da informacao") -> Color(0xFF0288D1) // Cyan Blue
        words.contains("tpcc") || words.contains("tcc") || name.contains("praticas cientifico") || name.contains("trabalho de conclusao") || name.contains("estagio") || name.contains("pesquisa") || name.contains("seminario") || name.contains("metodologia") -> Color(0xFF5C6BC0) // Indigo
        words.contains("zoo") || name.contains("zootecnia") || name.contains("pecuaria") -> Color(0xFF8D6E63) // Brown
        name.contains("cana") || name.contains("acucar") -> Color(0xFF7CB342) // Light Green
        name.contains("solo") || name.contains("plantas") -> Color(0xFFF57C00) // Orange

        // AGROINDUSTRIA
        words.contains("anab") || name.contains("analise de alimentos") || name.contains("bromatologia") || name.contains("bioquimica dos alimentos") -> Color(0xFF00897B) // Teal
        words.contains("bcta") || name.contains("biotecnologia") -> Color(0xFF8E24AA) // Purple
        words.contains("embe") || name.contains("embalagens") -> Color(0xFF546E7A) // Blue Grey
        words.contains("gaal") || name.contains("gestao agroindustrial") || name.contains("administracao") -> Color(0xFF3949AB) // Indigo
        words.contains("hial") || name.contains("higiene") || name.contains("legislacao") || name.contains("vigilancia sanitaria") -> Color(0xFF00ACC1) // Cyan
        words.contains("inag") || name.contains("instalacoes") || name.contains("equipamentos") -> Color(0xFF78909C) // Blue Grey
        words.contains("mbia") || name.contains("microbiologia") -> Color(0xFF43A047) // Green
        words.contains("nutr") || name.contains("nutricao") -> Color(0xFFE53935) // Red
        words.contains("oaqa") || name.contains("analise sensorial") || name.contains("controle de qualidade") -> Color(0xFFD81B60) // Pink
        words.contains("proc") || name.contains("processamento") || name.contains("conservacao") -> Color(0xFFFB8C00) // Dark Orange
        words.contains("quim") || name.contains("quimica dos alimentos") -> Color(0xFF1E88E5) // Blue
        words.contains("tcar") || name.contains("carnes") || name.contains("pescados") || name.contains("derivados") -> Color(0xFFC2185B) // Dark Pink
        words.contains("tgra") || name.contains("graos") || name.contains("cereais") || name.contains("panificacao") -> Color(0xFFFDD835) // Yellow
        words.contains("tlet") || name.contains("leite") || name.contains("laticinios") -> Color(0xFF039BE5) // Light Blue
        words.contains("tveg") || name.contains("frutas") || name.contains("hortalicas") || name.contains("vegetais") -> Color(0xFF7CB342) // Light Green
        name.contains("bebidas") || name.contains("fermentados") -> Color(0xFF8E24AA) // Purple
        name.contains("oleos") || name.contains("gorduras") -> Color(0xFFFBC02D) // Yellow

        // INFORMATICA
        words.contains("alg") || name.contains("algoritmo") || name.contains("logica") -> Color(0xFF0288D1) // Light Blue
        words.contains("bd") || name.contains("banco de dados") || name.contains("sql") -> Color(0xFFF57C00) // Orange
        words.contains("red") || name.contains("redes") || name.contains("infraestrutura") -> Color(0xFF7B1FA2) // Purple
        words.contains("web") || name.contains("internet") || name.contains("html") || name.contains("css") -> Color(0xFFE64A19) // Deep Orange
        words.contains("poo") || name.contains("orientada a objetos") || name.contains("java") || name.contains("kotlin") -> Color(0xFFD32F2F) // Red
        words.contains("so") || name.contains("sistemas operacionais") || name.contains("linux") -> Color(0xFF388E3C) // Green
        words.contains("eng") || name.contains("engenharia de software") || name.contains("requisitos") -> Color(0xFF512DA8) // Deep Purple
        words.contains("prog") || name.contains("programacao") || name.contains("desenvolvimento") -> Color(0xFF1976D2) // Blue
        words.contains("seg") || name.contains("seguranca") || name.contains("criptografia") -> Color(0xFFC2185B) // Pink
        words.contains("ia") || name.contains("inteligencia artificial") || name.contains("dados") -> Color(0xFF0097A7) // Cyan
        name.contains("mobile") || name.contains("dispositivos moveis") || name.contains("android") -> Color(0xFF388E3C) // Green
        name.contains("computacao") || name.contains("informatica") -> Color(0xFF1976D2) // Blue
        name.contains("hardware") || name.contains("arquitetura") -> Color(0xFF5D4037) // Brown

        // BASE COMUM
        name.contains("matematica") || words.contains("mat") || name.contains("calculo") || name.contains("algebra") || name.contains("geometria") -> Color(0xFF1976D2) // Blue
        name.contains("portugues") || name.contains("literatura") || name.contains("redacao") || name.contains("lingua portuguesa") || words.contains("lp") -> Color(0xFFC2185B) // Dark Pink
        name.contains("historia") || words.contains("his") -> Color(0xFF8D6E63) // Brown
        name.contains("geografia") || words.contains("geo") -> Color(0xFF388E3C) // Green
        name.contains("biologia") || words.contains("bio") -> Color(0xFF689F38) // Light Green
        name.contains("fisica") || words.contains("fis") -> Color(0xFF7B1FA2) // Purple
        name.contains("quimica") || words.contains("qui") -> Color(0xFF0097A7) // Cyan
        name.contains("filosofia") || words.contains("fil") -> Color(0xFF5D4037) // Dark Brown
        name.contains("sociologia") || words.contains("soc") -> Color(0xFFE64A19) // Deep Orange
        name.contains("ingles") || name.contains("espanhol") || name.contains("estrangeira") || words.contains("lem") -> Color(0xFF00796B) // Teal
        name.contains("artes") || name.contains("arte") || name.contains("musica") || name.contains("teatro") -> Color(0xFFE91E63) // Pink
        name.contains("educacao fisica") || words.contains("edf") || words.contains("ef") -> Color(0xFFF57C00) // Orange

        else -> {
            val hash = name.hashCode()
            val defaultColors = listOf(
                Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), Color(0xFF7B1FA2),
                Color(0xFFF57C00), Color(0xFF00796B), Color(0xFFC2185B), Color(0xFF0288D1),
                Color(0xFF512DA8), Color(0xFF689F38), Color(0xFFE64A19), Color(0xFF0097A7)
            )
            defaultColors[kotlin.math.abs(hash) % defaultColors.size]
        }
    }
}

private val paletteGradientCache = ConcurrentHashMap<String, List<Color>>()
fun getSubjectGradient(subjectName: String): List<androidx.compose.ui.graphics.Color> {
    if (subjectName.isBlank()) return listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF4F46E5), androidx.compose.ui.graphics.Color(0xFF4338CA))
    return paletteGradientCache.getOrPut(subjectName) {
        computeSubjectGradient(subjectName)
    }
}

private fun computeSubjectGradient(subjectName: String): List<androidx.compose.ui.graphics.Color> {
    val name = java.text.Normalizer.normalize(subjectName, java.text.Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase()

    val words = name.split(WORDS_SPLIT_REGEX)

    return when {
        // MATEMATICA / EXATAS / ESTATISTICA / CALCULO
        words.contains("mate") || name.contains("matematica") || name.contains("estatistica") || name.contains("calculo") || name.contains("geometria") || name.contains("algebra") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF3B82F6), androidx.compose.ui.graphics.Color(0xFF2563EB), androidx.compose.ui.graphics.Color(0xFF1D4ED8))

        // FISICA / ASTRONOMIA
        words.contains("fisc") || name.contains("fisica") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF06B6D4), androidx.compose.ui.graphics.Color(0xFF0284C7), androidx.compose.ui.graphics.Color(0xFF2563EB))

        // QUIMICA / BIOQUIMICA
        words.contains("quim") || name.contains("quimica") || name.contains("bioquimica") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF8B5CF6), androidx.compose.ui.graphics.Color(0xFFA855F7), androidx.compose.ui.graphics.Color(0xFFD946EF))

        // BIOLOGIA / NATUREZA / ECOLOGIA / RECURSOS RENOVAVEIS
        name.contains("biologia") || name.contains("natureza") || name.contains("genetica") || name.contains("ecologia") || words.contains("gerr") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF10B981), androidx.compose.ui.graphics.Color(0xFF059669), androidx.compose.ui.graphics.Color(0xFF84CC16))

        // INFORMATICA / PROGRAMACAO / WEB / SISTEMAS
        words.contains("info") || name.contains("informatica") || name.contains("programacao") || name.contains("algoritmo") || name.contains("computador") || name.contains("software") || name.contains("web") || name.contains("rede") || name.contains("sistemas") || name.contains("desenvolvimento") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF06B6D4), androidx.compose.ui.graphics.Color(0xFF3B82F6), androidx.compose.ui.graphics.Color(0xFF6366F1))

        // PORTUGUES / LINGUAGENS / LITERATURA / REDACAO
        words.contains("lipo") || words.contains("ling") || name.contains("portugues") || name.contains("lingua portuguesa") || name.contains("redacao") || name.contains("literatura") || name.contains("linguagens") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFF97316), androidx.compose.ui.graphics.Color(0xFFEA580C), androidx.compose.ui.graphics.Color(0xFFE11D48))

        // HISTORIA / FILOSOFIA / SOCIOLOGIA / HUMANAS
        words.contains("hist") || name.contains("historia") || words.contains("filo") || name.contains("filosofia") || words.contains("soci") || name.contains("sociologia") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFF59E0B), androidx.compose.ui.graphics.Color(0xFFD97706), androidx.compose.ui.graphics.Color(0xFFB45309))

        // GEOGRAFIA / GEOPOLITICA
        words.contains("geog") || name.contains("geografia") || name.contains("geopolitica") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF14B8A6), androidx.compose.ui.graphics.Color(0xFF0D9488), androidx.compose.ui.graphics.Color(0xFF059669))

        // IDIOMAS (INGLES / ESPANHOL / LIBRAS / FRANCES)
        name.contains("ingles") || name.contains("espanhol") || name.contains("libras") || name.contains("frances") || name.contains("lingua estrangeira") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFEC4899), androidx.compose.ui.graphics.Color(0xFFDB2777), androidx.compose.ui.graphics.Color(0xFF9333EA))

        // ARTES / ED. FISICA
        name.contains("arte") || name.contains("musica") || name.contains("danca") || words.contains("edfi") || name.contains("educacao fisica") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFF43F5E), androidx.compose.ui.graphics.Color(0xFFE11D48), androidx.compose.ui.graphics.Color(0xFFBE123C))

        // AGROPECUARIA / ZOOTECNIA / MAQUINAS / SOLOS
        words.contains("mapd") || words.contains("zoo") || name.contains("agro") || name.contains("agricultura") || name.contains("solo") || name.contains("plantas") ->
            listOf(androidx.compose.ui.graphics.Color(0xFF84CC16), androidx.compose.ui.graphics.Color(0xFF65A30D), androidx.compose.ui.graphics.Color(0xFF15803D))

        // AGROINDUSTRIA / ALIMENTOS / LATICINIOS
        words.contains("agd") || words.contains("carn") || name.contains("alimento") || name.contains("agroindustria") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFFB923C), androidx.compose.ui.graphics.Color(0xFFF97316), androidx.compose.ui.graphics.Color(0xFFC2410C))

        // ELETRICA / ELETRONICA
        name.contains("eletro") || name.contains("eletrica") || name.contains("circuito") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFFBBF24), androidx.compose.ui.graphics.Color(0xFFF59E0B), androidx.compose.ui.graphics.Color(0xFFD97706))

        // SAUDE / ENFERMAGEM
        name.contains("saude") || name.contains("enfermagem") || name.contains("primeiros socorros") ->
            listOf(androidx.compose.ui.graphics.Color(0xFFFB7185), androidx.compose.ui.graphics.Color(0xFFF43F5E), androidx.compose.ui.graphics.Color(0xFFE11D48))

        // DEFAULT / OUTROS
        else ->
            listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF4F46E5), androidx.compose.ui.graphics.Color(0xFF4338CA))
    }
}

val FriendCardColorPresets = listOf(
    "" to ("Padrão M3" to Color.Unspecified),
    "#E91E63" to ("Rosa" to Color(0xFFE91E63)),
    "#8B5CF6" to ("Roxo" to Color(0xFF8B5CF6)),
    "#3B82F6" to ("Azul" to Color(0xFF3B82F6)),
    "#06B6D4" to ("Ciano" to Color(0xFF06B6D4)),
    "#10B981" to ("Verde" to Color(0xFF10B981)),
    "#F59E0B" to ("Amarelo" to Color(0xFFF59E0B)),
    "#F97316" to ("Laranja" to Color(0xFFF97316)),
    "#EF4444" to ("Vermelho" to Color(0xFFEF4444)),
    "#1E293B" to ("Grafite" to Color(0xFF1E293B))
)
