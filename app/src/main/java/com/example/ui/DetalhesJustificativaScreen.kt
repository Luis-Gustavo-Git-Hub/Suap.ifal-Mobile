package com.example.ui

import com.example.ui.theme.AppSemanticColors

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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

data class JustificativaDetalhes(
    val aluno: String,
    val dataInicio: String,
    val dataFim: String,
    val motivo: String,
    val infoComplementar: String,
    val anexoNome: String?,
    val anexoUrl: String?,
    val dataSolicitacao: String,
    val situacao: String,
    val podeSolicitarReposicao: Boolean,
    val detalhamentoMensagem: String? = null,
    val detalhamentoFaltas: List<Map<String, String>> = emptyList()
)





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesJustificativaScreen(
    justificativaId: String,
    onBack: () -> Unit,
    onNavigateToReposicao: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var detalhes by remember { mutableStateOf<JustificativaDetalhes?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(justificativaId) {
        isLoading = true
        coroutineScope.launch {
            try {
                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie.isNullOrEmpty()) {
                    val cached = com.example.util.JustificativaCache.loadDetalhes(context, justificativaId)
                    if (cached != null) {
                        detalhes = cached
                        com.example.util.AppSnackbarManager.show("Sem sessão ativa — exibindo dados salvos.")
                    } else {
                        errorMessage = "Sessão expirada. Sincronize novamente no início."
                    }
                    isLoading = false
                    return@launch
                }

                val url = "https://suap.ifal.edu.br/edu/abonofaltas/$justificativaId/"
                val doc = withContext(Dispatchers.IO) {
                    Jsoup.connect(url)
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(15000)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; K)")
                        .get()
                }

                var aluno = "-"
                var dataInicio = "-"
                var dataFim = "-"
                var motivo = "-"
                var infoComplementar = "-"
                var anexoNome: String? = null
                var anexoUrl: String? = null
                var dataSolicitacao = "-"

                val dlItems = doc.select("dl.definition-list div.list-item")
                for (item in dlItems) {
                    val dt = item.selectFirst("dt")?.text()?.trim() ?: ""
                    val dd = item.selectFirst("dd")
                    
                    if (dt.isNotEmpty() && dd != null) {
                        when (dt) {
                            "Aluno" -> aluno = dd.text().trim()
                            "Data de Início" -> dataInicio = dd.text().trim()
                            "Data de Fim" -> dataFim = dd.text().trim()
                            "Motivo" -> motivo = dd.text().trim()
                            "Informação Complementar" -> infoComplementar = dd.text().trim()
                            "Data da Solicitação" -> dataSolicitacao = dd.text().trim()
                            "Anexo" -> {
                                val link = dd.selectFirst("a")
                                if (link != null) {
                                    anexoNome = link.text().trim()
                                    anexoUrl = link.attr("href")
                                }
                            }
                        }
                    }
                }

                val situacaoBadge = doc.selectFirst(".object-status .status")
                val situacao = situacaoBadge?.text()?.trim() ?: "Desconhecido"
                
                val podeSolicitarReposicao = doc.select("a[href*=/solicitar_reposicao_atividade/]").isNotEmpty()
                
                var detalhamentoMensagem: String? = null
                val detalhamentoFaltas = mutableListOf<Map<String, String>>()
                
                val detalhamentoDiv = doc.select("div#detalhamento .accordion-body").first()
                if (detalhamentoDiv != null) {
                    val alert = detalhamentoDiv.select(".alert").first()
                    if (alert != null) {
                        detalhamentoMensagem = alert.text().trim()
                    }
                    
                    val table = detalhamentoDiv.select("table").first()
                    if (table != null) {
                        val headers = table.select("thead th").map { it.text().trim() }
                        val rows = table.select("tbody tr")
                        for (row in rows) {
                            val cols = row.select("td").map { it.text().trim() }
                            val map = mutableMapOf<String, String>()
                            for (i in headers.indices) {
                                if (i < cols.size) {
                                    map[headers[i]] = cols[i]
                                }
                            }
                            if (map.isNotEmpty()) {
                                detalhamentoFaltas.add(map)
                            }
                        }
                    }
                }

                detalhes = JustificativaDetalhes(
                    aluno = aluno,
                    dataInicio = dataInicio,
                    dataFim = dataFim,
                    motivo = motivo,
                    infoComplementar = infoComplementar,
                    anexoNome = anexoNome,
                    anexoUrl = anexoUrl,
                    dataSolicitacao = dataSolicitacao,
                    situacao = situacao,
                    podeSolicitarReposicao = podeSolicitarReposicao,
                    detalhamentoMensagem = detalhamentoMensagem,
                    detalhamentoFaltas = detalhamentoFaltas
                )
                detalhes?.let { com.example.util.JustificativaCache.saveDetalhes(context, justificativaId, it) }

                isLoading = false
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                val cached = com.example.util.JustificativaCache.loadDetalhes(context, justificativaId)
                if (cached != null) {
                    detalhes = cached
                    com.example.util.AppSnackbarManager.show("Sem conexão — exibindo dados salvos.")
                } else {
                    errorMessage = "Erro ao carregar detalhes: ${e.message}"
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
                            text = "Detalhes da Solicitação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Abono de Falta / SUAP",
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
                    Text("Carregando detalhes do processo...", style = MaterialTheme.typography.bodySmall)
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
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (detalhes != null) {
                val item = detalhes!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Banner
                    val (statusBg, statusFg) = when (item.situacao.lowercase()) {
                        "deferido" -> AppSemanticColors.success.copy(alpha = 0.15f) to AppSemanticColors.success
                        "indeferido" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f) to MaterialTheme.colorScheme.error
                        "aguardando avaliação" -> AppSemanticColors.warning.copy(alpha = 0.15f) to AppSemanticColors.warning
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = statusBg
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(statusFg.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.situacao.contains("deferido", true)) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = statusFg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Situação da Solicitação",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusFg.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = item.situacao,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusFg
                                )
                            }
                        }
                    }

                    // Main Details Bento Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            DetailRowItem(icon = Icons.Default.Person, label = "Aluno", value = item.aluno)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            DetailRowItem(
                                icon = Icons.Default.DateRange,
                                label = "Período da Ausência",
                                value = if (item.dataInicio == item.dataFim) item.dataInicio else "${item.dataInicio} a ${item.dataFim}"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            DetailRowItem(icon = Icons.Default.Assignment, label = "Motivo Registrado", value = item.motivo)
                            
                            if (item.infoComplementar != "-") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                DetailRowItem(icon = Icons.Default.Notes, label = "Informação Complementar", value = item.infoComplementar)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            DetailRowItem(icon = Icons.Default.CalendarToday, label = "Data de Solicitação", value = item.dataSolicitacao)
                            
                            if (item.anexoUrl != null && item.anexoNome != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                Column {
                                    Text(
                                        text = "Documento Anexado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.anexoUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    com.example.util.AppLog.printStackTrace(e)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = item.anexoNome,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Abrir",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (item.podeSolicitarReposicao) {
                        Button(
                            onClick = { onNavigateToReposicao(justificativaId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solicitar Reposição de Atividade", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
