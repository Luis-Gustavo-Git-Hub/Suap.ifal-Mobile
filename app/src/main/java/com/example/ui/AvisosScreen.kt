package com.example.ui

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
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
    val rawErrorMessage by viewModel.avisosError.collectAsStateWithLifecycle()
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

    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val errorMessage = rawErrorMessage ?: (syncState as? com.example.sync.SyncState.Failed)?.let {
        com.example.network.SuapError.fromReason(it.reason).message
    }
    val syncTimes by viewModel.syncCoordinator.lastSyncedAt.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isLoading || syncState is com.example.sync.SyncState.Syncing,
        onRefresh = { viewModel.requestSync(force = true, module = com.example.sync.SyncModule.AVISOS) },
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
                .padding(horizontal = AppDimens.ScreenPadding, vertical = AppDimens.Spacing12)
        ) {
        val lastSyncTime = syncTimes[com.example.sync.SyncModule.AVISOS]?.let {
            java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
        }.orEmpty()
        com.example.ui.components.AppPageHeader(
            eyebrow = "SUAP",
            title = "Avisos",
            subtitle = "Comunicados e contatos dos seus professores.",
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailUnread,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            supportingContent = {
                if (syncState is com.example.sync.SyncState.Syncing) Text("Sincronizando...", style = MaterialTheme.typography.labelSmall)
                else if (lastSyncTime.isNotBlank()) {
                    com.example.ui.components.SyncTimestampBadge(rawTimestamp = lastSyncTime)
                }
            }
        )
        
        errorMessage?.takeIf { avisos.isEmpty() && syncState !is com.example.sync.SyncState.Syncing }?.let { message ->
            com.example.ui.components.AppFeedbackCard(
                title = "Não foi possível atualizar os avisos",
                message = if (avisos.isNotEmpty()) "$message Exibindo os avisos salvos no dispositivo." else message,
                modifier = Modifier.padding(vertical = AppDimens.Spacing8),
                actionLabel = "Tentar novamente",
                onAction = { viewModel.requestSync(force = true, module = com.example.sync.SyncModule.AVISOS) }
            )
        }

        val confirmedEmails = professorContacts.count { !it.emailEstimated && com.example.util.ProfessorEmail.isValid(it.email) }
        val estimatedEmails = professorContacts.count {
            it.emailEstimated || !com.example.util.ProfessorEmail.isValid(it.email)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.Spacing4),
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
                com.example.ui.components.AppTextButton(onClick = { showProfessorsSheet = true }, enabled = professorContacts.isNotEmpty()) {
                    Text("Ver todos")
                }
            }
            if (professorContacts.isNotEmpty()) LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
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
                modifier = Modifier.fillMaxWidth().padding(top = AppDimens.Spacing4),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    "Sincronize seus dados acadêmicos para carregar professores e e-mails.",
                    modifier = Modifier.padding(AppDimens.Spacing12),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.Spacing4, bottom = AppDimens.Spacing4),
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
                        modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        com.example.ui.components.AppTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.Spacing4),
            placeholder = { Text("Filtrar avisos...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
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

        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8), modifier = Modifier.padding(top = 2.dp, bottom = AppDimens.Spacing4)) {
            com.example.ui.components.AppFilterChip(
                selected = !showUnreadOnly,
                onClick = { showUnreadOnly = false },
                label = { Text("Todos (${avisos.size})") }
            )
            com.example.ui.components.AppFilterChip(
                selected = showUnreadOnly,
                onClick = { showUnreadOnly = true },
                label = { Text("Não lidos (${avisos.count { !it.isRead }})") },
                leadingIcon = if (avisos.any { !it.isRead }) ({ Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && filteredAvisos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.Spacing8),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
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
                        imageVector = Icons.Outlined.MarkEmailUnread,
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
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
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
        com.example.ui.components.AppBottomSheet(
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
        com.example.ui.components.AppDialog(
            onDismissRequest = { professorToEdit = null },
            title = { Text("E-mail de ${prof.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                    if (prof.emailEstimated) {
                        Text(
                            "O SUAP não informou o e-mail; o app estimou ${prof.email}. Digite o correto ou deixe em branco para manter a estimativa.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    com.example.ui.components.AppTextField(
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
                com.example.ui.components.AppTextButton(onClick = {
                    if (viewModel.setProfessorEmail(prof.name, emailText)) {
                        professorToEdit = null
                    }
                }, enabled = !invalidEmail) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_save)) }
            },
            dismissButton = {
                com.example.ui.components.AppTextButton(onClick = { professorToEdit = null }) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel)) }
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

    com.example.ui.components.AppContentCard(
        modifier = Modifier.width(290.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .background(cardBgGradient)
                .padding(AppDimens.Spacing12)
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
                        imageVector = Icons.Outlined.Email,
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
                        imageVector = Icons.Outlined.ContentCopy,
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
            com.example.network.SuapAuthRepository.getInstance(context).cookieHeaderFor("https://suap.ifal.edu.br")
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

    com.example.ui.components.AppContentCard(
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
        Column(modifier = Modifier.padding(AppDimens.Spacing12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
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
                        imageVector = Icons.Outlined.Campaign,
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
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
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
                            modifier = Modifier.padding(horizontal = AppDimens.Spacing4, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
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
                            .padding(vertical = AppDimens.Spacing12),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                    ) {
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp))
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                        com.example.ui.components.SkeletonBox(modifier = Modifier.fillMaxWidth().height(60.dp), shape = MaterialTheme.shapes.small)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
                        // Content card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(AppDimens.Spacing12)) {
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
                                    com.example.ui.components.AppTextButton(
                                        onClick = {
                                            messageDetail?.content?.let { content ->
                                                clipboardManager.setText(AnnotatedString(content))
                                                com.example.util.AppSnackbarManager.show("Conteúdo copiado!")
                                            }
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Copiar", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Share Button
                                    com.example.ui.components.AppTextButton(
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
                                        contentPadding = PaddingValues(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
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
                                modifier = Modifier.padding(horizontal = AppDimens.Spacing4)
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
                                        modifier = Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(accentColor.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AttachFile,
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
                                            imageVector = Icons.Outlined.KeyboardArrowRight,
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
            .padding(horizontal = AppDimens.ScreenPadding)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ContactMail,
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
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Fechar")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        com.example.ui.components.AppTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por professor ou disciplina...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpar busca")
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
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
                contentPadding = PaddingValues(bottom = AppDimens.Spacing32)
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
    com.example.ui.components.AppContentCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing16)) {
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
                                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = 3.dp)
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
                    .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onCopyEmail(professor.email) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
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
                            imageVector = Icons.Outlined.ContentCopy,
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
                            imageVector = Icons.Outlined.Edit,
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
                            imageVector = Icons.Outlined.Email,
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
