package com.example.ui

import com.example.ui.theme.AppSemanticColors

import com.example.ui.theme.rememberAppFlingBehavior

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val cardColorPresets = listOf(
    "" to ("Padrão M3" to Color.Unspecified),
    "#E91E63" to ("Rosa" to Color(0xFFE91E63)),
    "#8B5CF6" to ("Roxo" to Color(0xFF8B5CF6)),
    "#3B82F6" to ("Azul" to Color(0xFF3B82F6)),
    "#06B6D4" to ("Ciano" to Color(0xFF06B6D4)),
    "#10B981" to ("Verde" to Color(0xFF10B981)),
    "#F59E0B" to ("Amarelo" to Color(0xFFF59E0B)),
    "#F97316" to ("Laranja" to Color(0xFFF97316)),
    "#EF4444" to ("Vermelho" to Color(0xFFEF4444)),
    "#1E293B" to ("Grafite" to Color(0xFF1E293B))
)

fun formatLastUpdate(timestamp: Long): String {
    if (timestamp <= 0L) return "Sem notas atualizadas"
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    val sdfTime = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    val sdfDate = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))

    return when {
        diffMinutes < 2 -> "Notas atualizadas agora"
        diffMinutes < 60 -> "Notas atualizadas há ${diffMinutes} min"
        diffHours < 24 -> "Notas atualizadas hoje às ${sdfTime.format(Date(timestamp))}"
        diffDays == 1L -> "Notas atualizadas ontem às ${sdfTime.format(Date(timestamp))}"
        else -> "Notas atualizadas em ${sdfDate.format(Date(timestamp))}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit
) {
    val localSubjects by viewModel.subjects.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isSignedIn by remember { mutableStateOf(FriendsManager.isUserSignedIn()) }
    var signedInUserUid by remember {
        mutableStateOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
    }
    var isSigningIn by remember { mutableStateOf(false) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    val appPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var sharePhoto by remember { mutableStateOf(appPrefs.getBoolean("share_photo", true)) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var profileBase64Cache by remember { mutableStateOf("") }
    var myUserMatricula by remember { mutableStateOf("") }
    var myUserEmail by remember { mutableStateOf("") }
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
    var myBoletim by remember { mutableStateOf<BoletimShared?>(null) }
    var isLoadingCompare by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var friendToEdit by remember { mutableStateOf<Friend?>(null) }
    var customNameForEdit by remember { mutableStateOf("") }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            searchMatricula = result.contents
            isSearching = true
            coroutineScope.launch {
                searchResult = FriendsManager.searchUserByEmail(searchMatricula)
                isSearching = false
            }
        }
    }

    LaunchedEffect(isSignedIn, signedInUserUid) {
        if (isSignedIn) {
            isLoadingFriends = true
            friendsList = FriendsManager.getFriends()
            pendingRequests = FriendsManager.getPendingRequests()
            myCardColorHex = FriendsManager.getMyCardColor()
            isLoadingFriends = false
            
            val sharedPrefs = context.getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
            val savedMatricula = sharedPrefs.getString("saved_matricula", "") ?: ""
            
            myUserMatricula = savedMatricula
            
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            if (uid.isNotEmpty()) {
                try {
                    val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                    myUserEmail = userDoc.getString("email")?.takeIf { it.isNotEmpty() } ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
                } catch (e: Exception) {
                    myUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
                }
            } else {
                myUserEmail = ""
            }
            
            if (savedMatricula.isNotEmpty() && savedMatricula != "-") {
                val savedPhotoUrl = sharedPrefs.getString("saved_profile_photo_url", "") ?: ""
                var base64 = ""
                if (savedPhotoUrl.isNotEmpty()) {
                    try {
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(savedPhotoUrl)
                            .apply {
                                if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(savedPhotoUrl)) {
                                    addHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: "")
                                    addHeader("Referer", "https://suap.ifal.edu.br/")
                                }
                            }
                            .build()
                        val imageLoader = coil.ImageLoader(context)
                        val result = imageLoader.execute(request)
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
                FriendsManager.linkProfile(base64, sharePhoto)
            }
        }
    }
    
    LaunchedEffect(sharePhoto) {
        if (isSignedIn) {
            FriendsManager.linkProfile(profileBase64Cache, sharePhoto)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Rede Acadêmica",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isSignedIn) "${friendsList.size} amigos conectados" else "Conecte-se e compare notas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                actions = {
                    if (isSignedIn) {
                        Surface(
                            onClick = { showAddDialog = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
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
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Solicitações",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
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
                // Google Sign-In Hero Bento
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState(), flingBehavior = rememberAppFlingBehavior())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleAlt,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Rede Acadêmica do IFAL",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Conecte sua conta do Google para adicionar amigos, personalizar a cor do seu card, acompanhar atualizações de notas e comparar seu desempenho acadêmico.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureRow(icon = Icons.Default.Sync, text = "Sincronização em tempo real das notas do SUAP")
                            FeatureRow(icon = Icons.Default.Palette, text = "Personalização de cor do seu card para amigos")
                            FeatureRow(icon = Icons.Default.AccessTime, text = "Data e hora exatas de atualização do boletim")
                            FeatureRow(icon = Icons.Default.Compare, text = "Comparação detalhada de disciplinas e médias")
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isSigningIn = true
                            coroutineScope.launch {
                                val success = try {
                                    FriendsManager.signInWithGoogle(context)
                                } catch (e: Exception) {
                                    com.example.util.AppSnackbarManager.show("Erro no Google Sign-In")
                                    false
                                }
                                if (!success) {
                                    com.example.util.AppSnackbarManager.show("Falha ao fazer login com o Google.")
                                }
                                signedInUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                isSignedIn = success
                                isSigningIn = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Entrando...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Entrar com a Conta Google", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Top Section: Cor do Seu Card para Amigos
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Palette,
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
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // Horizontal Swatch Color Palette Picker
                                        LazyRow(
                flingBehavior = rememberAppFlingBehavior(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                                                FriendsManager.updateMyCardColor(hex)
                                                                friendsList = FriendsManager.getFriends()
                                                                com.example.util.AppSnackbarManager.show("Cor do card atualizada!")
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
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
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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

                                            Button(
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
                                                            com.example.util.AppSnackbarManager.show("Erro ao compartilhar boletim.")
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
                                                        imageVector = Icons.Default.Sync,
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
                                OutlinedTextField(
                                    value = listSearchQuery,
                                    onValueChange = { listSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Pesquisar amigos na lista...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Pesquisar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (listSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { listSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }

                            if (filteredFriends.isEmpty()) {
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
                                                imageVector = Icons.Default.PersonSearch,
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
                                                Button(
                                                    onClick = { showAddDialog = true },
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                            showCompareSheet = true
                                            isLoadingCompare = true
                                            coroutineScope.launch {
                                                try {
                                                    selectedFriendBoletim = FriendsManager.getSharedBoletim(friend.uid)
                                                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                                    if (myUid != null) {
                                                        myBoletim = FriendsManager.getSharedBoletim(myUid)
                                                    }
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
            ModalBottomSheet(onDismissRequest = { showRequestsSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                flingBehavior = rememberAppFlingBehavior(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                                        OutlinedTextField(
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
                                            TextButton(onClick = {
                                                coroutineScope.launch {
                                                    FriendsManager.declineFriendRequest(req.uid)
                                                    pendingRequests = FriendsManager.getPendingRequests()
                                                }
                                            }) {
                                                Text("Recusar", color = MaterialTheme.colorScheme.error)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (reqName.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            FriendsManager.acceptFriendRequest(req.uid, reqName.trim())
                                                            pendingRequests = FriendsManager.getPendingRequests()
                                                            friendsList = FriendsManager.getFriends()
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
            AlertDialog(
                onDismissRequest = { showSettingsSheet = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ajuste suas preferências de privacidade e compartilhamento com outros colegas da rede acadêmica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Connected Account Card
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
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
                                        text = currentUser.email ?: "Conta ativada",
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
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                            Button(
                                onClick = {
                                    showSettingsSheet = false
                                    coroutineScope.launch {
                                        val newEmail = FriendsManager.changeGoogleAccount(context)
                                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        signedInUserUid = currentUser?.uid
                                        isSignedIn = currentUser != null
                                        if (newEmail != null && currentUser != null) {
                                            com.example.util.AppSnackbarManager.show("Conta alterada para: $newEmail")
                                        } else {
                                            com.example.util.AppSnackbarManager.show("Troca de conta não concluída.")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwitchAccount,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trocar de Conta Google", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Disconnect Google Account Option
                        if (isSignedIn) {
                            OutlinedButton(
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
                                    imageVector = Icons.Default.Logout,
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
                    Button(
                        onClick = { showSettingsSheet = false },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Friend Custom Name Dialog
        friendToEdit?.let { friend ->
            AlertDialog(
                onDismissRequest = { friendToEdit = null },
                title = { Text("Apelido do Amigo", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Defina um apelido personalizado para identificar seu amigo:")
                        OutlinedTextField(
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
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                FriendsManager.updateFriendName(friend.uid, customNameForEdit)
                                friendsList = FriendsManager.getFriends()
                            } catch (e: Exception) {
                                com.example.util.AppLog.printStackTrace(e)
                            } finally {
                                friendToEdit = null
                            }
                        }
                    }) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { friendToEdit = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Remove Friend Confirmation Dialog
        friendToRemove?.let { friend ->
            AlertDialog(
                onDismissRequest = { friendToRemove = null },
                title = { Text("Remover Amigo", fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja remover ${friend.nome}? Vocês não poderão mais visualizar os boletins um do outro.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                FriendsManager.removeFriend(friend.uid)
                                friendsList = FriendsManager.getFriends()
                            } catch (e: Exception) {
                                com.example.util.AppLog.printStackTrace(e)
                            } finally {
                                friendToRemove = null
                            }
                        }
                    }) {
                        Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { friendToRemove = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Add Friend Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    searchResult = null
                    searchMatricula = ""
                    customNameForAdd = ""
                },
                title = { Text("Adicionar Colega", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Insira o e-mail Google do seu amigo ou escaneie o QR Code dele:")
                        OutlinedTextField(
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
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR Code")
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (searchMatricula.isNotBlank()) {
                                    isSearching = true
                                    coroutineScope.launch {
                                        searchResult = FriendsManager.searchUserByEmail(searchMatricula.trim())
                                        isSearching = false
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
                            OutlinedTextField(
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
                        TextButton(
                            onClick = {
                                if (customNameForAdd.isNotBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            FriendsManager.sendFriendRequest(searchResult!!.uid, customNameForAdd.trim())
                                            com.example.util.AppSnackbarManager.show("Solicitação enviada!")
                                            friendsList = FriendsManager.getFriends()
                                        } catch (e: Exception) {
                                            com.example.util.AppLog.printStackTrace(e)
                                        } finally {
                                            showAddDialog = false
                                            searchResult = null
                                            searchMatricula = ""
                                            customNameForAdd = ""
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Enviar Pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        searchResult = null
                        searchMatricula = ""
                        customNameForAdd = ""
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Compare Sheet
        if (showCompareSheet) {
            ModalBottomSheet(onDismissRequest = { showCompareSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Comparativo do Boletim", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    
                    selectedFriendForCompare?.let { friend ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
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
                        OutlinedTextField(
                            value = compareSearchQuery,
                            onValueChange = { compareSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            placeholder = { Text("Pesquisar disciplina...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        val mySubjects = remember(myBoletim) { myBoletim?.disciplinas?.associateBy { it["name"] ?: "" } ?: emptyMap() }
                        val friendSubjects = remember(selectedFriendBoletim) { selectedFriendBoletim!!.disciplinas }
                        val filteredSubjects = remember(friendSubjects, compareSearchQuery) { 
                            friendSubjects.filter { (it["name"] ?: "").contains(compareSearchQuery, ignoreCase = true) }
                        }
                        
                        LazyColumn(
                flingBehavior = rememberAppFlingBehavior(), modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Disciplina", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Turma", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Dif.", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Você", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                                    Text("Amigo", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            
                            items(
                                items = filteredSubjects,
                                key = { it["name"] ?: it.hashCode() },
                                contentType = { "compare_subject_item" }
                            ) { friendSub ->
                                val name = friendSub["name"] ?: ""
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = MaterialTheme.shapes.large,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        val gradesKeys = listOf("grade1" to "1º Bim", "grade2" to "2º Bim", "grade3" to "3º Bim", "grade4" to "4º Bim", "finalGrade" to "Média")
                                        val localSub = localSubjects.find { it.name.equals(name, ignoreCase = true) }
                                        
                                        var mySum = 0.0
                                        var myCount = 0
                                        var frSum = 0.0
                                        var frCount = 0
                                        var classSum = 0.0
                                        var classCount = 0
                                        
                                        gradesKeys.forEach { (key, label) ->
                                            val isMedia = key == "finalGrade"
                                            
                                            var myG = mySubjects[name]?.get(key)?.takeIf { it.isNotBlank() } ?: "-"
                                            var frG = friendSub[key]?.takeIf { it.isNotBlank() } ?: "-"
                                            var classGrade = when (key) {
                                                "grade1" -> localSub?.classGrade1
                                                "grade2" -> localSub?.classGrade2
                                                "grade3" -> localSub?.classGrade3
                                                "grade4" -> localSub?.classGrade4
                                                else -> null
                                            }?.takeIf { it.isNotBlank() && it != "0.0" && it != "0" } ?: "-"
                                            
                                            if (!isMedia) {
                                                val myN = myG.replace(",", ".").toDoubleOrNull()
                                                val frN = frG.replace(",", ".").toDoubleOrNull()
                                                val clN = classGrade.replace(",", ".").toDoubleOrNull()
                                                if (myN != null) { mySum += myN; myCount++ }
                                                if (frN != null) { frSum += frN; frCount++ }
                                                if (clN != null) { classSum += clN; classCount++ }
                                            } else {
                                                if (myCount > 0) myG = String.format(Locale.US, "%.1f", mySum / myCount).replace(".", ",")
                                                if (frCount > 0) frG = String.format(Locale.US, "%.1f", frSum / frCount).replace(".", ",")
                                                if (classCount > 0) classGrade = String.format(Locale.US, "%.1f", classSum / classCount).replace(".", ",")
                                            }
                                            
                                            val myNum = myG.replace(",", ".").toFloatOrNull()
                                            val frNum = frG.replace(",", ".").toFloatOrNull()
                                            
                                            val diffText = if (myNum != null && frNum != null) {
                                                val diff = myNum - frNum
                                                if (diff > 0) "+${String.format("%.1f", diff)}" 
                                                else if (diff < 0) String.format("%.1f", diff)
                                                else "0,0"
                                            } else {
                                                "-"
                                            }
                                            
                                            val diffColor = if (myNum != null && frNum != null) {
                                                val diff = myNum - frNum
                                                if (diff > 0) AppSemanticColors.success
                                                else if (diff < 0) AppSemanticColors.danger
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), 
                                                horizontalArrangement = Arrangement.SpaceBetween, 
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                
                                                Text(classGrade, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                                
                                                Text(diffText.replace(".", ","), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = diffColor, textAlign = TextAlign.Center)
                                                
                                                Text(myG, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                                
                                                Text(frG, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                                            }
                                            if (key != "finalGrade") {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
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
    }
}

@Composable
fun FriendItemCard(
    friend: Friend,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var friendBoletim by remember { mutableStateOf<BoletimShared?>(null) }
    val xp = friend.xp
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(friend.uid) {
        friendBoletim = FriendsManager.getSharedBoletim(friend.uid)
    }

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
    
    val totalSubjects = friendBoletim?.disciplinas?.size ?: 10
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
        1 -> "🌱 Calouro"
        2 -> "📖 Aprendiz"
        3 -> "🎓 Elite"
        4 -> "🧠 Mestre"
        5 -> "👑 Lenda"
        else -> "🌱 Calouro"
    }

    val badgeIcon = when (globalLevel) {
        1 -> Icons.Default.LocalFlorist
        2 -> Icons.Default.MenuBook
        3 -> Icons.Default.School
        4 -> Icons.Default.Psychology
        5 -> Icons.Default.WorkspacePremium
        else -> Icons.Default.Star
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onEditClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
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
                                imageVector = Icons.Default.PersonRemove,
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
