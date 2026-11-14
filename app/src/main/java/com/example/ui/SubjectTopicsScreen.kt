package com.example.ui

import com.example.ui.theme.Spacing

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import androidx.compose.material3.MaterialTheme

import android.net.Uri
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onBack: () -> Unit
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingMaterials.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(subjectUrl) {
        viewModel.fetchMaterials(subjectUrl)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { 
                    Column {
                        Text(
                            text = subjectName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = "Materiais da disciplina", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.back))
                    }
                },
                actions = {
                    if (materials.isNotEmpty()) {
                        IconButton(onClick = {
                            Toast.makeText(context, "Baixando ${materials.size} materiais...", Toast.LENGTH_SHORT).show()
                            materials.forEach { material ->
                                try {
                                    val url = "https://suap.ifal.edu.br${material.url}"
                                    val request = DownloadManager.Request(Uri.parse(url))
                                    val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                                    if (cookie != null) {
                                        request.addRequestHeader("Cookie", cookie)
                                    }
                                    request.setTitle(material.title)
                                    request.setDescription("Baixando material do SUAP")
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    val safeTitle = material.title.replace("/", "_")
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeTitle)
                                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    downloadManager.enqueue(request)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Icon(Icons.Rounded.Download, contentDescription = "Baixar Todos")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingState(Modifier.padding(padding))
        } else if (materials.isEmpty()) {
            EmptyState("Nenhum material de aula encontrado para $subjectName.", Modifier.padding(padding), "Tentar novamente") { viewModel.fetchMaterials(subjectUrl) }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.normal)
            ) {
                items(materials) { material ->
                    TopicCard(
                        modifier = Modifier,
                        material = material,
                        onDownloadClick = {
                            try {
                                val fullUrl = if (material.url.startsWith("http")) material.url else "https://suap.ifal.edu.br${material.url}"
                                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")

                                val request = DownloadManager.Request(Uri.parse(fullUrl))
                                if (cookie != null) {
                                    request.addRequestHeader("Cookie", cookie)
                                }
                                request.setTitle(material.title)
                                request.setDescription("Baixando material do SUAP")
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                val safeTitle = material.title.replace("/", "_")
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeTitle)
                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                                Toast.makeText(context, "Iniciando download de ${material.title}...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Erro ao iniciar download", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopicCard(modifier: Modifier = Modifier, material: Material, onDownloadClick: () -> Unit) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDownloadClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.normal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "Material",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (material.date.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = material.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Acessar material",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

