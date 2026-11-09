package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Material

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTopicsScreen(
    subjectName: String,
    subjectUrl: String,
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onViewPdfClick: (String, String) -> Unit = { _, _ -> }
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingMaterials.collectAsStateWithLifecycle()
    val errorMessage by viewModel.materialsError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(subjectUrl) {
        viewModel.fetchMaterials(subjectUrl)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedSubjectBadge(
                            subjectName = subjectName,
                            size = 42.dp,
                            iconSize = 22.dp
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = subjectName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Materiais de Aula",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchMaterials(subjectUrl) }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar materiais")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            AnimatedVisibility(
                visible = !isLoading && materials.isEmpty() && errorMessage == null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Nenhum material",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Não encontramos nenhum arquivo ou material de aula disponível para esta disciplina no momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        FilledTonalButton(
                            onClick = { viewModel.fetchMaterials(subjectUrl) },
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Atualizar página")
                        }
                    }
                }
            }

            if (!isLoading && errorMessage != null) {
                com.example.ui.components.AppFeedbackCard(
                    title = "Materiais indisponíveis",
                    message = errorMessage.orEmpty(),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    actionLabel = "Tentar novamente",
                    onAction = { viewModel.fetchMaterials(subjectUrl) }
                )
            }

            AnimatedVisibility(
                visible = !isLoading && materials.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut()
            ) {
                LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = materials,
                        key = { it.url.ifBlank { "${it.title}_${it.date}" } },
                        contentType = { "material_topic_card" }
                    ) { material ->
                        TopicCard(
                            material = material,
                            onDownloadClick = { downloadMaterial(context, material) },
                            onViewPdfClick = { filePath -> onViewPdfClick(material.title, filePath) }
                        )
                    }
                }
            }
        }
    }
}

fun downloadMaterial(context: Context, material: Material) {
    try {
        val fullUrl = if (material.url.startsWith("http")) material.url else "https://suap.ifal.edu.br${material.url}"
        val uri = Uri.parse(fullUrl)
        if (uri.scheme?.lowercase() != "https" || uri.host.isNullOrBlank()) {
            com.example.util.AppSnackbarManager.show("Endereço do material inválido.")
            return
        }
        val cookie = if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(fullUrl)) {
            CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
        } else null

        val request = DownloadManager.Request(Uri.parse(fullUrl))
        if (cookie != null) {
            request.addRequestHeader("Cookie", cookie)
        }
        request.setTitle(material.title)
        request.setDescription("Baixando material do SUAP")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        
        val safeTitle = material.title.replace("/", "_")
        val lastSegment = Uri.parse(material.url).lastPathSegment ?: ""
        val urlExt = if (lastSegment.contains(".")) lastSegment.substringAfterLast(".") else ""
        val fileName = if (urlExt.isNotEmpty() && !safeTitle.endsWith(".$urlExt", ignoreCase = true)) {
            "$safeTitle.$urlExt"
        } else if (!safeTitle.matches(".*\\\\.[a-zA-Z0-9]{2,4}$".toRegex())) {
            "$safeTitle.pdf"
        } else {
            safeTitle
        }
        
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        com.example.util.AppSnackbarManager.show("Iniciando download de ${material.title}...")
    } catch (e: Exception) {
        com.example.util.AppLog.printStackTrace(e)
        com.example.util.AppSnackbarManager.show("Erro ao iniciar download", actionLabel = "Tentar de novo", onAction = { downloadMaterial(context, material) })
    }
}


fun getMaterialFileName(material: Material): String {
    val safeTitle = material.title.replace("/", "_")
    val lastSegment = Uri.parse(material.url).lastPathSegment ?: ""
    val urlExt = if (lastSegment.contains(".")) lastSegment.substringAfterLast(".") else ""
    return if (urlExt.isNotEmpty() && !safeTitle.endsWith(".$urlExt", ignoreCase = true)) {
        "$safeTitle.$urlExt"
    } else if (!safeTitle.matches(".*\\.[a-zA-Z0-9]{2,4}$".toRegex())) {
        "$safeTitle.pdf"
    } else {
        safeTitle
    }
}

fun getMaterialFile(material: Material): java.io.File {
    return java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getMaterialFileName(material))
}

@Composable
fun TopicCard(
    modifier: Modifier = Modifier, 
    material: Material, 
    onDownloadClick: () -> Unit, 
    onViewPdfClick: ((String) -> Unit)? = null
) {
    var hasRequestedDownload by remember { mutableStateOf(false) }
    val fileExtension = remember(material.url) { 
        material.url.substringAfterLast('.', "").lowercase() 
    }
    
    val errColor = MaterialTheme.colorScheme.error
    val primColor = MaterialTheme.colorScheme.primary
    val succColor = AppSemanticColors.success
    val warnColor = AppSemanticColors.warning
    val tertColor = MaterialTheme.colorScheme.tertiary
    val secColor = MaterialTheme.colorScheme.secondary
    val outColor = MaterialTheme.colorScheme.outline

    val (iconBgBrush, iconTintColor, iconVector, formatLabel) = remember(fileExtension, errColor, primColor, succColor, warnColor, tertColor, secColor, outColor) {
        when (fileExtension) {
            "pdf" -> Quadruple(
                listOf(errColor, errColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.PictureAsPdf,
                "PDF"
            )
            "doc", "docx" -> Quadruple(
                listOf(primColor, primColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.Description,
                "WORD"
            )
            "xls", "xlsx", "csv" -> Quadruple(
                listOf(succColor, succColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.TableChart,
                "PLANILHA"
            )
            "ppt", "pptx" -> Quadruple(
                listOf(warnColor, warnColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.Slideshow,
                "SLIDES"
            )
            "zip", "rar", "7z" -> Quadruple(
                listOf(tertColor, tertColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.FolderZip,
                "ZIP"
            )
            "jpg", "jpeg", "png", "gif" -> Quadruple(
                listOf(secColor, secColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.Image,
                "IMAGEM"
            )
            "mp4", "mkv", "avi" -> Quadruple(
                listOf(primColor, tertColor),
                Color.White,
                Icons.Default.VideoLibrary,
                "VÍDEO"
            )
            else -> Quadruple(
                listOf(outColor, outColor.copy(alpha = 0.8f)),
                Color.White,
                Icons.Default.InsertDriveFile,
                "ARQUIVO"
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vibrant glossy 3D File icon badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(iconBgBrush),
                            shape = MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                            ),
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "Tipo de Material",
                        tint = iconTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = iconBgBrush.first().copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = formatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iconBgBrush.first(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (material.date.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = material.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = material.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fileExtension == "pdf" && onViewPdfClick != null) {
                    OutlinedButton(
                        onClick = { onViewPdfClick(material.url) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Visualizar PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Visualizar",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        hasRequestedDownload = true
                        onDownloadClick()
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Baixar Arquivo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
