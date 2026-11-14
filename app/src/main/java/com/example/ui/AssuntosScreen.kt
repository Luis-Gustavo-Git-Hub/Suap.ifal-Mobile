package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Subject
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private val WHITESPACE_REGEX = Regex("\\s+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssuntosScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onMaterialsClick: (String, String) -> Unit,
    onHistoryClick: (String, String) -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
        
    val filteredSubjects = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) {
            subjects
        } else {
            val query = searchQuery.trim().lowercase()
            subjects.filter { 
                it.name.orEmpty().lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Materiais e Aulas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${subjects.size} disciplinas ativas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar por disciplina...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Nenhuma disciplina encontrada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tente buscar por outro termo ou limpe o campo de busca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredSubjects,
                        key = { it.name.orEmpty() },
                        contentType = { "subject_card" }
                    ) { subject ->
                        val urlToUse = remember(subject) {
                            if ((subject.url ?: "").isNotEmpty()) subject.url.orEmpty() else subject.detailUrl.orEmpty()
                        }
                        AssuntoSubjectCard(
                            subject = subject, 
                            onMaterialsClick = { onMaterialsClick(subject.name.orEmpty(), urlToUse) },
                            onHistoryClick = { onHistoryClick(subject.name.orEmpty(), urlToUse) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSubjectBadge(
    subjectName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ShapeDefaults.Medium,
    customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    customGradient: List<Color>? = null
) {
    val clean = remember(subjectName) { subjectName.trim().replace(WHITESPACE_REGEX, " ") }
    val gradientColors = remember(clean, customGradient) { customGradient ?: getSubjectGradient(clean) }
    val icon = remember(clean, customIcon) { customIcon ?: getSubjectIcon(clean) }

    val bgBrush = remember(gradientColors) {
        Brush.linearGradient(colors = gradientColors)
    }
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = subjectName,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AssuntoSubjectCard(subject: Subject, onMaterialsClick: () -> Unit, onHistoryClick: () -> Unit) {
    val cleanName = remember(subject.name) { 
        subject.name.orEmpty().trim().replace(WHITESPACE_REGEX, " ") 
    }
    val gradientColors = remember(cleanName) { getSubjectGradient(cleanName) }
    val primaryColor = gradientColors.first()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedSubjectBadge(
                    subjectName = cleanName,
                    size = 48.dp,
                    iconSize = 24.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Bento style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onMaterialsClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download, 
                        contentDescription = null, 
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Materiais",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                FilledTonalButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote, 
                        contentDescription = null, 
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aulas",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
private val subjectIconCache = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.ui.graphics.vector.ImageVector>()
private val subjectColorCache = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.ui.graphics.Color>()
private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val WORDS_SPLIT_REGEX = Regex("\\W+")

fun getSubjectIcon(subjectName: String): androidx.compose.ui.graphics.vector.ImageVector {
    if (subjectName.isBlank()) return androidx.compose.material.icons.Icons.Default.Folder
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
        words.contains("dtcr") || name.contains("desenho tecnico") || name.contains("construcoes rurais") -> androidx.compose.material.icons.Icons.Default.Architecture
        words.contains("extdr") || name.contains("extensao rural") -> androidx.compose.material.icons.Icons.Default.NaturePeople
        words.contains("geag") || name.contains("geociencias") || name.contains("agroecologia") -> androidx.compose.material.icons.Icons.Default.Landscape
        words.contains("gepr") || name.contains("gestao da producao") -> androidx.compose.material.icons.Icons.Default.Factory
        words.contains("gerr") || name.contains("recursos renovaveis") || name.contains("meio ambiente") || name.contains("ecologia") || name.contains("saneamento") || name.contains("ambiental") || name.contains("residuo") -> androidx.compose.material.icons.Icons.Default.Recycling
        words.contains("gtec") || name.contains("geotecnologias") || name.contains("topografia") -> androidx.compose.material.icons.Icons.Default.Map
        words.contains("irdr") || name.contains("irrigacao") || name.contains("drenagem") -> androidx.compose.material.icons.Icons.Default.WaterDrop
        words.contains("mapd") || name.contains("agrimplementos") || name.contains("maquinas") || name.contains("agricultura") || name.contains("agro") -> androidx.compose.material.icons.Icons.Default.Agriculture
        words.contains("meca") || name.contains("mecanizacao") || name.contains("conservacao do solo") || name.contains("mecanica") || name.contains("manutencao") || name.contains("usinagem") || name.contains("motores") -> androidx.compose.material.icons.Icons.Default.Build
        words.contains("osppmr") || name.contains("sistemas de producao") || name.contains("manejo de recursos") -> androidx.compose.material.icons.Icons.Default.AccountTree
        words.contains("prag") || name.contains("fitossanidade") || name.contains("pragas") -> androidx.compose.material.icons.Icons.Default.BugReport
        words.contains("spa") || name.contains("sistema de producao animal") || name.contains("producao animal") -> androidx.compose.material.icons.Icons.Default.CrueltyFree
        words.contains("ssag") || name.contains("saude e seguranca") || name.contains("seguranca na agropecuaria") || name.contains("sistemas agropecuarios") -> androidx.compose.material.icons.Icons.Default.VerifiedUser
        name.contains("ti agp") || words.contains("tia") || name.contains("tecnologia da informacao") -> androidx.compose.material.icons.Icons.Default.Memory
        words.contains("tpcc") || words.contains("tcc") || name.contains("praticas cientifico") || name.contains("trabalho de conclusao") || name.contains("estagio") || name.contains("pesquisa") || name.contains("seminario") || name.contains("metodologia") -> androidx.compose.material.icons.Icons.Default.School
        words.contains("zoo") || name.contains("zootecnia") || name.contains("pecuaria") -> androidx.compose.material.icons.Icons.Default.Pets
        name.contains("cana") || name.contains("acucar") -> androidx.compose.material.icons.Icons.Default.Grass
        name.contains("solo") || name.contains("plantas") -> androidx.compose.material.icons.Icons.Default.Park

        // AGROINDUSTRIA
        words.contains("afqa") || name.contains("analise fisico-quimica") -> androidx.compose.material.icons.Icons.Default.Analytics
        words.contains("agd") || name.contains("agroindustria") -> androidx.compose.material.icons.Icons.Default.PrecisionManufacturing
        words.contains("carn") || words.contains("tdqi") || name.contains("tecnologia de carnes") || name.contains("tecnologia de queijos") -> androidx.compose.material.icons.Icons.Default.SetMeal
        name.contains("emb alim") || name.contains("embalagens") -> androidx.compose.material.icons.Icons.Default.TakeoutDining
        words.contains("gra") || name.contains("graos") || name.contains("raizes") || name.contains("tuberculos") -> androidx.compose.material.icons.Icons.Default.Grain
        words.contains("hcqa") || name.contains("higiene") || name.contains("qualidade") -> androidx.compose.material.icons.Icons.Default.CleanHands
        name.contains("micro alim") || name.contains("microb lege") || name.contains("microbiologia") -> androidx.compose.material.icons.Icons.Default.Coronavirus
        words.contains("pca") || name.contains("processamento de carnes") || name.contains("origem animal") -> androidx.compose.material.icons.Icons.Default.EggAlt
        words.contains("pesc") || name.contains("pescados") -> androidx.compose.material.icons.Icons.Default.Sailing
        words.contains("pta") || name.contains("principios de tecnologia") -> androidx.compose.material.icons.Icons.Default.Blender
        name.contains("sist resf") || name.contains("sistemas de resfriamento") || name.contains("refrigeracao") -> androidx.compose.material.icons.Icons.Default.AcUnit
        words.contains("tcad") || name.contains("carnes e derivados") -> androidx.compose.material.icons.Icons.Default.LunchDining
        name.contains("tec beb") || name.contains("tecnologia de bebidas") || name.contains("bebidas") -> androidx.compose.material.icons.Icons.Default.WineBar
        words.contains("tlat") || words.contains("tld") || name.contains("tecnologia de leites") || name.contains("leite") || name.contains("laticinios") || name.contains("derivados") -> androidx.compose.material.icons.Icons.Default.Icecream
        words.contains("topa") || name.contains("produtos de origem animal") || name.contains("bromatologia") || name.contains("alimento") || name.contains("nutricao") || name.contains("gastronomia") || name.contains("culinaria") -> androidx.compose.material.icons.Icons.Default.Restaurant

        // GERAIS/HUMANAS
        words.contains("edfi") || name.contains("educacao fisica") || name.contains("ed fisica") || name.contains("ed. fisica") -> androidx.compose.material.icons.Icons.Default.FitnessCenter
        words.contains("empr") || name.contains("empreendedorismo") -> androidx.compose.material.icons.Icons.Default.Lightbulb
        words.contains("filo") || name.contains("filosofia") || name.contains("etica") -> androidx.compose.material.icons.Icons.Default.Psychology
        words.contains("fisc") || name.contains("fisica") -> androidx.compose.material.icons.Icons.Default.RocketLaunch
        words.contains("geog") || name.contains("geografia") || name.contains("geopolitica") -> androidx.compose.material.icons.Icons.Default.Public
        words.contains("gest") || name.contains("gest org") || name.contains("gestao") || name.contains("administracao") || name.contains("marketing") -> androidx.compose.material.icons.Icons.Default.Groups
        words.contains("gost") || name.contains("gestao e organizacao") -> androidx.compose.material.icons.Icons.Default.Engineering
        words.contains("hist") || name.contains("historia") -> androidx.compose.material.icons.Icons.Default.HistoryEdu
        words.contains("info") || name.contains("informatica") || name.contains("programacao") || name.contains("algoritmo") || name.contains("computador") || name.contains("software") || name.contains("web") || name.contains("banco de dados") || name.contains("rede") || name.contains("sistemas") || name.contains("desenvolvimento") -> androidx.compose.material.icons.Icons.Default.Terminal
        name.contains("int econ") || name.contains("economia") || name.contains("contabilidade") || name.contains("financas") -> androidx.compose.material.icons.Icons.Default.AttachMoney
        words.contains("lies") || name.contains("linguagem e sociedade") -> androidx.compose.material.icons.Icons.Default.Forum
        words.contains("lipo") || words.contains("ling") || name.contains("portugues") || name.contains("lingua portuguesa") || name.contains("redacao") || name.contains("literatura") || name.contains("linguagens") -> androidx.compose.material.icons.Icons.Default.MenuBook
        words.contains("mate") || name.contains("matematica") || name.contains("estatistica") || name.contains("calculo") || name.contains("geometria") || name.contains("algebra") -> androidx.compose.material.icons.Icons.Default.Calculate
        words.contains("quim") || name.contains("quimica") || name.contains("bioquimica") -> androidx.compose.material.icons.Icons.Default.Science
        words.contains("rht") || name.contains("relacoes humanas") -> androidx.compose.material.icons.Icons.Default.Diversity3
        words.contains("sect") || words.contains("segt") || name.contains("seguranca do trabalho") -> androidx.compose.material.icons.Icons.Default.Shield
        words.contains("soci") || name.contains("sociologia") || name.contains("cidadania") -> androidx.compose.material.icons.Icons.Default.Diversity1
        
        // GENERIC FALLBACKS & MISSING ORIGINALS
        name.contains("biologia") || name.contains("natureza") || name.contains("genetica") -> androidx.compose.material.icons.Icons.Default.Biotech
        name.contains("arte") || name.contains("musica") || name.contains("teatro") || name.contains("danca") || name.contains("desenho") -> androidx.compose.material.icons.Icons.Default.Palette
        name.contains("ingles") || name.contains("espanhol") || name.contains("libras") || name.contains("lingua estrangeira") || name.contains("frances") -> androidx.compose.material.icons.Icons.Default.Language
        name.contains("direito") || name.contains("legislacao") || name.contains("normas") -> androidx.compose.material.icons.Icons.Default.Gavel
        name.contains("eletro") || name.contains("eletrica") || name.contains("circuito") || name.contains("eletronica") -> androidx.compose.material.icons.Icons.Default.ElectricBolt
        name.contains("saude") || name.contains("enfermagem") || name.contains("primeiros socorros") -> androidx.compose.material.icons.Icons.Default.MedicalServices
        name.contains("projeto") || name.contains("assignment") -> androidx.compose.material.icons.Icons.Default.Assignment
        name.contains("construcao") || name.contains("edifica") || name.contains("arquitetura") -> androidx.compose.material.icons.Icons.Default.HomeRepairService
        
        else -> androidx.compose.material.icons.Icons.Default.Folder
    }
}

fun getSubjectColor(subjectName: String, isDark: Boolean): androidx.compose.ui.graphics.Color {
    if (subjectName.isBlank()) return androidx.compose.ui.graphics.Color(0xFF757575)
    return subjectColorCache.getOrPut(subjectName) {
        computeSubjectColor(subjectName)
    }
}

private fun computeSubjectColor(subjectName: String): androidx.compose.ui.graphics.Color {
    val name = java.text.Normalizer.normalize(subjectName, java.text.Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase()
        
    val words = name.split(WORDS_SPLIT_REGEX)
        
    return when {
        // AGROPECUARIA
        words.contains("dtcr") || name.contains("desenho tecnico") || name.contains("construcoes rurais") -> androidx.compose.ui.graphics.Color(0xFFE65100) // Deep Orange
        words.contains("extdr") || name.contains("extensao rural") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        words.contains("geag") || name.contains("geociencias") || name.contains("agroecologia") -> androidx.compose.ui.graphics.Color(0xFF689F38) // Light Green
        words.contains("gepr") || name.contains("gestao da producao") -> androidx.compose.ui.graphics.Color(0xFF455A64) // Blue Grey
        words.contains("gerr") || name.contains("recursos renovaveis") || name.contains("meio ambiente") || name.contains("ecologia") || name.contains("saneamento") || name.contains("ambiental") || name.contains("residuo") -> androidx.compose.ui.graphics.Color(0xFF388E3C) // Green
        words.contains("gtec") || name.contains("geotecnologias") || name.contains("topografia") -> androidx.compose.ui.graphics.Color(0xFF8D6E63) // Brown
        words.contains("irdr") || name.contains("irrigacao") || name.contains("drenagem") -> androidx.compose.ui.graphics.Color(0xFF0277BD) // Light Blue
        words.contains("mapd") || name.contains("agrimplementos") || name.contains("maquinas") || name.contains("agricultura") || name.contains("agro") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        words.contains("meca") || name.contains("mecanizacao") || name.contains("conservacao do solo") || name.contains("mecanica") || name.contains("manutencao") || name.contains("usinagem") || name.contains("motores") -> androidx.compose.ui.graphics.Color(0xFF455A64) // Blue Grey
        words.contains("osppmr") || name.contains("sistemas de producao") || name.contains("manejo de recursos") -> androidx.compose.ui.graphics.Color(0xFF00796B) // Teal
        words.contains("prag") || name.contains("fitossanidade") || name.contains("pragas") -> androidx.compose.ui.graphics.Color(0xFFAFB42B) // Lime
        words.contains("spa") || name.contains("sistema de producao animal") || name.contains("producao animal") -> androidx.compose.ui.graphics.Color(0xFF795548) // Brown
        words.contains("ssag") || name.contains("saude e seguranca") || name.contains("seguranca na agropecuaria") || name.contains("sistemas agropecuarios") -> androidx.compose.ui.graphics.Color(0xFF0097A7) // Cyan
        name.contains("ti agp") || words.contains("tia") || name.contains("tecnologia da informacao") -> androidx.compose.ui.graphics.Color(0xFF0288D1) // Cyan Blue
        words.contains("tpcc") || words.contains("tcc") || name.contains("praticas cientifico") || name.contains("trabalho de conclusao") || name.contains("estagio") || name.contains("pesquisa") || name.contains("seminario") || name.contains("metodologia") -> androidx.compose.ui.graphics.Color(0xFF5C6BC0) // Indigo
        words.contains("zoo") || name.contains("zootecnia") || name.contains("pecuaria") -> androidx.compose.ui.graphics.Color(0xFF8D6E63) // Brown
        name.contains("cana") || name.contains("acucar") -> androidx.compose.ui.graphics.Color(0xFF7CB342) // Light Green
        name.contains("solo") || name.contains("plantas") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange

        // AGROINDUSTRIA
        words.contains("afqa") || name.contains("analise fisico-quimica") -> androidx.compose.ui.graphics.Color(0xFFC2185B) // Pink
        words.contains("agd") || name.contains("agroindustria") -> androidx.compose.ui.graphics.Color(0xFF00897B) // Teal
        words.contains("carn") || words.contains("tdqi") || name.contains("tecnologia de carnes") || name.contains("tecnologia de queijos") -> androidx.compose.ui.graphics.Color(0xFFD84315) // Deep Orange
        name.contains("emb alim") || name.contains("embalagens") -> androidx.compose.ui.graphics.Color(0xFFFBC02D) // Yellow
        words.contains("gra") || name.contains("graos") || name.contains("raizes") || name.contains("tuberculos") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        words.contains("hcqa") || name.contains("higiene") || name.contains("qualidade") -> androidx.compose.ui.graphics.Color(0xFF00796B) // Teal
        name.contains("micro alim") || name.contains("microb lege") || name.contains("microbiologia") -> androidx.compose.ui.graphics.Color(0xFF388E3C) // Green
        words.contains("pca") || name.contains("processamento de carnes") || name.contains("origem animal") -> androidx.compose.ui.graphics.Color(0xFFE64A19) // Deep Orange darker
        words.contains("pesc") || name.contains("pescados") -> androidx.compose.ui.graphics.Color(0xFF0277BD) // Blue
        words.contains("pta") || name.contains("principios de tecnologia") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        name.contains("sist resf") || name.contains("sistemas de resfriamento") || name.contains("refrigeracao") -> androidx.compose.ui.graphics.Color(0xFF00BCD4) // Cyan
        words.contains("tcad") || name.contains("carnes e derivados") -> androidx.compose.ui.graphics.Color(0xFFD84315) // Deep Orange
        name.contains("tec beb") || name.contains("tecnologia de bebidas") || name.contains("bebidas") -> androidx.compose.ui.graphics.Color(0xFF7B1FA2) // Purple
        words.contains("tlat") || words.contains("tld") || name.contains("tecnologia de leites") || name.contains("leite") || name.contains("laticinios") || name.contains("derivados") -> androidx.compose.ui.graphics.Color(0xFF03A9F4) // Light Blue
        words.contains("topa") || name.contains("produtos de origem animal") || name.contains("bromatologia") || name.contains("alimento") || name.contains("nutricao") || name.contains("gastronomia") || name.contains("culinaria") -> androidx.compose.ui.graphics.Color(0xFFE64A19) // Deep Orange

        // GERAIS/HUMANAS
        words.contains("edfi") || name.contains("educacao fisica") || name.contains("ed fisica") || name.contains("ed. fisica") -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        words.contains("empr") || name.contains("empreendedorismo") -> androidx.compose.ui.graphics.Color(0xFFFBC02D) // Yellow
        words.contains("filo") || name.contains("filosofia") || name.contains("etica") -> androidx.compose.ui.graphics.Color(0xFF512DA8) // Deep Purple
        words.contains("fisc") || name.contains("fisica") -> androidx.compose.ui.graphics.Color(0xFF0097A7) // Cyan
        words.contains("geog") || name.contains("geografia") || name.contains("geopolitica") -> androidx.compose.ui.graphics.Color(0xFF689F38) // Light Green
        words.contains("gest") || name.contains("gest org") || name.contains("gestao") || name.contains("administracao") || name.contains("marketing") -> androidx.compose.ui.graphics.Color(0xFF00897B) // Teal
        words.contains("gost") || name.contains("gestao e organizacao") -> androidx.compose.ui.graphics.Color(0xFF455A64) // Blue Grey
        words.contains("hist") || name.contains("historia") -> androidx.compose.ui.graphics.Color(0xFF6D4C41) // Brown
        words.contains("info") || name.contains("informatica") || name.contains("programacao") || name.contains("algoritmo") || name.contains("computador") || name.contains("software") || name.contains("web") || name.contains("banco de dados") || name.contains("rede") || name.contains("sistemas") || name.contains("desenvolvimento") -> androidx.compose.ui.graphics.Color(0xFF0288D1) // Cyan Blue
        name.contains("int econ") || name.contains("economia") || name.contains("contabilidade") || name.contains("financas") -> androidx.compose.ui.graphics.Color(0xFF388E3C) // Green
        words.contains("lies") || name.contains("linguagem e sociedade") -> androidx.compose.ui.graphics.Color(0xFF7B1FA2) // Purple
        words.contains("lipo") || words.contains("ling") || name.contains("portugues") || name.contains("lingua portuguesa") || name.contains("redacao") || name.contains("literatura") || name.contains("linguagens") -> androidx.compose.ui.graphics.Color(0xFFD32F2F) // Red
        words.contains("mate") || name.contains("matematica") || name.contains("estatistica") || name.contains("calculo") || name.contains("geometria") || name.contains("algebra") -> androidx.compose.ui.graphics.Color(0xFF1976D2) // Blue
        words.contains("quim") || name.contains("quimica") || name.contains("bioquimica") -> androidx.compose.ui.graphics.Color(0xFF303F9F) // Indigo
        words.contains("rht") || name.contains("relacoes humanas") -> androidx.compose.ui.graphics.Color(0xFF00897B) // Teal
        words.contains("sect") || words.contains("segt") || name.contains("seguranca do trabalho") -> androidx.compose.ui.graphics.Color(0xFF00796B) // Teal
        words.contains("soci") || name.contains("sociologia") || name.contains("cidadania") -> androidx.compose.ui.graphics.Color(0xFFFBC02D) // Yellow
        
        // GENERIC FALLBACKS & MISSING ORIGINALS
        name.contains("biologia") || name.contains("natureza") || name.contains("genetica") -> androidx.compose.ui.graphics.Color(0xFF388E3C) // Green
        name.contains("arte") || name.contains("musica") || name.contains("teatro") || name.contains("danca") || name.contains("desenho") -> androidx.compose.ui.graphics.Color(0xFFC2185B) // Pink
        name.contains("ingles") || name.contains("espanhol") || name.contains("libras") || name.contains("lingua estrangeira") || name.contains("frances") -> androidx.compose.ui.graphics.Color(0xFF7B1FA2) // Purple
        name.contains("direito") || name.contains("legislacao") || name.contains("normas") -> androidx.compose.ui.graphics.Color(0xFF4E342E) // Dark Brown
        name.contains("eletro") || name.contains("eletrica") || name.contains("circuito") || name.contains("eletronica") -> androidx.compose.ui.graphics.Color(0xFFFF8F00) // Amber
        name.contains("saude") || name.contains("enfermagem") || name.contains("primeiros socorros") -> androidx.compose.ui.graphics.Color(0xFFE91E63) // Pink / Red
        name.contains("projeto") || name.contains("assignment") -> androidx.compose.ui.graphics.Color(0xFF5C6BC0) // Indigo
        name.contains("construcao") || name.contains("edifica") || name.contains("arquitetura") -> androidx.compose.ui.graphics.Color(0xFFE65100) // Deep Orange
        
        else -> androidx.compose.ui.graphics.Color(0xFF757575) // Grey
    }
}

private val subjectGradientCache = java.util.concurrent.ConcurrentHashMap<String, List<androidx.compose.ui.graphics.Color>>()

fun getSubjectGradient(subjectName: String): List<androidx.compose.ui.graphics.Color> {
    if (subjectName.isBlank()) return listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF4F46E5), androidx.compose.ui.graphics.Color(0xFF4338CA))
    return subjectGradientCache.getOrPut(subjectName) {
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
