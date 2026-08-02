package com.example.ui

import androidx.compose.animation.animateContentSize
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.Aviso
import com.example.data.ProfessorContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisosScreen(viewModel: SuapViewModel) {
    val avisos by viewModel.avisos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAvisos.collectAsStateWithLifecycle()
    val professorContacts by viewModel.professorContacts.collectAsStateWithLifecycle()
    var showProfessorsSheet by remember { mutableStateOf(false) }
    
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchAvisos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Avisos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Comunicados do SUAP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (professorContacts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "E-mails dos Professores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showProfessorsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Expandir E-mails",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(professorContacts) { professor ->
                    CompactProfessorCard(
                        professor = professor,
                        onCopyEmail = { email ->
                            clipboardManager.setText(AnnotatedString(email))
                            Toast.makeText(
                                context,
                                "E-mail copiado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        PaddingValues(horizontal = 24.dp).let {
            Text(
                text = "Avisos Recentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (avisos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tudo limpo por aqui!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Nenhum aviso encontrado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(avisos) { aviso ->
                    CompactAvisoCard(aviso = aviso, viewModel = viewModel)
                }
            }
        }
    }


    if (showProfessorsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfessorsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProfessorContactsSheetContent(
                contacts = professorContacts,
                onClose = { showProfessorsSheet = false }
            )
        }
    }
}

@Composable
fun CompactProfessorCard(
    professor: ProfessorContact,
    onCopyEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfessorAvatar(
                    photoUrl = professor.photoUrl,
                    name = professor.name,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = professor.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (professor.discipline.isNotEmpty()) {
                        Text(
                            text = professor.discipline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            // Email row with copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = professor.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onCopyEmail(professor.email) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar E-mail",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessorAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = remember(name) {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
            parts[0].first().uppercase()
        } else {
            "P"
        }
    }
    val backgroundColor = remember(name) {
        val colors = listOf(
            Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F),
            Color(0xFF7B1FA2), Color(0xFFE65100), Color(0xFF00796B),
            Color(0xFF0288D1), Color(0xFFC2185B)
        )
        val index = kotlin.math.abs(name.hashCode()) % colors.size
        colors[index]
    }
    val cookie = remember {
        try {
            android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: ""
        } catch (e: Exception) { "" }
    }
    val targetUrl = remember(photoUrl) {
        if (photoUrl.isNotBlank()) {
            photoUrl
        } else {
            ""
        }
    }
    
    val imageRequest = remember(targetUrl, cookie) {
        ImageRequest.Builder(context)
            .data(targetUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .apply {
                if (cookie.isNotEmpty() && targetUrl.contains("suap")) {
                    addHeader("Cookie", cookie)
                    addHeader("Referer", "https://suap.ifal.edu.br/")
                }
            }
            .crossfade(true)
            .build()
    }
    
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Foto de $name",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
    ) {
        val state = painter.state
        if (state is coil.compose.AsyncImagePainter.State.Loading || state is coil.compose.AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

@Composable
fun CompactAvisoCard(aviso: Aviso, viewModel: SuapViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var messageDetail by remember { mutableStateOf<com.example.data.MessageDetail?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = aviso.subject,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = aviso.sender,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = aviso.date.take(10), // Short date format if possible
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (aviso.detailUrl.isNotEmpty()) {
                        androidx.compose.material3.IconButton(
                            onClick = { 
                                isExpanded = !isExpanded
                                if (isExpanded && messageDetail == null) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        messageDetail = viewModel.getMessageDetail(aviso.detailUrl)
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Visibility,
                                contentDescription = if (isExpanded) "Fechar Detalhes" else "Visualizar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(8.dp).size(24.dp))
                    } else {
                        Column {
                            Text(
                                text = messageDetail?.content ?: "Não foi possível carregar o conteúdo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (messageDetail != null && messageDetail!!.attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Anexos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                messageDetail!!.attachments.forEach { attachment ->
                                    val context = LocalContext.current
                                    OutlinedButton(
                                        onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(attachment.url))
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Erro ao abrir link", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(androidx.compose.material.icons.Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(attachment.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorContactsSheetContent(
    contacts: List<ProfessorContact>,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val filteredContacts = remember(searchQuery, contacts) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.discipline.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = 20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContactMail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "E-mails dos Professores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por professor ou disciplina...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${filteredContacts.size} professor(es) encontrado(s)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum professor encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredContacts) { professor ->
                    ProfessorCardItem(
                        professor = professor,
                        onCopyEmail = { email ->
                            clipboardManager.setText(AnnotatedString(email))
                            Toast.makeText(
                                context,
                                "E-mail copiado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onSendEmail = { email ->
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Enviar e-mail"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Nenhum app de e-mail encontrado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessorCardItem(
    professor: ProfessorContact,
    onCopyEmail: (String) -> Unit,
    onSendEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfessorAvatar(
                    photoUrl = professor.photoUrl,
                    name = professor.name,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = professor.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (professor.discipline.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = professor.discipline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Email Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onCopyEmail(professor.email) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = professor.email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onCopyEmail(professor.email) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar E-mail",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onSendEmail(professor.email) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Enviar E-mail",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
