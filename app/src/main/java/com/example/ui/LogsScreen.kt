package com.example.ui

import com.example.ui.theme.Spacing

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import androidx.compose.material3.MaterialTheme

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                                Toast.makeText(context, "Logs salvos em Downloads!", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, "Logs salvos em: Downloads/$fileName", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao salvar em Downloads: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Logs salvos com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            saveLogsToStorage()
        }
    }

    fun triggerSaveLogs() {
        try {
            launcher.launch("suap_logs_${System.currentTimeMillis()}.txt")
        } catch (t: Throwable) {
            saveLogsToStorage()
        }
    }

    fun shareLogs() {
        try {
            val shareText = logs.takeLast(400).joinToString("\n")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Logs e Relatório do SUAP")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Logs"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar logs: ${e.message}", Toast.LENGTH_SHORT).show()
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
            AppTopBar(
                title = { Text("Logs e Erros do App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { loadLogs() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = {
                        CrashLogger.clearLogs(context)
                        loadLogs()
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Limpar Logs Internos")
                    }
                    IconButton(onClick = { shareLogs() }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartilhar")
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
                        .padding(horizontal = Spacing.normal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        onClick = { triggerSaveLogs() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baixar Logs (.txt)")
                    }

                    SecondaryButton(
                        onClick = { shareLogs() },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    .padding(horizontal = Spacing.normal, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = filterOnlyErrors,
                    onClick = { filterOnlyErrors = !filterOnlyErrors },
                    label = { Text("Apenas Erros/Crashes", style = MaterialTheme.typography.bodySmall) }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.normal),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(displayedLogs, key = { index, _ -> index }) { _, log ->
                            val safeLog = log.orEmpty()
                            val isError = safeLog.contains(" E ") || safeLog.contains("FATAL") || safeLog.contains("Exception") || safeLog.contains("Error", ignoreCase = true) || safeLog.contains("CRASH")
                            Text(
                                text = safeLog,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}
