package com.example.ui

import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.Normalizer

private fun cachedFriendDateFormat(pattern: String) = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat(pattern, Locale.forLanguageTag("pt-BR"))
}

private val FRIEND_TIME_FORMATTER = cachedFriendDateFormat("HH:mm")
private val FRIEND_DATE_FORMATTER = cachedFriendDateFormat("dd/MM 'às' HH:mm")

private data class FriendSubjectMatch(
    val name: String,
    val mine: com.example.data.Subject?,
    val friend: Map<String, String>?
)

private fun subjectComparisonKey(name: String): String = Normalizer
    .normalize(name, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private fun matchFriendSubjects(
    mine: List<com.example.data.Subject>,
    friend: List<Map<String, String>>
): List<FriendSubjectMatch> {
    val mineByKey = mine.filter { subjectComparisonKey(it.name).isNotBlank() }
        .groupBy { subjectComparisonKey(it.name) }
    val friendByKey = friend.filter { subjectComparisonKey(it["name"].orEmpty()).isNotBlank() }
        .groupBy { subjectComparisonKey(it["name"].orEmpty()) }
    return (mineByKey.keys + friendByKey.keys).distinct().map { key ->
        val mineMatches = mineByKey[key].orEmpty()
        val friendMatches = friendByKey[key].orEmpty()
        val count = maxOf(mineMatches.size, friendMatches.size)
        (0 until count).map { index ->
            FriendSubjectMatch(
                name = mineMatches.getOrNull(index)?.name
                    ?: friendMatches.getOrNull(index)?.get("name").orEmpty(),
                mine = mineMatches.getOrNull(index),
                friend = friendMatches.getOrNull(index)
            )
        }
    }.flatten().sortedBy { it.name.lowercase(Locale.ROOT) }
}

val cardColorPresets = com.example.ui.theme.FriendCardColorPresets

fun formatLastUpdate(timestamp: Long): String {
    if (timestamp <= 0L) return "Sem notas atualizadas"
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    val date = Date(timestamp)
    val timeFormatter = checkNotNull(FRIEND_TIME_FORMATTER.get())

    return when {
        diffMinutes < 2 -> "Notas atualizadas agora"
        diffMinutes < 60 -> "Notas atualizadas há ${diffMinutes} min"
        diffHours < 24 -> "Notas atualizadas hoje às ${timeFormatter.format(date)}"
        diffDays == 1L -> "Notas atualizadas ontem às ${timeFormatter.format(date)}"
        else -> "Notas atualizadas em ${checkNotNull(FRIEND_DATE_FORMATTER.get()).format(date)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsUnavailableScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            com.example.ui.components.AppTopBar(
                title = { com.example.ui.components.AppBarTitle("Rede Acadêmica") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDimens.Spacing24),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Rede Acadêmica indisponível",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Amigos não está configurado nesta instalação. Você ainda pode usar o boletim, as aulas e os outros recursos do app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            com.example.ui.components.AppOutlineButton(onClick = onBack) { Text("Voltar ao app") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    if (!FriendsManager.isFirebaseConfigured(context)) {
        FriendsUnavailableScreen(onBack)
        return
    }

    val localSubjects by viewModel.subjects.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    var isSignedIn by remember { mutableStateOf(FriendsManager.isUserSignedIn()) }
    var signedInUserUid by remember {
        mutableStateOf(FriendsManager.currentUserId())
    }
    var isSigningIn by remember { mutableStateOf(false) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    val appPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var sharePhoto by remember { mutableStateOf(appPrefs.getBoolean("share_photo", true)) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var profileBase64Cache by remember { mutableStateOf("") }
    var myUserMatricula by remember { mutableStateOf("") }
    var myCardColorHex by remember { mutableStateOf("") }
    var searchMatricula by rememberSaveable { mutableStateOf("") }
    var listSearchQuery by rememberSaveable { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<Friend?>(null) }
    var customNameForAdd by remember { mutableStateOf("") }
    
    var showCompareSheet by rememberSaveable { mutableStateOf(false) }
    var compareSearchQuery by rememberSaveable { mutableStateOf("") }
    var showRequestsSheet by rememberSaveable { mutableStateOf(false) }
    var selectedFriendBoletim by remember { mutableStateOf<BoletimShared?>(null) }
    var selectedFriendForCompare by remember { mutableStateOf<Friend?>(null) }
    var isLoadingCompare by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var friendsLoadError by remember { mutableStateOf<String?>(null) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var friendToEdit by remember { mutableStateOf<Friend?>(null) }
    var customNameForEdit by remember { mutableStateOf("") }

    suspend fun refreshFriendsData() {
        isLoadingFriends = true
        friendsLoadError = null
        try {
            friendsList = FriendsManager.getFriends()
            pendingRequests = FriendsManager.getPendingRequests()
            myCardColorHex = FriendsManager.getMyCardColor()
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
            friendsLoadError = FriendsManager.userFacingError(e)
        } finally {
            isLoadingFriends = false
        }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            searchMatricula = result.contents
            isSearching = true
            coroutineScope.launch {
                try {
                    searchResult = FriendsManager.searchUserByEmail(searchMatricula)
                    if (searchResult == null) {
                        com.example.util.AppSnackbarManager.show("Nenhum usuário encontrado com esse e-mail.")
                    }
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                    com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                } finally {
                    isSearching = false
                }
            }
        }
    }

    LaunchedEffect(isSignedIn, signedInUserUid) {
        if (isSignedIn) {
            refreshFriendsData()
            
            val sharedPrefs = context.getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
            val savedMatricula = sharedPrefs.getString("saved_matricula", "") ?: ""
            
            myUserMatricula = savedMatricula
            
            if (savedMatricula.isNotEmpty() && savedMatricula != "-") {
                val savedPhotoUrl = sharedPrefs.getString("saved_profile_photo_url", "") ?: ""
                var base64 = ""
                if (savedPhotoUrl.isNotEmpty()) {
                    try {
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(savedPhotoUrl)
                            .size(96, 96)
                            .apply {
                                if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(savedPhotoUrl)) {
                                    addHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: "")
                                    addHeader("Referer", "https://suap.ifal.edu.br/")
                                }
                            }
                            .build()
                        val result = context.imageLoader.execute(request)
                        val drawable = result.drawable
                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                            val bitmap = drawable.bitmap
                            val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96 * bitmap.height / bitmap.width, true)
                            val outputStream = java.io.ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                            base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                        }
                    } catch (e: Exception) {
                        com.example.util.AppLog.printStackTrace(e)
                    }
                }
                profileBase64Cache = base64
                try {
                    FriendsManager.linkProfile(base64, sharePhoto)
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                    friendsLoadError = FriendsManager.userFacingError(e)
                }
            }
        }
    }
    
    LaunchedEffect(sharePhoto) {
        if (isSignedIn) {
            try {
                FriendsManager.linkProfile(profileBase64Cache, sharePhoto)
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                friendsLoadError = FriendsManager.userFacingError(e)
            }
        }
    }

    Scaffold(
        topBar = {
            com.example.ui.components.AppTopBar(
                title = {
                    com.example.ui.components.AppBarTitle(
                        title = "Amigos",
                        subtitle = if (isSignedIn) "${friendsList.size} conectados" else "Rede Acadêmica do IFAL"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    if (isSignedIn) {
                        IconButton(
                            onClick = { coroutineScope.launch { refreshFriendsData() } },
                            enabled = !isLoadingFriends
                        ) {
                            if (isLoadingFriends) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar amigos")
                            }
                        }
                        Surface(
                            onClick = { showAddDialog = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = AppDimens.Spacing4)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = "Adicionar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Adicionar",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { showRequestsSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (pendingRequests.isNotEmpty()) {
                                        Badge { Text(pendingRequests.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Solicitações",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Configurações",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
            if (!isSignedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState(), flingBehavior = rememberAppFlingBehavior())
                        .padding(AppDimens.Spacing16),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
                ) {
                    com.example.ui.components.AppPageHeader(
                        title = "Conecte-se com seus colegas",
                        subtitle = "Entre com sua conta Google para montar sua rede acadêmica, compartilhar atualizações do boletim e comparar disciplinas.",
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.PeopleAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    )

                    com.example.ui.components.AppCard {
                        Text(
                            text = "Sua rede acadêmica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(AppDimens.Spacing12))
                        FeatureRow(icon = Icons.Outlined.PersonAdd, text = "Adicione colegas por e-mail ou QR Code")
                        Spacer(Modifier.height(AppDimens.Spacing12))
                        FeatureRow(icon = Icons.Outlined.Sync, text = "Compartilhe atualizações do seu boletim")
                        Spacer(Modifier.height(AppDimens.Spacing12))
                        FeatureRow(icon = Icons.Outlined.Compare, text = "Compare disciplinas e médias")
                    }

                    com.example.ui.components.AppPrimaryButton(
                        onClick = {
                            isSigningIn = true
                            coroutineScope.launch {
                                try {
                                    FriendsManager.signInWithGoogle(context)
                                } catch (e: Exception) {
                                    com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                } finally {
                                    signedInUserUid = FriendsManager.currentUserId()
                                    isSignedIn = FriendsManager.isUserSignedIn()
                                    isSigningIn = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(AppDimens.Spacing8))
                            Text("Aguardando Google…")
                        } else {
                            Icon(Icons.Outlined.Login, contentDescription = null)
                            Spacer(Modifier.width(AppDimens.Spacing8))
                            Text("Entrar com Google")
                        }
                    }
                }
            } else {
                if (isLoadingFriends) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Carregando a rede de amigos...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Main Friends Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                    ) {
                        var isSyncing by remember { mutableStateOf(false) }

                        val mySelectedColor = remember(myCardColorHex) {
                            if (myCardColorHex.isNotBlank()) {
                                try { Color(android.graphics.Color.parseColor(myCardColorHex)) } catch (e: Exception) { null }
                            } else null
                        }

                        val filteredFriends = remember(friendsList, listSearchQuery) {
                            friendsList.filter { it.nome.contains(listSearchQuery, ignoreCase = true) || it.email.contains(listSearchQuery, ignoreCase = true) }
                        }

                        LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = AppDimens.Spacing16, end = AppDimens.Spacing16, top = AppDimens.Spacing8, bottom = AppDimens.Spacing24),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                        ) {
                            friendsLoadError?.let { errorMessage ->
                                item {
                                    com.example.ui.components.AppFeedbackCard(
                                        title = "Não foi possível carregar a Rede Acadêmica",
                                        message = errorMessage,
                                        actionLabel = "Tentar novamente",
                                        onAction = { coroutineScope.launch { refreshFriendsData() } }
                                    )
                                }
                            }
                            // 1. CARD COLOR CUSTOMIZER & SHARE BOLETIM BENTO CONTAINER
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = if (mySelectedColor != null) {
                                        if (isDark) mySelectedColor.copy(alpha = 0.22f) else mySelectedColor.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (mySelectedColor != null) mySelectedColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(AppDimens.Spacing16),
                                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                                    ) {
                                        // Top Section: Cor do Seu Card para Amigos
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Palette,
                                                    contentDescription = null,
                                                    tint = mySelectedColor ?: MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Cor do Seu Card na Rede",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = "Escolha a cor que seus amigos verão no seu perfil",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = (mySelectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = cardColorPresets.find { it.first == myCardColorHex }?.second?.first ?: "Personalizado",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = mySelectedColor ?: MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                                                )
                                            }
                                        }

                                        // Horizontal Swatch Color Palette Picker
                                        LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                                            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) {
                                            items(
                                                items = cardColorPresets,
                                                key = { it.first },
                                                contentType = { "color_preset" }
                                            ) { (hex, pair) ->
                                                val (label, color) = pair
                                                val isSelected = myCardColorHex == hex
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(if (color == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant else color)
                                                        .border(
                                                            if (isSelected) 3.dp else 1.dp,
                                                            if (isSelected) (if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else Color.White) else MaterialTheme.colorScheme.outlineVariant,
                                                            CircleShape
                                                        )
                                                        .clickable {
                                                            myCardColorHex = hex
                                                            coroutineScope.launch {
                                                                try {
                                                                    FriendsManager.updateMyCardColor(hex)
                                                                    friendsList = FriendsManager.getFriends()
                                                                    com.example.util.AppSnackbarManager.show("Cor do card atualizada!")
                                                                } catch (e: Exception) {
                                                                    com.example.util.AppLog.printStackTrace(e)
                                                                    com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Check,
                                                            contentDescription = "Selecionado",
                                                            tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = (mySelectedColor ?: MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.25f))

                                        // Share / Sync Grades Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = AppDimens.Spacing8)) {
                                                Text(
                                                    text = "Compartilhar Meu Boletim",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Envie suas notas atualizadas para a nuvem dos amigos",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            com.example.ui.components.AppPrimaryButton(
                                                onClick = {
                                                    isSyncing = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val sharedPrefs = context.getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
                                                            val matricula = sharedPrefs.getString("saved_matricula", "") ?: ""
                                                            
                                                            if (matricula.isNotEmpty() && matricula != "-") {
                                                                val subjects = viewModel.subjects.value
                                                                val simpleData = subjects.map {
                                                                    val parseGrade: (String?) -> Double? = { input ->
                                                                        if (input.isNullOrBlank() || input == "-") null
                                                                        else {
                                                                            val cleaned = input.trim().replace(',', '.')
                                                                            val value = cleaned.toDoubleOrNull()
                                                                            if (value == null || !value.isFinite() || value.isNaN()) null
                                                                            else if (value < 0.0) 0.0 else value
                                                                        }
                                                                    }
                                                                    val bn1 = parseGrade(it.grade1)
                                                                    val bn2 = parseGrade(it.grade2)
                                                                    val bn3 = parseGrade(it.grade3)
                                                                    val bn4 = parseGrade(it.grade4)
                                                                    val validGrades = listOfNotNull(bn1, bn2, bn3, bn4)
                                                                    val media = if (validGrades.isNotEmpty()) validGrades.sum() / validGrades.size else 0.0
                                                                    var fGrade = it.finalGrade ?: ""
                                                                    if (fGrade.isEmpty() && media > 0) {
                                                                        fGrade = String.format(Locale.US, "%.1f", media)
                                                                    }

                                                                    mapOf(
                                                                        "name" to (it.name ?: ""),
                                                                        "grade1" to (it.grade1 ?: ""),
                                                                        "grade2" to (it.grade2 ?: ""),
                                                                        "grade3" to (it.grade3 ?: ""),
                                                                        "grade4" to (it.grade4 ?: ""),
                                                                        "finalGrade" to fGrade,
                                                                        "status" to (it.status ?: "")
                                                                    )
                                                                }
                                                                FriendsManager.shareBoletim(simpleData)
                                                                friendsList = FriendsManager.getFriends()
                                                                com.example.util.AppSnackbarManager.show("Boletim compartilhado com amigos!")
                                                            } else {
                                                                com.example.util.AppSnackbarManager.show("Faça login no SUAP primeiro para carregar notas.")
                                                            }
                                                        } catch (e: Exception) {
                                                            com.example.util.AppLog.printStackTrace(e)
                                                            com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                        } finally {
                                                            isSyncing = false
                                                        }
                                                    }
                                                },
                                                shape = MaterialTheme.shapes.medium,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = mySelectedColor ?: MaterialTheme.colorScheme.primary
                                                ),
                                                enabled = !isSyncing
                                            ) {
                                                if (isSyncing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Enviando...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Sync,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Atualizar Notas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. SEARCH & FILTER BAR
                            item {
                                com.example.ui.components.AppTextField(
                                    value = listSearchQuery,
                                    onValueChange = { listSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Pesquisar amigos na lista...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Pesquisar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (listSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { listSearchQuery = "" }) {
                                                Icon(Icons.Outlined.Clear, contentDescription = "Limpar")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }

                            if (friendsLoadError == null && filteredFriends.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PersonSearch,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = if (friendsList.isEmpty()) "Sua lista de amigos está vazia" else "Nenhum amigo encontrado",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (friendsList.isEmpty()) "Adicione colegas via e-mail ou QR Code para acompanhar boletins!" else "Tente buscar com outro termo.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            if (friendsList.isEmpty()) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                com.example.ui.components.AppPrimaryButton(
                                                    onClick = { showAddDialog = true },
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Adicionar Amigos", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = filteredFriends,
                                    key = { it.uid },
                                    contentType = { "friend_card" }
                                ) { friend ->
                                    FriendItemCard(
                                        friend = friend,
                                        onClick = {
                                            selectedFriendForCompare = friend
                                            selectedFriendBoletim = null
                                            compareSearchQuery = ""
                                            showCompareSheet = true
                                            isLoadingCompare = true
                                            coroutineScope.launch {
                                                try {
                                                    selectedFriendBoletim = FriendsManager.getSharedBoletim(friend.uid)
                                                } catch (e: Exception) {
                                                    com.example.util.AppLog.printStackTrace(e)
                                                    com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                } finally {
                                                    isLoadingCompare = false
                                                }
                                            }
                                        },
                                        onRemoveClick = { friendToRemove = friend },
                                        onEditClick = {
                                            friendToEdit = friend
                                            customNameForEdit = friend.nome
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Requests Sheet Modal
        if (showRequestsSheet) {
            com.example.ui.components.AppBottomSheet(onDismissRequest = { showRequestsSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.Spacing16)
                        .padding(bottom = AppDimens.Spacing32),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
                ) {
                    Text(
                        text = "Solicitações de Amizade",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (pendingRequests.isEmpty()) {
                        Text(
                            text = "Nenhuma solicitação pendente no momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                flingBehavior = rememberAppFlingBehavior(), verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                            items(
                                items = pendingRequests,
                                key = { it.uid },
                                contentType = { "pending_request" }
                            ) { req ->
                                var reqName by remember { mutableStateOf("") }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(AppDimens.Spacing12),
                                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                                        ) {
                                            Base64Image(
                                                base64String = req.photoBase64,
                                                modifier = Modifier.size(42.dp).clip(CircleShape),
                                                contentDescription = null
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = req.email,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Quer adicionar você como amigo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        com.example.ui.components.AppTextField(
                                            value = reqName,
                                            onValueChange = { reqName = it },
                                            label = { Text("Apelido para este amigo *") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.medium
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            com.example.ui.components.AppTextButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        FriendsManager.declineFriendRequest(req.uid)
                                                        pendingRequests = FriendsManager.getPendingRequests()
                                                    } catch (e: Exception) {
                                                        com.example.util.AppLog.printStackTrace(e)
                                                        com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                    }
                                                }
                                            }) {
                                                Text("Recusar", color = MaterialTheme.colorScheme.error)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            com.example.ui.components.AppPrimaryButton(
                                                onClick = {
                                                    if (reqName.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            try {
                                                                FriendsManager.acceptFriendRequest(req.uid, reqName.trim())
                                                                pendingRequests = FriendsManager.getPendingRequests()
                                                                friendsList = FriendsManager.getFriends()
                                                                com.example.util.AppSnackbarManager.show("Solicitação aceita.")
                                                            } catch (e: Exception) {
                                                                com.example.util.AppLog.printStackTrace(e)
                                                                com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                            }
                                                        }
                                                    } else {
                                                        com.example.util.AppSnackbarManager.show("Insira um apelido!")
                                                    }
                                                },
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                Text("Aceitar", fontWeight = FontWeight.Bold)
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

        // Settings Dialog (Includes Profile Photo Toggle)
        if (showSettingsSheet) {
            com.example.ui.components.AppDialog(
                onDismissRequest = { showSettingsSheet = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Configurações da Rede",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ajuste suas preferências de privacidade e compartilhamento com outros colegas da rede acadêmica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Connected Account Card
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        val currentGoogleEmail = FriendsManager.currentGoogleAccountEmail()
                        if (currentUser != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(AppDimens.Spacing12),
                                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Conta Google Conectada",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = currentGoogleEmail ?: "Conta ativada",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Share Photo Switch
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(AppDimens.Spacing12),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = AppDimens.Spacing8)) {
                                    Text(
                                        text = "Compartilhar Foto",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Permite que seus amigos vejam sua foto de perfil do SUAP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = sharePhoto,
                                    onCheckedChange = {
                                        sharePhoto = it
                                        appPrefs.edit().putBoolean("share_photo", it).apply()
                                    }
                                )
                            }
                        }

                        // Switch Google Account Option
                        if (isSignedIn) {
                            com.example.ui.components.AppPrimaryButton(
                                onClick = {
                                    showSettingsSheet = false
                                    val previousEmail = FriendsManager.currentGoogleAccountEmail()
                                    isSigningIn = true
                                    coroutineScope.launch {
                                        try {
                                            var switchFailed = false
                                            val switchResult = try {
                                                FriendsManager.changeGoogleAccount(context)
                                            } catch (e: Exception) {
                                                switchFailed = true
                                                com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                                null
                                            }
                                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            signedInUserUid = currentUser?.uid
                                            isSignedIn = currentUser != null
                                            if (!switchFailed) {
                                                when {
                                                    switchResult == null -> com.example.util.AppSnackbarManager.show("Nenhuma conta foi selecionada. Sua conta atual foi mantida.")
                                                    !switchResult.profileUpdated -> com.example.util.AppSnackbarManager.show("Conta conectada como ${switchResult.email}, mas o perfil no Firebase não foi atualizado.")
                                                    !switchResult.email.equals(previousEmail, ignoreCase = true) -> com.example.util.AppSnackbarManager.show("Conta conectada: ${switchResult.email}")
                                                    else -> com.example.util.AppSnackbarManager.show("Essa conta já estava conectada: ${switchResult.email}")
                                                }
                                            }
                                        } finally {
                                            isSigningIn = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                enabled = !isSigningIn
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwitchAccount,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trocar de Conta Google", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Disconnect Google Account Option
                        if (isSignedIn) {
                            com.example.ui.components.AppOutlineButton(
                                onClick = {
                                    coroutineScope.launch {
                                        FriendsManager.signOut(context)
                                        signedInUserUid = null
                                        isSignedIn = false
                                        showSettingsSheet = false
                                        com.example.util.AppSnackbarManager.show("Conta do Google desconectada com sucesso.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Desconectar Conta do Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    com.example.ui.components.AppPrimaryButton(
                        onClick = { showSettingsSheet = false },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_close), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Friend Custom Name Dialog
        friendToEdit?.let { friend ->
            com.example.ui.components.AppDialog(
                onDismissRequest = { friendToEdit = null },
                title = { Text("Apelido do Amigo", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                        Text("Defina um apelido personalizado para identificar seu amigo:")
                        com.example.ui.components.AppTextField(
                            value = customNameForEdit,
                            onValueChange = { customNameForEdit = it },
                            label = { Text("Apelido") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                },
                confirmButton = {
                    com.example.ui.components.AppTextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                FriendsManager.updateFriendName(friend.uid, customNameForEdit)
                                friendsList = FriendsManager.getFriends()
                            } catch (e: Exception) {
                                com.example.util.AppLog.printStackTrace(e)
                                com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                            } finally {
                                friendToEdit = null
                            }
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    com.example.ui.components.AppTextButton(onClick = { friendToEdit = null }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel))
                    }
                }
            )
        }
        
        // Remove Friend Confirmation Dialog
        friendToRemove?.let { friend ->
            com.example.ui.components.AppDialog(
                onDismissRequest = { friendToRemove = null },
                title = { Text("Remover Amigo", fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja remover ${friend.nome}? Vocês não poderão mais visualizar os boletins um do outro.") },
                confirmButton = {
                    com.example.ui.components.AppTextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                FriendsManager.removeFriend(friend.uid)
                                friendsList = FriendsManager.getFriends()
                            } catch (e: Exception) {
                                com.example.util.AppLog.printStackTrace(e)
                                com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                            } finally {
                                friendToRemove = null
                            }
                        }
                    }) {
                        Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    com.example.ui.components.AppTextButton(onClick = { friendToRemove = null }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel))
                    }
                }
            )
        }

        // Add Friend Dialog
        if (showAddDialog) {
            com.example.ui.components.AppDialog(
                onDismissRequest = {
                    showAddDialog = false
                    searchResult = null
                    searchMatricula = ""
                    customNameForAdd = ""
                },
                title = { Text("Adicionar Colega", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                        Text("Insira o e-mail Google do seu amigo ou escaneie o QR Code dele:")
                        com.example.ui.components.AppTextField(
                            value = searchMatricula,
                            onValueChange = { searchMatricula = it },
                            label = { Text("E-mail Google do Amigo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val options = ScanOptions()
                                    options.setPrompt("Digitalize o QR Code do Amigo")
                                    options.setBeepEnabled(true)
                                    options.setOrientationLocked(true)
                                    scanLauncher.launch(options)
                                }) {
                                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Escanear QR Code")
                                }
                            }
                        )

                        com.example.ui.components.AppPrimaryButton(
                            onClick = {
                                if (searchMatricula.isNotBlank()) {
                                    isSearching = true
                                    coroutineScope.launch {
                                        try {
                                            searchResult = FriendsManager.searchUserByEmail(searchMatricula)
                                            if (searchResult == null) {
                                                com.example.util.AppSnackbarManager.show("Nenhum usuário encontrado com esse e-mail.")
                                            }
                                        } catch (e: Exception) {
                                            com.example.util.AppLog.printStackTrace(e)
                                            com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Buscar Usuário")
                            }
                        }

                        searchResult?.let { found ->
                            HorizontalDivider()
                            Text("Usuário encontrado!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            com.example.ui.components.AppTextField(
                                value = customNameForAdd,
                                onValueChange = { customNameForAdd = it },
                                label = { Text("Apelido para este amigo *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                },
                confirmButton = {
                    if (searchResult != null) {
                        com.example.ui.components.AppTextButton(
                            onClick = {
                                if (customNameForAdd.isNotBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            FriendsManager.sendFriendRequest(searchResult!!.uid, customNameForAdd.trim())
                                            com.example.util.AppSnackbarManager.show("Solicitação enviada!")
                                            friendsList = FriendsManager.getFriends()
                                        } catch (e: Exception) {
                                            com.example.util.AppLog.printStackTrace(e)
                                            com.example.util.AppSnackbarManager.show(FriendsManager.userFacingError(e))
                                            return@launch
                                        }
                                        showAddDialog = false
                                        searchResult = null
                                        searchMatricula = ""
                                        customNameForAdd = ""
                                    }
                                }
                            }
                        ) {
                            Text("Enviar Pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    com.example.ui.components.AppTextButton(onClick = {
                        showAddDialog = false
                        searchResult = null
                        searchMatricula = ""
                        customNameForAdd = ""
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_cancel))
                    }
                }
            )
        }

        // Compare Sheet
        if (showCompareSheet) {
            com.example.ui.components.AppBottomSheet(onDismissRequest = { showCompareSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.ScreenPadding)
                        .padding(bottom = AppDimens.Spacing32),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Comparativo do Boletim", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    
                    selectedFriendForCompare?.let { friend ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatLastUpdate(friend.lastUpdateTimestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoadingCompare) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(32.dp))
                    } else if (selectedFriendBoletim == null) {
                        Text("Este amigo ainda não compartilhou o boletim no app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        com.example.ui.components.AppTextField(
                            value = compareSearchQuery,
                            onValueChange = { compareSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppDimens.Spacing12),
                            placeholder = { Text("Pesquisar disciplina...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Pesquisar") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        FriendComparisonContent(
                            localSubjects = localSubjects,
                            friendSubjects = selectedFriendBoletim!!.disciplinas,
                            searchQuery = compareSearchQuery
                        )

                    }
                }
            }
        }
    }
}


private data class FriendGradePoint(val stage: String, val mine: Double?, val friend: Double?)

private fun parseComparisonGrade(value: String?): Double? = value
    ?.trim()
    ?.replace(",", ".")
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() && it in 0.0..10.0 }

private fun FriendSubjectMatch.gradePoints(): List<FriendGradePoint> {
    val friendKeys = listOf("grade1", "grade2", "grade3", "grade4")
    val mineGrades = listOf(mine?.grade1, mine?.grade2, mine?.grade3, mine?.grade4)
    return friendKeys.mapIndexed { index, key ->
        FriendGradePoint(
            stage = "B${index + 1}",
            mine = parseComparisonGrade(mineGrades[index]),
            friend = parseComparisonGrade(friend?.get(key))
        )
    }
}

@Composable
private fun FriendComparisonContent(
    localSubjects: List<com.example.data.Subject>,
    friendSubjects: List<Map<String, String>>,
    searchQuery: String
) {
    val matches = remember(localSubjects, friendSubjects) {
        matchFriendSubjects(localSubjects, friendSubjects)
    }
    val normalizedQuery = remember(searchQuery) { subjectComparisonKey(searchQuery) }
    val visible = remember(matches, normalizedQuery) {
        matches.filter {
            normalizedQuery.isBlank() || subjectComparisonKey(it.name).contains(normalizedQuery)
        }
    }
    val common = visible.filter { it.mine != null && it.friend != null }
    val mineOnly = visible.filter { it.mine != null && it.friend == null }
    val friendOnly = visible.filter { it.mine == null && it.friend != null }
    val gradePairs = common.flatMap { it.gradePoints() }
    val mineAverage = gradePairs.mapNotNull { it.mine }.average().takeIf { it.isFinite() }
    val friendAverage = gradePairs.mapNotNull { it.friend }.average().takeIf { it.isFinite() }

    if (localSubjects.isEmpty()) {
        com.example.ui.components.AppFeedbackCard(
            title = "Suas notas não estão carregadas",
            message = "Entre no SUAP e sincronize o boletim para comparar suas disciplinas com as do amigo."
        )
        return
    }

    if (visible.isEmpty()) {
        com.example.ui.components.AppEmptyState("Nenhuma disciplina encontrada para essa busca.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
        flingBehavior = rememberAppFlingBehavior(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
        contentPadding = PaddingValues(bottom = AppDimens.Spacing16)
    ) {
        item {
            com.example.ui.components.AppCard {
                Text("Visão geral", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${common.size} ${if (common.size == 1) "disciplina em comum" else "disciplinas em comum"}. Só comparamos nomes equivalentes; matérias exclusivas de cada curso ficam separadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(AppDimens.Spacing12))
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                    ComparisonMetric("Sua média", mineAverage, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    ComparisonMetric("Média do amigo", friendAverage, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }

        if (common.isNotEmpty()) {
            item {
                com.example.ui.components.AppSectionHeader(
                    title = "Comparação por etapa",
                    action = { Text("0–10", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
            items(common, key = { "common:${subjectComparisonKey(it.name)}:${it.mine?.id.orEmpty()}:${it.friend?.hashCode()}" }) { match ->
                FriendSubjectChartCard(match)
            }
        } else {
            item {
                com.example.ui.components.AppFeedbackCard(
                    title = "Nenhuma matéria em comum",
                    message = "Os nomes das disciplinas não coincidem entre os boletins. Para evitar uma comparação errada, elas não foram agrupadas."
                )
            }
        }

        if (mineOnly.isNotEmpty()) {
            item { UnmatchedSubjectsCard("Só no seu boletim", mineOnly.map { it.name }) }
        }
        if (friendOnly.isNotEmpty()) {
            item { UnmatchedSubjectsCard("Só no boletim do amigo", friendOnly.map { it.name }) }
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, value: Double?, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.10f)
    ) {
        Column(Modifier.padding(AppDimens.Spacing12)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it) } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun FriendSubjectChartCard(match: FriendSubjectMatch) {
    val points = remember(match) { match.gradePoints() }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    com.example.ui.components.AppCard {
        Text(match.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(AppDimens.Spacing8))
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)) {
            ChartLegend(primary, "Você")
            ChartLegend(secondary, "Amigo")
        }
        Canvas(
            modifier = Modifier.fillMaxWidth().height(120.dp).semantics {
                val detail = points.joinToString { point ->
                    "${point.stage}: você ${point.mine?.let { String.format(Locale.US, "%.1f", it) } ?: "sem nota"}, " +
                        "amigo ${point.friend?.let { String.format(Locale.US, "%.1f", it) } ?: "sem nota"}"
                }
                contentDescription = "Notas de ${match.name}. $detail"
            }
        ) {
            val left = 10.dp.toPx()
            val right = size.width - 10.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val chartHeight = bottom - top
            for (value in listOf(0, 5, 10)) {
                val y = bottom - chartHeight * value / 10f
                drawLine(gridColor.copy(alpha = 0.55f), Offset(left, y), Offset(right, y), 1.dp.toPx())
            }
            val groupWidth = (right - left) / points.size
            val barWidth = minOf(13.dp.toPx(), groupWidth * 0.24f)
            points.forEachIndexed { index, point ->
                val center = left + groupWidth * (index + 0.5f)
                listOf(point.mine to primary, point.friend to secondary).forEachIndexed { side, entry ->
                    val (grade, color) = entry
                    if (grade != null) {
                        val height = chartHeight * (grade / 10.0).toFloat()
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(center + (side - 0.5f) * barWidth - barWidth / 2f, bottom - height),
                            size = androidx.compose.ui.geometry.Size(barWidth, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            points.forEach { point ->
                Text(point.stage, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(AppDimens.Spacing4))
        Text(
            points.joinToString("   ·   ") { point ->
                "${point.stage}: ${point.mine?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it) } ?: "—"} / ${point.friend?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it) } ?: "—"}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
        Surface(Modifier.size(10.dp), shape = CircleShape, color = color) {}
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnmatchedSubjectsCard(title: String, subjects: List<String>) {
    com.example.ui.components.AppCard {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        subjects.forEach { subject ->
            Text("• $subject", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FriendItemCard(
    friend: Friend,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val xp = friend.xp
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val customCardColor = remember(friend.cardColorHex) {
        if (friend.cardColorHex.isNotBlank() && friend.cardColorHex != "#00000000") {
            try {
                Color(android.graphics.Color.parseColor(friend.cardColorHex))
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val cardBg = if (customCardColor != null) {
        if (isDark) customCardColor.copy(alpha = 0.22f) else customCardColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorderColor = if (customCardColor != null) {
        customCardColor.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    
    val totalSubjects = friend.sharedSubjectCount.takeIf { it > 0 } ?: 10
    val lvl2 = totalSubjects * 70
    val lvl3 = totalSubjects * 140
    val lvl4 = totalSubjects * 230
    val lvl5 = totalSubjects * 320

    val globalLevel = when {
        xp >= lvl5 -> 5
        xp >= lvl4 -> 4
        xp >= lvl3 -> 3
        xp >= lvl2 -> 2
        else -> 1
    }
    
    val badgeColor = when (globalLevel) {
        1 -> AppSemanticColors.success
        2 -> MaterialTheme.colorScheme.primary
        3 -> MaterialTheme.colorScheme.tertiary
        4 -> AppSemanticColors.warning
        5 -> MaterialTheme.colorScheme.secondary
        else -> Color.Gray
    }
    
    val emblemName = when (globalLevel) {
        1 -> "Calouro"
        2 -> "Aprendiz"
        3 -> "Elite"
        4 -> "Mestre"
        5 -> "Lenda"
        else -> "Calouro"
    }

    val badgeIcon = when (globalLevel) {
        1 -> Icons.Outlined.LocalFlorist
        2 -> Icons.Outlined.MenuBook
        3 -> Icons.Outlined.School
        4 -> Icons.Outlined.Psychology
        5 -> Icons.Outlined.WorkspacePremium
        else -> Icons.Outlined.Star
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = cardBg,
        border = BorderStroke(if (customCardColor != null) 1.5.dp else 1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            // Header Row: Avatar, Name/Email, Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Base64Image(
                    base64String = friend.photoBase64,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            customCardColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentDescription = "Foto de ${friend.nome}"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                    ) {
                        Text(
                            text = friend.nome.ifEmpty { "Amigo sem nome" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customCardColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(customCardColor)
                            )
                        }
                    }
                    Text(
                        text = friend.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onEditClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar Apelido",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onRemoveClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PersonRemove,
                                contentDescription = "Remover Amigo",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = cardBorderColor.copy(alpha = 0.3f))

            // LAST UPDATE DATE BADGE (DATE OF LAST GRADE UPDATE FOR FRIEND) - FULL WIDTH TO PREVENT OVERFLOW/CUT-OFF
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = (customCardColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, (customCardColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Última atualização de notas",
                        modifier = Modifier.size(15.dp),
                        tint = customCardColor ?: MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatLastUpdate(friend.lastUpdateTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = customCardColor ?: MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // XP & LEVEL BADGES ROW (DISTRUBUTED BELOW TO COMPLETELY PREVENT CUT-OFF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // XP Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${xp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                  
                // Emblem Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor.copy(alpha = 0.15f),
                    contentColor = badgeColor,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(badgeIcon, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = emblemName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Button: Comparar Notas
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = customCardColor ?: MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (customCardColor != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comparar Notas com ${friend.nome.split(" ").firstOrNull() ?: "Amigo"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun Base64Image(base64String: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val decodedBitmap = remember(base64String) {
        if (base64String.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
