package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterOnlyErrors by remember { mutableStateOf(false) }

    fun loadLogs() {
        coroutineScope.launch {
            isLoading = true
            logs = withContext(Dispatchers.IO) {
                try {
                    val fullText = CrashLogger.getAllLogs(context)
                    val lines = fullText.lines()
                    if (lines.isEmpty()) listOf("Nenhum log disponível.") else lines
                } catch (t: Throwable) {
                    listOf("Erro ao carregar logs: ${t.localizedMessage ?: t.message}")
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadLogs()
    }

    fun saveLogsToStorage() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentText = logs.joinToString("\n")
                    val fileName = "suap_app_logs_${System.currentTimeMillis()}.txt"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { stream ->
                                stream.write(contentText.toByteArray())
                            }
                            withContext(Dispatchers.Main) {
                                com.example.util.AppSnackbarManager.show("Logs salvos em Downloads!")
                            }
                            return@withContext
                        }
                    }

                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val logFile = File(downloadsDir, fileName)
                    FileOutputStream(logFile).use { fos ->
                        fos.write(contentText.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        com.example.util.AppSnackbarManager.show("Logs salvos em: Downloads/$fileName")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        com.example.util.AppSnackbarManager.show("Erro ao salvar em Downloads: ${e.message}")
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(logs.joinToString("\n").toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            com.example.util.AppSnackbarManager.show("Logs salvos com sucesso!")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            com.example.util.AppSnackbarManager.show("Erro ao salvar: ${e.message}")
                        }
                    }
                }
            }
        } else {
            // saveLogsToStorage()
        }
    }

    fun triggerSaveLogs() {
        try {
            launcher.launch("suap_logs_${System.currentTimeMillis()}.txt")
        } catch (t: Throwable) {
            // saveLogsToStorage()
        }
    }

    fun shareLogs() {
        coroutineScope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    CrashLogger.createShareableReport(context)
                }
                val reportUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    report
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Relatório de diagnóstico SUAP")
                    putExtra(Intent.EXTRA_STREAM, reportUri)
                    clipData = android.content.ClipData.newUri(context.contentResolver, "Relatório SUAP", reportUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório"))
            } catch (e: Exception) {
                com.example.util.AppSnackbarManager.show("Não foi possível preparar o relatório para compartilhamento.")
            }
        }
    }

    val displayedLogs = remember(logs, filterOnlyErrors) {
        val safeLogs = logs.orEmpty()
        if (filterOnlyErrors) {
            safeLogs.filter { log ->
                val line = log.orEmpty()
                line.contains(" E ") || line.contains("FATAL") || line.contains("Exception") || line.contains("Error", ignoreCase = true) || line.contains("CRASH")
            }
        } else {
            safeLogs
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs e Erros do App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { loadLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = {
                        CrashLogger.clearLogs(context)
                        loadLogs()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpar Logs Internos")
                    }
                    IconButton(onClick = { shareLogs() }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { triggerSaveLogs() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baixar Logs (.txt)")
                    }

                    OutlinedButton(
                        onClick = { shareLogs() },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = filterOnlyErrors,
                    onClick = { filterOnlyErrors = !filterOnlyErrors },
                    label = { Text("Apenas Erros/Crashes", style = MaterialTheme.typography.labelMedium) }
                )
                Text(
                    text = "${displayedLogs.size} linhas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = displayedLogs,
                            key = { index, _ -> index },
                            contentType = { _, _ -> "log_line" }
                        ) { _, log ->
                            val safeLog = log.orEmpty()
                            val isError = safeLog.contains(" E ") || safeLog.contains("FATAL") || safeLog.contains("Exception") || safeLog.contains("Error", ignoreCase = true) || safeLog.contains("CRASH")
                            Text(
                                text = safeLog,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
