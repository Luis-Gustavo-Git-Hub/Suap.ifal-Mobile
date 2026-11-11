package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior

import androidx.compose.animation.animateContentSize
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val errorMessage by viewModel.avisosError.collectAsStateWithLifecycle()
    val professorContacts by viewModel.professorContacts.collectAsStateWithLifecycle()
    var showProfessorsSheet by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showUnreadOnly by rememberSaveable { mutableStateOf(false) }
    var professorToEdit by remember { mutableStateOf<ProfessorContact?>(null) }

    val filteredAvisos = remember(searchQuery, showUnreadOnly, avisos) {
        val q = searchQuery.trim()
        avisos.filter { aviso ->
            (!showUnreadOnly || !aviso.isRead) &&
                (q.isBlank() || aviso.subject.contains(q, ignoreCase = true) ||
                    aviso.sender.contains(q, ignoreCase = true) || aviso.date.contains(q, ignoreCase = true))
        }
    }
    
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchAvisos()
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.fetchAvisos() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
        com.example.ui.components.AppPageHeader(
            eyebrow = "SUAP",
            title = "Central de avisos",
            subtitle = "Comunicados e contatos dos seus professores.",
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            supportingContent = {
                if (lastSyncTime.isNotBlank()) {
                    com.example.ui.components.SyncTimestampBadge(rawTimestamp = lastSyncTime)
                }
            }
        )
        
        errorMessage?.let { message ->
            com.example.ui.components.AppFeedbackCard(
                title = "Não foi possível atualizar os avisos",
                message = if (avisos.isNotEmpty()) "$message Exibindo os avisos salvos no dispositivo." else message,
                modifier = Modifier.padding(vertical = 8.dp),
                actionLabel = "Tentar novamente",
                onAction = { viewModel.fetchAvisos() }
            )
        }

        val confirmedEmails = professorContacts.count { !it.emailEstimated && com.example.util.ProfessorEmail.isValid(it.email) }
        val estimatedEmails = professorContacts.count {
            it.emailEstimated || !com.example.util.ProfessorEmail.isValid(it.email)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("E-mails dos professores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (professorContacts.isEmpty()) "Os contatos aparecem após sincronizar as disciplinas."
                        else "$confirmedEmails confirmados · $estimatedEmails estimados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showProfessorsSheet = true }, enabled = professorContacts.isNotEmpty()) {
                    Text("Ver todos")
                }
            }
            if (professorContacts.isNotEmpty()) LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = professorContacts,
                    key = { com.example.util.ProfessorEmail.nameKey(it.name) },
                    contentType = { "compact_professor_card" }
                ) { professor ->
                    CompactProfessorCard(
                        professor = professor,
                        onCopyEmail = { email ->
                            clipboardManager.setText(AnnotatedString(email))
                            com.example.util.AppSnackbarManager.show("E-mail copiado!")
                        }
                    )
                }
            }
            else Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    "Sincronize seus dados acadêmicos para carregar professores e e-mails.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Avisos recentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (filteredAvisos.size != avisos.size || avisos.any { !it.isRead }) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = if (showUnreadOnly) "${filteredAvisos.size} não lidos" else "${avisos.count { !it.isRead }} não lidos",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text("Filtrar avisos...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
            FilterChip(
                selected = !showUnreadOnly,
                onClick = { showUnreadOnly = false },
                label = { Text("Todos (${avisos.size})") }
            )
            FilterChip(
                selected = showUnreadOnly,
                onClick = { showUnreadOnly = true },
                label = { Text("Não lidos (${avisos.count { !it.isRead }})") },
                leadingIcon = if (avisos.any { !it.isRead }) ({ Icon(Icons.Default.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && filteredAvisos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    com.example.ui.components.AvisoCardSkeleton()
                }
            }
        } else if (filteredAvisos.isEmpty() && errorMessage == null) {
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
                        text = when {
                            showUnreadOnly && avisos.none { !it.isRead } -> "Você não tem avisos não lidos."
                            showUnreadOnly -> "Nenhum aviso não lido corresponde à busca."
                            searchQuery.isNotEmpty() -> "Nenhum aviso corresponde à busca!"
                            else -> "Tudo limpo por aqui!"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (showUnreadOnly && avisos.none { !it.isRead }) "Os avisos sincronizados estão marcados como lidos." else "Ajuste a busca ou escolha ‘Todos’ para ver outros avisos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredAvisos,
                    key = { it.detailUrl.ifBlank { "${it.subject}_${it.date}" } },
                    contentType = { "aviso_card" }
                ) { aviso ->
                    CompactAvisoCard(aviso = aviso, viewModel = viewModel)
                }
            }
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
                onClose = { showProfessorsSheet = false },
                onEditEmail = { professorToEdit = it }
            )
        }
    }

    professorToEdit?.let { prof ->
        var emailText by remember(prof.name) { mutableStateOf(if (prof.emailEstimated) "" else prof.email) }
        val invalidEmail = emailText.isNotBlank() && !com.example.util.ProfessorEmail.isValid(emailText)
        AlertDialog(
            onDismissRequest = { professorToEdit = null },
            title = { Text("E-mail de ${prof.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (prof.emailEstimated) {
                        Text(
                            "O SUAP não informou o e-mail; o app estimou ${prof.email}. Digite o correto ou deixe em branco para manter a estimativa.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        isError = invalidEmail,
                        supportingText = if (invalidEmail) {
                            { Text("Digite um endereço de e-mail válido.") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.setProfessorEmail(prof.name, emailText)) {
                        professorToEdit = null
                    }
                }, enabled = !invalidEmail) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { professorToEdit = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun CompactProfessorCard(
    professor: ProfessorContact,
    onCopyEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Premium Bento Card styling with custom linear gradient
    val cardBgGradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Card(
        modifier = Modifier.width(290.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .background(cardBgGradient)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfessorAvatar(
                    photoUrl = professor.photoUrl,
                    name = professor.name,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = professor.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (professor.discipline.isNotEmpty()) {
                        Text(
                            text = professor.discipline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

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
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = professor.email + if (professor.emailEstimated) " (estimado)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                FilledTonalIconButton(
                    onClick = { onCopyEmail(professor.email) },
                    modifier = Modifier.size(30.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar E-mail",
                        modifier = Modifier.size(13.dp)
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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val backgroundColor = remember(name, primary, secondary, tertiary) {
        val colors = listOf(primary, secondary, tertiary)
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
                if (cookie.isNotEmpty() && com.example.util.UrlSecurityUtils.isAllowedSuapUrl(targetUrl)) {
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

@Composable
fun CompactAvisoCard(aviso: Aviso, viewModel: SuapViewModel) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var messageDetail by remember { mutableStateOf<com.example.data.MessageDetail?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val accentColor = remember(aviso.subject, primary, secondary, tertiary) {
        val colors = listOf(primary, secondary, tertiary)
        val index = kotlin.math.abs(aviso.subject.hashCode()) % colors.size
        colors[index]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                isExpanded = !isExpanded
                if (isExpanded && messageDetail == null) {
                    isLoading = true
                    coroutineScope.launch {
                        messageDetail = viewModel.getMessageDetail(aviso.detailUrl)
                        isLoading = false
                    }
                }
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) {
                accentColor.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular visual indicator with dynamic icon and gradient-like background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.06f)
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Core details column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = aviso.subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = if (isExpanded) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = aviso.sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Right side: date & expand arrow
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = accentColor.copy(alpha = 0.08f),
                        border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = aviso.date.take(10),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp))
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth().height(60.dp), shape = MaterialTheme.shapes.small)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Content card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = messageDetail?.content ?: "Não foi possível carregar o conteúdo.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 24.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick action buttons inside card content
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copy Button
                                    TextButton(
                                        onClick = {
                                            messageDetail?.content?.let { content ->
                                                clipboardManager.setText(AnnotatedString(content))
                                                com.example.util.AppSnackbarManager.show("Conteúdo copiado!")
                                            }
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Copiar", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Share Button
                                    TextButton(
                                        onClick = {
                                            messageDetail?.content?.let { content ->
                                                try {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "${aviso.subject}\n\n$content")
                                                        type = "text/plain"
                                                    }
                                                    val shareIntent = Intent.createChooser(sendIntent, "Compartilhar aviso")
                                                    context.startActivity(shareIntent)
                                                } catch (e: Exception) {
                                                    com.example.util.AppSnackbarManager.show("Erro ao compartilhar aviso")
                                                }
                                            }
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Compartilhar", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Attachments section
                        if (messageDetail != null && messageDetail!!.attachments.isNotEmpty()) {
                            Text(
                                text = "ANEXOS DISPONÍVEIS:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            messageDetail!!.attachments.forEach { attachment ->
                                Surface(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            com.example.util.AppSnackbarManager.show("Erro ao abrir link")
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shadowElevation = 0.5.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(accentColor.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = attachment.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = accentColor.copy(alpha = 0.7f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorContactsSheetContent(
    contacts: List<ProfessorContact>,
    onClose: () -> Unit,
    onEditEmail: (ProfessorContact) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
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
            shape = MaterialTheme.shapes.large,
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
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = filteredContacts,
                    key = { com.example.util.ProfessorEmail.nameKey(it.name) },
                    contentType = { "professor_card_item" }
                ) { professor ->
                    ProfessorCardItem(
                        professor = professor,
                        onCopyEmail = { email ->
                            clipboardManager.setText(AnnotatedString(email))
                            com.example.util.AppSnackbarManager.show("E-mail copiado!")
                        },
                        onSendEmail = { email ->
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Enviar e-mail"))
                            } catch (e: Exception) {
                                com.example.util.AppSnackbarManager.show("Nenhum app de e-mail encontrado")
                            }
                        },
                        onEditEmail = { onEditEmail(professor) }
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
    onSendEmail: (String) -> Unit,
    onEditEmail: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
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
                            shape = MaterialTheme.shapes.small,
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
                        MaterialTheme.shapes.medium
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
                        text = professor.email + if (professor.emailEstimated) " (estimado)" else "",
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
                        onClick = onEditEmail,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Corrigir E-mail",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onSendEmail(professor.email) },
                        modifier = Modifier.size(36.dp),
                        enabled = !professor.emailEstimated
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = if (professor.emailEstimated) "Envio indisponível: e-mail estimado" else "Enviar E-mail",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
