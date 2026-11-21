package com.example.ui

import androidx.compose.material.icons.outlined.*

private val subjectIconCache = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.ui.graphics.vector.ImageVector>()
private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val WORDS_SPLIT_REGEX = Regex("\\W+")

fun getSubjectIcon(subjectName: String): androidx.compose.ui.graphics.vector.ImageVector {
    if (subjectName.isBlank()) return androidx.compose.material.icons.Icons.Outlined.Folder
    return subjectIconCache.getOrPut(subjectName) {
        computeSubjectIcon(subjectName)
    }
}

private fun computeSubjectIcon(subjectName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val name = java.text.Normalizer.normalize(subjectName, java.text.Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase()

    val words = name.split(WORDS_SPLIT_REGEX)

    return when {
        // AGROPECUARIA
        words.contains("dtcr") || name.contains("desenho tecnico") || name.contains("construcoes rurais") -> androidx.compose.material.icons.Icons.Outlined.Architecture
        words.contains("extdr") || name.contains("extensao rural") -> androidx.compose.material.icons.Icons.Outlined.NaturePeople
        words.contains("geag") || name.contains("geociencias") || name.contains("agroecologia") -> androidx.compose.material.icons.Icons.Outlined.Landscape
        words.contains("gepr") || name.contains("gestao da producao") -> androidx.compose.material.icons.Icons.Outlined.Factory
        words.contains("gerr") || name.contains("recursos renovaveis") || name.contains("meio ambiente") || name.contains("ecologia") || name.contains("saneamento") || name.contains("ambiental") || name.contains("residuo") -> androidx.compose.material.icons.Icons.Outlined.Recycling
        words.contains("gtec") || name.contains("geotecnologias") || name.contains("topografia") -> androidx.compose.material.icons.Icons.Outlined.Map
        words.contains("irdr") || name.contains("irrigacao") || name.contains("drenagem") -> androidx.compose.material.icons.Icons.Outlined.WaterDrop
        words.contains("mapd") || name.contains("agrimplementos") || name.contains("maquinas") || name.contains("agricultura") || name.contains("agro") -> androidx.compose.material.icons.Icons.Outlined.Agriculture
        words.contains("meca") || name.contains("mecanizacao") || name.contains("conservacao do solo") || name.contains("mecanica") || name.contains("manutencao") || name.contains("usinagem") || name.contains("motores") -> androidx.compose.material.icons.Icons.Outlined.Build
        words.contains("osppmr") || name.contains("sistemas de producao") || name.contains("manejo de recursos") -> androidx.compose.material.icons.Icons.Outlined.AccountTree
        words.contains("prag") || name.contains("fitossanidade") || name.contains("pragas") -> androidx.compose.material.icons.Icons.Outlined.BugReport
        words.contains("spa") || name.contains("sistema de producao animal") || name.contains("producao animal") -> androidx.compose.material.icons.Icons.Outlined.CrueltyFree
        words.contains("ssag") || name.contains("saude e seguranca") || name.contains("seguranca na agropecuaria") || name.contains("sistemas agropecuarios") -> androidx.compose.material.icons.Icons.Outlined.VerifiedUser
        name.contains("ti agp") || words.contains("tia") || name.contains("tecnologia da informacao") -> androidx.compose.material.icons.Icons.Outlined.Memory
        words.contains("tpcc") || words.contains("tcc") || name.contains("praticas cientifico") || name.contains("trabalho de conclusao") || name.contains("estagio") || name.contains("pesquisa") || name.contains("seminario") || name.contains("metodologia") -> androidx.compose.material.icons.Icons.Outlined.School
        words.contains("zoo") || name.contains("zootecnia") || name.contains("pecuaria") -> androidx.compose.material.icons.Icons.Outlined.Pets
        name.contains("cana") || name.contains("acucar") -> androidx.compose.material.icons.Icons.Outlined.Grass
        name.contains("solo") || name.contains("plantas") -> androidx.compose.material.icons.Icons.Outlined.Park

        // AGROINDUSTRIA
        words.contains("afqa") || name.contains("analise fisico-quimica") -> androidx.compose.material.icons.Icons.Outlined.Analytics
        words.contains("agd") || name.contains("agroindustria") -> androidx.compose.material.icons.Icons.Outlined.PrecisionManufacturing
        words.contains("carn") || words.contains("tdqi") || name.contains("tecnologia de carnes") || name.contains("tecnologia de queijos") -> androidx.compose.material.icons.Icons.Outlined.SetMeal
        name.contains("emb alim") || name.contains("embalagens") -> androidx.compose.material.icons.Icons.Outlined.TakeoutDining
        words.contains("gra") || name.contains("graos") || name.contains("raizes") || name.contains("tuberculos") -> androidx.compose.material.icons.Icons.Outlined.Grain
        words.contains("hcqa") || name.contains("higiene") || name.contains("qualidade") -> androidx.compose.material.icons.Icons.Outlined.CleanHands
        name.contains("micro alim") || name.contains("microb lege") || name.contains("microbiologia") -> androidx.compose.material.icons.Icons.Outlined.Coronavirus
        words.contains("pca") || name.contains("processamento de carnes") || name.contains("origem animal") -> androidx.compose.material.icons.Icons.Outlined.EggAlt
        words.contains("pesc") || name.contains("pescados") -> androidx.compose.material.icons.Icons.Outlined.Sailing
        words.contains("pta") || name.contains("principios de tecnologia") -> androidx.compose.material.icons.Icons.Outlined.Blender
        name.contains("sist resf") || name.contains("sistemas de resfriamento") || name.contains("refrigeracao") -> androidx.compose.material.icons.Icons.Outlined.AcUnit
        words.contains("tcad") || name.contains("carnes e derivados") -> androidx.compose.material.icons.Icons.Outlined.LunchDining
        name.contains("tec beb") || name.contains("tecnologia de bebidas") || name.contains("bebidas") -> androidx.compose.material.icons.Icons.Outlined.WineBar
        words.contains("tlat") || words.contains("tld") || name.contains("tecnologia de leites") || name.contains("leite") || name.contains("laticinios") || name.contains("derivados") -> androidx.compose.material.icons.Icons.Outlined.Icecream
        words.contains("topa") || name.contains("produtos de origem animal") || name.contains("bromatologia") || name.contains("alimento") || name.contains("nutricao") || name.contains("gastronomia") || name.contains("culinaria") -> androidx.compose.material.icons.Icons.Outlined.Restaurant

        // GERAIS/HUMANAS
        words.contains("edfi") || name.contains("educacao fisica") || name.contains("ed fisica") || name.contains("ed. fisica") -> androidx.compose.material.icons.Icons.Outlined.FitnessCenter
        words.contains("empr") || name.contains("empreendedorismo") -> androidx.compose.material.icons.Icons.Outlined.Lightbulb
        words.contains("filo") || name.contains("filosofia") || name.contains("etica") -> androidx.compose.material.icons.Icons.Outlined.Psychology
        words.contains("fisc") || name.contains("fisica") -> androidx.compose.material.icons.Icons.Outlined.RocketLaunch
        words.contains("geog") || name.contains("geografia") || name.contains("geopolitica") -> androidx.compose.material.icons.Icons.Outlined.Public
        words.contains("gest") || name.contains("gest org") || name.contains("gestao") || name.contains("administracao") || name.contains("marketing") -> androidx.compose.material.icons.Icons.Outlined.Groups
        words.contains("gost") || name.contains("gestao e organizacao") -> androidx.compose.material.icons.Icons.Outlined.Engineering
        words.contains("hist") || name.contains("historia") -> androidx.compose.material.icons.Icons.Outlined.HistoryEdu
        words.contains("info") || name.contains("informatica") || name.contains("programacao") || name.contains("algoritmo") || name.contains("computador") || name.contains("software") || name.contains("web") || name.contains("banco de dados") || name.contains("rede") || name.contains("sistemas") || name.contains("desenvolvimento") -> androidx.compose.material.icons.Icons.Outlined.Terminal
        name.contains("int econ") || name.contains("economia") || name.contains("contabilidade") || name.contains("financas") -> androidx.compose.material.icons.Icons.Outlined.AttachMoney
        words.contains("lies") || name.contains("linguagem e sociedade") -> androidx.compose.material.icons.Icons.Outlined.Forum
        words.contains("lipo") || words.contains("ling") || name.contains("portugues") || name.contains("lingua portuguesa") || name.contains("redacao") || name.contains("literatura") || name.contains("linguagens") -> androidx.compose.material.icons.Icons.Outlined.MenuBook
        words.contains("mate") || name.contains("matematica") || name.contains("estatistica") || name.contains("calculo") || name.contains("geometria") || name.contains("algebra") -> androidx.compose.material.icons.Icons.Outlined.Calculate
        words.contains("quim") || name.contains("quimica") || name.contains("bioquimica") -> androidx.compose.material.icons.Icons.Outlined.Science
        words.contains("rht") || name.contains("relacoes humanas") -> androidx.compose.material.icons.Icons.Outlined.Diversity3
        words.contains("sect") || words.contains("segt") || name.contains("seguranca do trabalho") -> androidx.compose.material.icons.Icons.Outlined.Shield
        words.contains("soci") || name.contains("sociologia") || name.contains("cidadania") -> androidx.compose.material.icons.Icons.Outlined.Diversity1

        // GENERIC FALLBACKS & MISSING ORIGINALS
        name.contains("biologia") || name.contains("natureza") || name.contains("genetica") -> androidx.compose.material.icons.Icons.Outlined.Biotech
        name.contains("arte") || name.contains("musica") || name.contains("teatro") || name.contains("danca") || name.contains("desenho") -> androidx.compose.material.icons.Icons.Outlined.Palette
        name.contains("ingles") || name.contains("espanhol") || name.contains("libras") || name.contains("lingua estrangeira") || name.contains("frances") -> androidx.compose.material.icons.Icons.Outlined.Language
        name.contains("direito") || name.contains("legislacao") || name.contains("normas") -> androidx.compose.material.icons.Icons.Outlined.Gavel
        name.contains("eletro") || name.contains("eletrica") || name.contains("circuito") || name.contains("eletronica") -> androidx.compose.material.icons.Icons.Outlined.ElectricBolt
        name.contains("saude") || name.contains("enfermagem") || name.contains("primeiros socorros") -> androidx.compose.material.icons.Icons.Outlined.MedicalServices
        name.contains("projeto") || name.contains("assignment") -> androidx.compose.material.icons.Icons.Outlined.Assignment
        name.contains("construcao") || name.contains("edifica") || name.contains("arquitetura") -> androidx.compose.material.icons.Icons.Outlined.HomeRepairService

        else -> androidx.compose.material.icons.Icons.Outlined.Folder
    }
}
