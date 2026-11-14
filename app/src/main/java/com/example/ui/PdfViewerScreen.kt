package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.BufferedInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val fullUrl = if (url.startsWith("http")) url else "https://suap.ifal.edu.br$url"
                if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(fullUrl)) {
                    errorMessage = "Endereço do PDF bloqueado por segurança."
                    return@withContext
                }
                val request = Request.Builder().url(fullUrl).build()

                val client = com.example.network.SuapClient.okHttpClient
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body ?: throw IOException("O servidor não retornou um arquivo.")
                        val mediaType = body.contentType()?.type.orEmpty()
                        if (mediaType.isNotEmpty() && mediaType != "application" && mediaType != "binary") {
                            throw IOException("A resposta do SUAP não é um PDF.")
                        }
                        val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                        try {
                            BufferedInputStream(body.byteStream()).use { input ->
                                input.mark(PDF_HEADER_SCAN_BYTES)
                                val header = ByteArray(PDF_HEADER_SCAN_BYTES)
                                val headerLength = input.read(header).coerceAtLeast(0)
                                val hasPdfHeader = String(header, 0, headerLength, Charsets.US_ASCII).contains("%PDF-")
                                input.reset()
                                if (!hasPdfHeader) throw IOException("O SUAP retornou um conteúdo que não é PDF.")
                                file.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (file.length() == 0L) throw IOException("O PDF recebido está vazio.")
                            pdfFile = file
                        } catch (e: Exception) {
                            file.delete()
                            throw e
                        }
                    } else {
                        errorMessage = "Erro ao baixar o arquivo. Código: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errorMessage = "Falha ao carregar o PDF: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (pdfFile != null) {
                PdfRendererView(pdfFile!!)
            }
        }
    }
}

@Composable
fun PdfRendererView(file: File) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    val renderMutex = remember { Mutex() }

    DisposableEffect(file) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        pdfRenderer = renderer
        onDispose {
            renderer.close()
            fileDescriptor.close()
            if (file.exists()) file.delete()
        }
    }

    pdfRenderer?.let { renderer ->
        LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = renderer.pageCount,
                key = { it },
                contentType = { "pdf_page" }
            ) { index ->
                var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                
                LaunchedEffect(index) {
                    withContext(Dispatchers.IO) {
                        renderMutex.withLock {
                            try {
                                val page = renderer.openPage(index)
                                try {
                                    val density = context.resources.displayMetrics.density
                                    val targetWidth = (context.resources.displayMetrics.widthPixels - (16 * density).toInt())
                                        .coerceAtLeast(1)
                                        .coerceAtMost(MAX_RENDER_WIDTH_PX)
                                    val scale = targetWidth.toFloat() / page.width.toFloat()
                                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    imageBitmap = bitmap.asImageBitmap()
                                } finally {
                                    page.close()
                                }
                            } catch (e: Exception) {
                                com.example.util.AppLog.printStackTrace(e)
                            }
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = "Página ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_RENDER_WIDTH_PX = 1800
private const val PDF_HEADER_SCAN_BYTES = 1024
