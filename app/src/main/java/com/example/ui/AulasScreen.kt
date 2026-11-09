package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.CancellationException
import com.example.util.SuapParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AulasScreen(
    subjectName: String,
    url: String,
    cookie: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var aulas by remember { mutableStateOf<List<AulaHistory>>(emptyList()) }
    var materials by remember { mutableStateOf<List<Material>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMaterials by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var onlyAbsences by rememberSaveable { mutableStateOf(false) }
    var showMaterialsSheet by rememberSaveable { mutableStateOf(false) }

    fun buildUrlWithTab(rawUrl: String, tabName: String): String {
        val base = if (rawUrl.startsWith("http")) rawUrl else "https://suap.ifal.edu.br${if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"}"
        val uri = Uri.parse(base)
        val builder = uri.buildUpon()
        builder.clearQuery()
        uri.queryParameterNames.forEach { param ->
            if (param != "tab") {
                uri.getQueryParameters(param).forEach { value ->
                    builder.appendQueryParameter(param, value)
                }
            }
        }
        builder.appendQueryParameter("tab", tabName)
        return builder.build().toString()
    }

    suspend fun loadData() {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    val cachedMaterials = MaterialOfflineCache.load(context, url)
                    if (cachedMaterials.isNotEmpty()) materials = cachedMaterials
                    isLoading = true
                    isLoadingMaterials = true
                    errorMessage = null
                }

                val suapAuthRepository = com.example.network.SuapAuthRepository.getInstance(context)

                // Extract discipline numeric ID if present (e.g. 2236)
                val idMatch = Regex("""/(\d+)/?""").find(url)?.groupValues?.get(1)

                val allParsedAulas = mutableListOf<AulaHistory>()
                val allParsedMaterials = mutableListOf<Material>()
                var aulasRequestFailed = false

                // Primary URL to fetch aulas
                val finalAulasUrl = buildUrlWithTab(url, "aulas")
                val aulasReq = Request.Builder().url(finalAulasUrl)
                val aulasResult = suapAuthRepository.executeRequest(aulasReq)

                aulasResult.onSuccess { html ->
                    if (html.isNotEmpty()) {
                        allParsedAulas.addAll(SuapParser.parseAulas(html).getOrDefault(emptyList()))
                        allParsedMaterials.addAll(SuapParser.parseMaterials(html).getOrDefault(emptyList()))

                        // If empty aulas, check if there's a link to the real disciplina
                        val disciplinaHref = SuapParser.findDisciplinaUrl(html).getOrNull()
                        if (allParsedAulas.isEmpty() && !disciplinaHref.isNullOrEmpty()) {
                            val redirectUrl = buildUrlWithTab(disciplinaHref, "aulas")
                            val redirectResult = suapAuthRepository.executeRequest(Request.Builder().url(redirectUrl))
                            redirectResult.onSuccess { redirectHtml ->
                                if (redirectHtml.isNotEmpty()) {
                                    allParsedAulas.addAll(SuapParser.parseAulas(redirectHtml).getOrDefault(emptyList()))
                                    allParsedMaterials.addAll(SuapParser.parseMaterials(redirectHtml).getOrDefault(emptyList()))
                                }
                            }.onError { message, _ ->
                                aulasRequestFailed = true
                                com.example.util.AppLog.w("AulasScreen", "Erro ao buscar aulas na disciplina vinculada: $message")
                            }
                        }
                    }
                }.onError { msg, _ ->
                    aulasRequestFailed = true
                    com.example.util.AppLog.e("AulasScreen", "Erro ao carregar aulas: $msg")
                }

                // Explicitly query both possible materiais tab URLs: /edu/disciplina/$id/?tab=materiais and /edu/aluno_disciplina/$id/?tab=materiais
                val urlsToTryMaterials = mutableListOf<String>()
                if (!idMatch.isNullOrBlank()) {
                    urlsToTryMaterials.add("https://suap.ifal.edu.br/edu/disciplina/$idMatch/?tab=materiais")
                    urlsToTryMaterials.add("https://suap.ifal.edu.br/edu/aluno_disciplina/$idMatch/?tab=materiais")
                }
                urlsToTryMaterials.add(buildUrlWithTab(url, "materiais"))

                for (matUrl in urlsToTryMaterials.distinct()) {
                    try {
                        val matResult = suapAuthRepository.executeRequest(Request.Builder().url(matUrl))
                        matResult.onSuccess { matHtml ->
                            if (matHtml.isNotEmpty()) {
                                allParsedMaterials.addAll(SuapParser.parseMaterials(matHtml).getOrDefault(emptyList()))
                                if (allParsedAulas.isEmpty()) {
                                    allParsedAulas.addAll(SuapParser.parseAulas(matHtml).getOrDefault(emptyList()))
                                }
                            }
                        }.onError { message, _ ->
                            aulasRequestFailed = true
                            com.example.util.AppLog.w("AulasScreen", "Erro ao buscar dados offline da disciplina: $message")
                        }
                        if (allParsedMaterials.isNotEmpty()) break
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        com.example.util.AppLog.printStackTrace(e)
                    }
                }

                val finalAulas = allParsedAulas.distinctBy { "${it.data}_${it.conteudo}" }
                val finalMaterials = allParsedMaterials.distinctBy { it.url }
                if (finalMaterials.isNotEmpty()) MaterialOfflineCache.save(context, url, finalMaterials)
                val offlineAulas = if (finalAulas.isEmpty() && aulasRequestFailed) {
                    try {
                        AppDatabase.getDatabase(context).aulaHistoryDao().getAulasForSubject(url).map { it.toDomain() }
                    } catch (e: Exception) {
                        com.example.util.AppLog.w("AulasScreen", "Could not load offline class history", e)
                        emptyList()
                    }
                } else emptyList()

                // Save successfully loaded aulas to Room Database for Offline availability
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val dao = db.aulaHistoryDao()
                    if (finalAulas.isNotEmpty()) {
                        dao.deleteAulasForSubject(url)
                        dao.insertAulas(finalAulas.map { it.toEntity(url) })
                    }
                } catch (dbEx: Exception) {
                    com.example.util.AppLog.printStackTrace(dbEx)
                }

                withContext(Dispatchers.Main) {
                    aulas = finalAulas.ifEmpty { offlineAulas }
                    materials = finalMaterials.ifEmpty { MaterialOfflineCache.load(context, url) }
                    isLoading = false
                    isLoadingMaterials = false
                    if (finalAulas.isEmpty() && offlineAulas.isEmpty()) {
                        errorMessage = if (aulasRequestFailed) "Sem conexão. Ainda não há aulas salvas offline." else "Nenhuma aula registrada pelo professor ainda nesta disciplina."
                    } else if (offlineAulas.isNotEmpty()) {
                        com.example.util.AppSnackbarManager.show("Modo Offline: exibindo aulas salvas localmente")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Try fetching from Offline Database fallback
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val dao = db.aulaHistoryDao()
                    val cachedEntities = dao.getAulasForSubject(url)
                    if (cachedEntities.isNotEmpty()) {
                        val cachedAulas = cachedEntities.map { it.toDomain() }
                        withContext(Dispatchers.Main) {
                            aulas = cachedAulas
                            errorMessage = null
                            isLoading = false
                            isLoadingMaterials = false
                            com.example.util.AppSnackbarManager.show("Modo Offline: exibindo aulas salvas localmente")
                        }
                        return@withContext
                    }
                } catch (dbEx: Exception) {
                    com.example.util.AppLog.printStackTrace(dbEx)
                }

                withContext(Dispatchers.Main) {
                    errorMessage = "Erro de conexão ao carregar aulas: ${e.message}"
                    isLoading = false
                    isLoadingMaterials = false
                }
            }
        }
    }

    LaunchedEffect(url) {
        loadData()
    }

    // Expand/collapse standalone materials (show max 3 by default)
    var expandStandaloneMaterials by rememberSaveable { mutableStateOf(false) }

    // Precalculate aula -> matching materials map once to eliminate heavy recalculations during scroll
    val aulaMaterialsMap = remember(aulas, materials) {
        val map = mutableMapOf<AulaHistory, List<Material>>()
        aulas.forEach { aula ->
            val list = mutableListOf<Material>()
            // 1. Matched from subject's materials tab by date / title / keywords
            materials.filter { mat -> isDateMatching(aula, mat) }.forEach { mat ->
                if (list.none { it.url == mat.url }) {
                    list.add(mat)
                }
            }
            // 2. Direct attachment from aula if present (only if it's actually a download URL)
            if (aula.attachmentUrl.isNotBlank() && isFileDownloadUrl(aula.attachmentUrl)) {
                val title = aula.attachmentTitle.ifBlank { "Material de Aula" }
                if (list.none { it.url == aula.attachmentUrl }) {
                    list.add(Material(title = title, url = aula.attachmentUrl, date = aula.data))
                }
            }
            map[aula] = list
        }
        map
    }

    val matchedMaterialUrls = remember(aulaMaterialsMap) {
        val set = mutableSetOf<String>()
        aulaMaterialsMap.values.forEach { list ->
            list.forEach { set.add(it.url) }
        }
        set
    }

    val standaloneMaterials = remember(materials, matchedMaterialUrls, searchQuery) {
        materials.filter { mat ->
            !matchedMaterialUrls.contains(mat.url) &&
            (searchQuery.isBlank() || mat.title.contains(searchQuery, ignoreCase = true) || mat.date.contains(searchQuery, ignoreCase = true))
        }
    }

    val displayedStandaloneMaterials = remember(standaloneMaterials, expandStandaloneMaterials) {
        if (expandStandaloneMaterials) standaloneMaterials else standaloneMaterials.take(3)
    }

    // Filtered list
    val filteredAulas = remember(aulas, searchQuery, onlyAbsences) {
        aulas.filter { aula ->
            val matchesQuery = searchQuery.isBlank() ||
                aula.conteudo.contains(searchQuery, ignoreCase = true) ||
                aula.data.contains(searchQuery, ignoreCase = true) ||
                aula.professor.contains(searchQuery, ignoreCase = true)

            val matchesAbsence = if (onlyAbsences) {
                (aula.numFaltas.toIntOrNull() ?: 0) > 0
            } else {
                true
            }

            matchesQuery && matchesAbsence
        }
    }

    // Stats calculations
    val totalAulasMinistradas = remember(aulas) {
        aulas.sumOf { it.numAulas.toIntOrNull() ?: 1 }
    }
    val totalFaltas = remember(aulas) {
        aulas.sumOf { it.numFaltas.toIntOrNull() ?: 0 }
    }
    val mainProfessor = remember(aulas) {
        aulas.map { it.professor }.firstOrNull { it.isNotBlank() } ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedSubjectBadge(
                            subjectName = subjectName,
                            size = 36.dp,
                            iconSize = 19.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subjectName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aulas e Conteúdos Ministrados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { coroutineScope.launch { loadData() } },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            when {
                isLoading && aulas.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acessando aulas ministradas no SUAP...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        repeat(4) {
                            com.example.ui.components.AulaCardSkeleton()
                        }
                    }
                }
                errorMessage != null && aulas.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    loadData()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Bento Summary Cards Header
                        item {
                            AulasBentoHeader(
                                totalAulas = totalAulasMinistradas,
                                totalFaltas = totalFaltas,
                                professor = mainProfessor,
                                totalRegistros = aulas.size
                            )
                        }

                        // Search & Filter controls
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Pesquisar conteúdo ou data...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                                    },
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

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = !onlyAbsences,
                                        onClick = { onlyAbsences = false },
                                        label = { Text("Todas (${aulas.size})") },
                                        leadingIcon = if (!onlyAbsences) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )

                                    val countFaltas = aulas.count { (it.numFaltas.toIntOrNull() ?: 0) > 0 }
                                    FilterChip(
                                        selected = onlyAbsences,
                                        onClick = { onlyAbsences = true },
                                        label = { Text("Com faltas ($countFaltas)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        leadingIcon = if (onlyAbsences) {
                                            { Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )

                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Standalone Materials Section (materials not matched to a specific class date, or posted for general study)
                        if (standaloneMaterials.isNotEmpty() && !onlyAbsences) {
                            item(key = "header_standalone_materials") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Materiais e Arquivos Avulsos",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "Conteúdos postados fora das datas de aula",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.secondary
                                        ) {
                                            Text(
                                                text = "${standaloneMaterials.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(
                                items = displayedStandaloneMaterials,
                                key = { it.url.ifBlank { "${it.title}_${it.date}" } },
                                contentType = { "standalone_material" }
                            ) { mat ->
                                StandaloneMaterialCard(
                                    material = mat,
                                    onDownload = {
                                        downloadSuapFile(context, mat.title, mat.url)
                                    }
                                )
                            }

                            if (standaloneMaterials.size > 3) {
                                item(key = "btn_expand_standalone_materials") {
                                    OutlinedButton(
                                        onClick = { expandStandaloneMaterials = !expandStandaloneMaterials },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                                            contentColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (expandStandaloneMaterials) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (expandStandaloneMaterials) "Mostrar apenas os mais recentes" else "Ver todos os ${standaloneMaterials.size} materiais (+${standaloneMaterials.size - 3})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            if (filteredAulas.isNotEmpty()) {
                                item(key = "header_aulas_diario") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Diário de Aulas Ministradas",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredAulas.isEmpty() && standaloneMaterials.isEmpty()) {
                            item(key = "empty_state") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (onlyAbsences) "Parabéns! Você não possui faltas nesta disciplina." else "Nenhuma aula encontrada para a busca.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredAulas,
                                key = { "${it.data}_${it.etapa}_${it.conteudo.hashCode()}_${it.professor}" },
                                contentType = { "aula_card" }
                            ) { aula ->
                                val matchingMaterials = aulaMaterialsMap[aula] ?: emptyList()

                                AulaCard(
                                    aula = aula,
                                    matchingMaterials = matchingMaterials,
                                    onDownloadAttachment = { url, title ->
                                        downloadSuapFile(context, title, url)
                                    },
                                    onOpenLink = { rawUrl ->
                                        openInBrowser(context, rawUrl)
                                    },
                                    onCopyContent = { content ->
                                        clipboardManager.setText(AnnotatedString(content))
                                        com.example.util.AppSnackbarManager.show("Conteúdo copiado!")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Subject Materials (Files/Slides uploaded to the subject)
    if (showMaterialsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMaterialsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Materiais de Aula",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (materials.isEmpty()) "Nenhum arquivo anexado" else "${materials.size} arquivo(s) disponível(is)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (materials.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                materials.forEach { mat ->
                                    downloadSuapFile(context, mat.title, mat.url)
                                }
                                com.example.util.AppSnackbarManager.show("Baixando ${materials.size} arquivos...")
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baixar Todos", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingMaterials && materials.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            com.example.ui.components.AulaCardSkeleton()
                        }
                    }
                } else if (materials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "O professor não postou materiais anexos nesta disciplina.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = materials,
                            key = { it.url.ifBlank { "${it.title}_${it.date}" } },
                            contentType = { "material_item" }
                        ) { mat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getMaterialIcon(mat.url, mat.title),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mat.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${getMaterialTypeDescription(mat.url, mat.title)} • ${if (mat.date.isNotBlank()) mat.date else "Geral"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { openInBrowser(context, mat.url) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Abrir material",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        FilledTonalIconButton(
                                            onClick = { downloadSuapFile(context, mat.title, mat.url) },
                                            modifier = Modifier.size(36.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Baixar material",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}


@Composable
fun AulasBentoHeader(
    totalAulas: Int,
    totalFaltas: Int,
    professor: String,
    totalRegistros: Int
) {
    val hasFaltas = totalFaltas > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bento Row 1: Aulas ministradas & Faltas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Total Aulas
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Aulas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$totalAulas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "$totalRegistros dias registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card Faltas
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (hasFaltas) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (hasFaltas) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasFaltas) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hasFaltas) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Faltas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasFaltas) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$totalFaltas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasFaltas) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = if (hasFaltas) "Faltas acumuladas" else "100% frequência",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasFaltas) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Professor Card (if available)
        if (professor.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Professor(a) Responsável",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = professor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AulaCard(
    aula: AulaHistory,
    matchingMaterials: List<Material> = emptyList(),
    onDownloadAttachment: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    onCopyContent: (String) -> Unit
) {
    val faltasCount = remember(aula.numFaltas) { aula.numFaltas.toIntOrNull() ?: 0 }
    val hasFalta = faltasCount > 0

    // Extract all unique links (from HTML and regex search on text)
    val combinedLinks = remember(aula) {
        val list = mutableListOf<AulaLink>()
        // 1. Links parsed directly from HTML <a> tags
        aula.links.forEach { link ->
            if (link.url.isNotBlank() && list.none { it.url == link.url }) {
                list.add(link)
            }
        }
        // 2. Fallback regex to capture any plaintext URLs (http, https, www)
        URL_REGEX.findAll(aula.conteudo).forEach { match ->
            val raw = match.value.trim().trimEnd('.', ',', ';', ':', ')', ']')
            val targetUrl = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
            if (targetUrl.isNotBlank() && list.none { it.url == targetUrl }) {
                list.add(AulaLink(title = raw, url = targetUrl))
            }
        }
        list
    }

    // Filter external web links (excluding already matched materials)
    val externalWebLinks = remember(combinedLinks, matchingMaterials) {
        val downloadUrls = matchingMaterials.map { it.url }.toSet()
        combinedLinks.filter { link ->
            !downloadUrls.contains(link.url)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (hasFalta) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (hasFalta) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date with Calendar Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (hasFalta) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hasFalta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = aula.data,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (aula.etapa.isNotBlank()) {
                            Text(
                                text = aula.etapa,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Presence / Absence pill
                Surface(
                    color = if (hasFalta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasFalta) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (hasFalta) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (hasFalta) {
                                if (faltasCount == 1) "1 Falta" else "$faltasCount Faltas"
                            } else {
                                "Presença"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasFalta) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: CONTEÚDO MINISTRADO
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (hasFalta) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "CONTEÚDO MINISTRADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Copy button
                        IconButton(
                            onClick = { onCopyContent(aula.conteudo) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar conteúdo",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = aula.conteudo.ifBlank { "Conteúdo não especificado pelo professor." },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 21.sp,
                            color = if (aula.conteudo.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Section 2: CONTEÚDO PARA DOWNLOAD (Highlighted & Distinct)
            if (matchingMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header for Download Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Text(
                                    text = "CONTEÚDO PARA DOWNLOAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${matchingMaterials.size} ${if (matchingMaterials.size == 1) "arquivo" else "arquivos"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // List of downloadable materials
                        matchingMaterials.forEach { mat ->
                            Surface(
                                onClick = { onDownloadAttachment(mat.url, mat.title) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getMaterialIcon(mat.url, mat.title),
                                            contentDescription = "Arquivo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mat.title.ifBlank { "Arquivo de Aula" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${getMaterialTypeDescription(mat.url, mat.title)} • ${if (mat.date.isNotBlank()) mat.date else "SUAP"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { onDownloadAttachment(mat.url, mat.title) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Baixar",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: LINKS EXTERNOS / ACESSO WEB
            if (externalWebLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Links para acesso web:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    externalWebLinks.forEach { link ->
                        Surface(
                            onClick = { onOpenLink(link.url) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = link.title.ifBlank { link.url },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Number of classes & Professor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class count badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val numAulasInt = remember(aula.numAulas) { aula.numAulas.toIntOrNull() ?: 1 }
                    Text(
                        text = "$numAulasInt ${if (numAulasInt == 1) "aula ministrada" else "aulas ministradas"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Professor name (if available)
                if (aula.professor.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val formattedProf = remember(aula.professor) {
                            aula.professor.split(" ").let { parts ->
                                if (parts.size >= 2) "${parts.first()} ${parts.last()}" else aula.professor
                            }
                        }
                        Text(
                            text = formattedProf,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandaloneMaterialCard(
    material: Material,
    onDownload: () -> Unit
) {
    val fileExtension = remember(material.url) { 
        material.url.substringAfterLast('.', "").lowercase() 
    }

    val secContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val sec = MaterialTheme.colorScheme.secondary
    val dangerContainer = AppSemanticColors.dangerContainer
    val danger = AppSemanticColors.danger
    val infoContainer = AppSemanticColors.infoContainer
    val info = AppSemanticColors.info
    val successContainer = AppSemanticColors.successContainer
    val success = AppSemanticColors.success
    val warningContainer = AppSemanticColors.warningContainer
    val warning = AppSemanticColors.warning
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary

    val (iconBgColor, iconTintColor, iconVector) = remember(
        fileExtension, secContainer, onSecContainer, sec,
        dangerContainer, danger, infoContainer, info,
        successContainer, success, warningContainer, warning,
        tertiaryContainer, tertiary
    ) {
        when (fileExtension) {
            "pdf" -> Triple(dangerContainer, danger, Icons.Default.PictureAsPdf)
            "doc", "docx" -> Triple(infoContainer, info, Icons.Default.Description)
            "xls", "xlsx" -> Triple(successContainer, success, Icons.Default.TableChart)
            "ppt", "pptx" -> Triple(warningContainer, warning, Icons.Default.Slideshow)
            "zip", "rar", "7z" -> Triple(tertiaryContainer, tertiary, Icons.Default.FolderZip)
            "jpg", "jpeg", "png" -> Triple(secContainer, onSecContainer, Icons.Default.Image)
            else -> Triple(sec.copy(alpha = 0.2f), sec, getMaterialIcon(material.url, material.title))
        }
    }

    OutlinedCard(
        onClick = onDownload,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBgColor, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = getMaterialTypeDescription(material.url, material.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (material.date.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        )
                        Text(
                            text = material.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onDownload,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Baixar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getMaterialIcon(url: String, title: String): androidx.compose.ui.graphics.vector.ImageVector {
    val combined = "$url $title".lowercase()
    return when {
        combined.contains(".pdf") -> Icons.Default.PictureAsPdf
        combined.contains(".zip") || combined.contains(".rar") || combined.contains(".7z") -> Icons.Default.FolderZip
        combined.contains(".doc") || combined.contains(".docx") -> Icons.Default.Description
        combined.contains(".ppt") || combined.contains(".pptx") || combined.contains("slide") || combined.contains("apresenta") -> Icons.Default.Slideshow
        combined.contains(".xls") || combined.contains(".xlsx") || combined.contains(".csv") -> Icons.Default.TableChart
        combined.contains(".mp4") || combined.contains(".mkv") || combined.contains("video") -> Icons.Default.VideoLibrary
        else -> Icons.Default.FileDownload
    }
}

fun getMaterialTypeDescription(url: String, title: String): String {
    val combined = "$url $title".lowercase()
    return when {
        combined.contains(".pdf") -> "Documento PDF"
        combined.contains(".zip") || combined.contains(".rar") || combined.contains(".7z") -> "Arquivo Compactado (ZIP)"
        combined.contains(".doc") || combined.contains(".docx") -> "Documento Word"
        combined.contains(".ppt") || combined.contains(".pptx") || combined.contains("slide") || combined.contains("apresenta") -> "Slides da Aula"
        combined.contains(".xls") || combined.contains(".xlsx") || combined.contains(".csv") -> "Planilha"
        combined.contains(".mp4") || combined.contains(".mkv") -> "Vídeo de Aula"
        else -> "Material de Estudo"
    }
}

fun isFileDownloadUrl(url: String): Boolean {
    val clean = url.lowercase().trim()
    if (clean.isBlank()) return false
    val fileExts = listOf(".pdf", ".zip", ".rar", ".7z", ".tar", ".gz", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".txt", ".csv", ".mp4", ".mp3", ".avi", ".mkv", ".ipynb", ".py", ".c", ".cpp", ".java", ".sql")
    if (fileExts.any { clean.endsWith(it) || clean.contains("$it?") }) return true
    return clean.contains("/media/") ||
           clean.contains("/material/") ||
           clean.contains("/download/") ||
           clean.contains("/documento/") ||
           clean.contains("/arquivo/") ||
           clean.contains("/registrar_acesso_aluno_material_aula/")
}

fun openInBrowser(context: Context, rawUrl: String) {
    try {
        val cleanUrl = rawUrl.trim()
        val fullUrl = when {
            cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true) -> cleanUrl
            cleanUrl.startsWith("www.", ignoreCase = true) -> "https://$cleanUrl"
            cleanUrl.startsWith("/") -> "https://suap.ifal.edu.br$cleanUrl"
            else -> "https://$cleanUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        com.example.util.AppLog.printStackTrace(e)
        com.example.util.AppSnackbarManager.show("Não foi possível abrir o link: ${e.message}")
    }
}

fun downloadSuapFile(context: Context, title: String, url: String) {
    try {
        val cleanUrl = url.trim()
        val fullUrl = if (cleanUrl.startsWith("http", ignoreCase = true)) cleanUrl else "https://suap.ifal.edu.br$cleanUrl"
        if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(fullUrl)) {
            val scheme = Uri.parse(cleanUrl).scheme?.lowercase()
            if (scheme == "https" || scheme == "http") openInBrowser(context, cleanUrl)
            else com.example.util.AppSnackbarManager.show("Endereço do arquivo inválido.")
            return
        }
        val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")

        val request = DownloadManager.Request(Uri.parse(fullUrl))
        if (cookie != null) {
            request.addRequestHeader("Cookie", cookie)
        }
        request.setTitle(title)
        request.setDescription("Baixando arquivo do SUAP")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val safeTitle = title.replace("/", "_").replace(":", "_").replace(" ", "_")
        val lastSegment = Uri.parse(url).lastPathSegment ?: ""
        val urlExt = if (lastSegment.contains(".")) lastSegment.substringAfterLast(".") else ""
        val fileName = if (urlExt.isNotEmpty() && !safeTitle.endsWith(".$urlExt", ignoreCase = true)) {
            "$safeTitle.$urlExt"
        } else if (!safeTitle.matches(".*\\.[a-zA-Z0-9]{2,4}$".toRegex())) {
            "$safeTitle.pdf"
        } else {
            safeTitle
        }

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        com.example.util.AppSnackbarManager.show("Iniciando download de $title...")
    } catch (e: Exception) {
        com.example.util.AppLog.printStackTrace(e)
        com.example.util.AppSnackbarManager.show("Erro ao iniciar download: ${e.message}")
    }
}

// Precompiled Regexes and Maps for maximum scroll performance
private val URL_REGEX = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"{}|\\^`\[\]]+""")
private val AULA_NUMBER_REGEX = Regex("""(?i)\b(?:aula|aula\s*n[ºo°]?)\s*0?(\d{1,2})\b""")
private val SLASH_DATE_PATTERN = Regex("""\b(\d{1,2})[/.\-_](\d{1,2})(?:[/.\-_](\d{2,4}))?\b""")
private val ISO_DATE_PATTERN = Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""")
private val TEXT_MONTH_PATTERN = Regex("""(?i)\b(\d{1,2})\s+de\s+(janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro|jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)(?:\s+de\s+(\d{2,4}))?\b""")
private val MONTH_MAP = mapOf(
    "jan" to "01", "janeiro" to "01",
    "fev" to "02", "fevereiro" to "02",
    "mar" to "03", "marco" to "03", "março" to "03",
    "abr" to "04", "abril" to "04",
    "mai" to "05", "maio" to "05",
    "jun" to "06", "junho" to "06",
    "jul" to "07", "julho" to "07",
    "ago" to "08", "agosto" to "08",
    "set" to "09", "setembro" to "09",
    "out" to "10", "outubro" to "10",
    "nov" to "11", "novembro" to "11",
    "dez" to "12", "dezembro" to "12"
)

fun isDateMatching(aula: AulaHistory, mat: Material): Boolean {
    // 1. Direct URL matching
    if (aula.attachmentUrl.isNotBlank() && (aula.attachmentUrl == mat.url || mat.url.contains(aula.attachmentUrl))) return true
    if (aula.links.any { it.url.isNotBlank() && (it.url == mat.url || mat.url.contains(it.url)) }) return true

    // 2. Direct string containment
    val cleanAulaDate = aula.data.trim()
    val cleanMatDate = mat.date.trim()
    if (cleanAulaDate.isNotBlank() && cleanMatDate.isNotBlank()) {
        if (cleanAulaDate.equals(cleanMatDate, ignoreCase = true)) return true
        if (cleanMatDate.contains(cleanAulaDate) || cleanAulaDate.contains(cleanMatDate)) return true
    }

    // 3. Normalized Date Tokens from Aula vs Material (scanning date, title, and url)
    val aulaTokens = extractDateTokens(aula.data + " " + aula.conteudo)
    val matTokens = extractDateTokens(mat.date + " " + mat.title + " " + mat.url)

    if (aulaTokens.isNotEmpty() && matTokens.isNotEmpty()) {
        if (aulaTokens.intersect(matTokens).isNotEmpty()) return true
    }

    // 4. Aula Number Matching (e.g. "Aula 03" or "Aula 3" in title and in aula)
    val aulaNumbersInAula = AULA_NUMBER_REGEX.findAll("${aula.conteudo} ${aula.etapa} ${aula.data}").mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()
    val aulaNumbersInMat = AULA_NUMBER_REGEX.findAll("${mat.title} ${mat.url}").mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()

    if (aulaNumbersInAula.isNotEmpty() && aulaNumbersInMat.isNotEmpty()) {
        if (aulaNumbersInAula.intersect(aulaNumbersInMat).isNotEmpty()) return true
    }

    return false
}

private fun extractDateTokens(text: String): Set<String> {
    val tokens = mutableSetOf<String>()
    if (text.isBlank()) return tokens

    // Match dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy, dd_MM_yyyy, dd/MM, dd-MM, etc.
    SLASH_DATE_PATTERN.findAll(text).forEach { match ->
        val day = match.groupValues[1].toIntOrNull() ?: 0
        val month = match.groupValues[2].toIntOrNull() ?: 0
        val yearStr = match.groupValues[3]

        if (day in 1..31 && month in 1..12) {
            val dStr = day.toString().padStart(2, '0')
            val mStr = month.toString().padStart(2, '0')
            tokens.add("$dStr/$mStr")

            if (yearStr.isNotBlank()) {
                val yStr = if (yearStr.length == 2) "20$yearStr" else yearStr
                tokens.add("$dStr/$mStr/$yStr")
            }
        }
    }

    // Match ISO yyyy-MM-dd
    ISO_DATE_PATTERN.findAll(text).forEach { match ->
        val year = match.groupValues[1]
        val month = match.groupValues[2].toIntOrNull() ?: 0
        val day = match.groupValues[3].toIntOrNull() ?: 0

        if (day in 1..31 && month in 1..12) {
            val dStr = day.toString().padStart(2, '0')
            val mStr = month.toString().padStart(2, '0')
            tokens.add("$dStr/$mStr")
            tokens.add("$dStr/$mStr/$year")
        }
    }

    // Match textual Portuguese months: e.g. "05 de março", "5 de mar"
    TEXT_MONTH_PATTERN.findAll(text).forEach { match ->
        val day = match.groupValues[1].toIntOrNull() ?: 0
        val monthWord = match.groupValues[2].lowercase()
        val yearStr = match.groupValues[3]
        val monthNum = MONTH_MAP[monthWord]

        if (day in 1..31 && monthNum != null) {
            val dStr = day.toString().padStart(2, '0')
            tokens.add("$dStr/$monthNum")
            if (yearStr.isNotBlank()) {
                val yStr = if (yearStr.length == 2) "20$yearStr" else yearStr
                tokens.add("$dStr/$monthNum/$yStr")
            }
        }
    }

    return tokens
}

