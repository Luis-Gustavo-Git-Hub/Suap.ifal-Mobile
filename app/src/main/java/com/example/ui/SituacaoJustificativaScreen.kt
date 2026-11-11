package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class JustificativaFaltaItem(
    val dataInicio: String,
    val dataFim: String,
    val motivo: String,
    val dataSolicitacao: String,
    val situacao: String,
    val detalhesUrl: String? = null
)






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SituacaoJustificativaScreen(
    onBack: () -> Unit,
    onNavigateToDetalhes: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var justificativas by remember { mutableStateOf<List<JustificativaFaltaItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                val prefs = context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE)
                var matricula = prefs.getString("saved_matricula", "") ?: ""
                
                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie.isNullOrEmpty()) {
                    val cached = com.example.util.JustificativaCache.loadList(context)
                    if (cached != null) {
                        justificativas = cached
                        com.example.util.AppSnackbarManager.show("Sem sessão ativa — exibindo dados salvos.")
                    } else {
                        errorMessage = "Sessão expirada. Sincronize novamente no início."
                    }
                    isLoading = false
                    return@launch
                }
                
                if (matricula.isEmpty() || matricula == "-") {
                    try {
                        val homeDoc = withContext(Dispatchers.IO) {
                            Jsoup.connect("https://suap.ifal.edu.br/")
                                .header("Cookie", cookie)
                                .followRedirects(false)
                                .timeout(15000)
                                .userAgent("Mozilla/5.0 (Linux; Android 10; K)")
                                .get()
                        }
                        val profileLink = homeDoc.select("a.user-profile").attr("href")
                        val match = Regex("""/edu/aluno/(\d+)/""").find(profileLink)
                        if (match != null) {
                            matricula = match.groupValues[1]
                            prefs.edit().putString("saved_matricula", matricula).apply()
                        }
                    } catch(e: Exception) {}
                    
                    if (matricula.isEmpty() || matricula == "-") {
                        errorMessage = "Matrícula não identificada. Atualize seus dados no início."
                        isLoading = false
                        return@launch
                    }
                }

                val url = "https://suap.ifal.edu.br/edu/aluno/$matricula/?tab=abono_falta"
                val doc = withContext(Dispatchers.IO) {
                    Jsoup.connect(url)
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(15000)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; K)")
                        .get()
                }

                val items = mutableListOf<JustificativaFaltaItem>()
                val table = doc.select("div[data-tab=abono_falta] table").first()
                if (table != null) {
                    val rows = table.select("tbody tr")
                    for (row in rows) {
                        val cols = row.select("td")
                        if (cols.size >= 6) {
                            items.add(
                                JustificativaFaltaItem(
                                    dataInicio = cols[1].text().trim(),
                                    dataFim = cols[2].text().trim(),
                                    motivo = cols[3].text().trim(),
                                    dataSolicitacao = cols[4].text().trim(),
                                    situacao = cols[5].text().trim(),
                                    detalhesUrl = cols[0].selectFirst("a")?.attr("href")
                                )
                            )
                        }
                    }
                }
                justificativas = items
                com.example.util.JustificativaCache.saveList(context, items)
                isLoading = false
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                val cached = com.example.util.JustificativaCache.loadList(context)
                if (cached != null) {
                    justificativas = cached
                    com.example.util.AppSnackbarManager.show("Sem conexão — exibindo dados salvos.")
                } else {
                    errorMessage = "Erro ao buscar solicitações: ${e.message}"
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Situação das Justificativas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Acompanhamento no SUAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Carregando solicitações...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Erro desconhecido",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (justificativas.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nenhuma justificativa registrada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Você ainda não possui pedidos de abono de faltas enviados no SUAP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    items(
                        items = justificativas,
                        key = { it.detalhesUrl ?: "${it.dataInicio}_${it.motivo}" },
                        contentType = { "justificativa_item" }
                    ) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header: Período & Status Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Período Solicitado",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (item.dataInicio == item.dataFim) item.dataInicio else "${item.dataInicio} a ${item.dataFim}",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    val (statusBg, statusFg) = when (item.situacao.lowercase()) {
                                        "deferido" -> AppSemanticColors.success.copy(alpha = 0.15f) to AppSemanticColors.success
                                        "indeferido" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f) to MaterialTheme.colorScheme.error
                                        "aguardando avaliação" -> AppSemanticColors.warning.copy(alpha = 0.15f) to AppSemanticColors.warning
                                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = item.situacao.uppercase(),
                                            color = statusFg,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Motivo
                                Column {
                                    Text(
                                        text = "Motivo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.motivo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Date requested pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Solicitado em: ${item.dataSolicitacao}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Details Action Button (Accessible Height)
                                if (!item.detalhesUrl.isNullOrEmpty()) {
                                    Button(
                                        onClick = {
                                            val match = Regex("""/edu/abonofaltas/(\d+)/""").find(item.detalhesUrl)
                                            val id = match?.groupValues?.get(1)
                                            if (id != null) {
                                                onNavigateToDetalhes(id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ver Detalhes Completos",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold
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
