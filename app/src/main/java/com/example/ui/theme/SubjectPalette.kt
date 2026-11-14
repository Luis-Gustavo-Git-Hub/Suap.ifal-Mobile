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
