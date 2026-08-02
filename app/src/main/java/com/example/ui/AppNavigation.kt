package com.example.ui

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.receiver.ClassReminderReceiver
import com.example.BuildConfig
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AppNavigation(viewModel: SuapViewModel) {
    val scope = rememberCoroutineScope()
    
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE) }
    val sigaaPrefs = remember { context.getSharedPreferences("sigaa_prefs", Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    var sigaaCredentialsSaved by remember { mutableStateOf(sigaaPrefs.getString("username", "")?.isNotEmpty() == true && sigaaPrefs.getString("password", "")?.isNotEmpty() == true) }
    var suapCredentialsSaved by remember { mutableStateOf(sharedPrefs.getString("username", "")?.isNotEmpty() == true && sharedPrefs.getString("password", "")?.isNotEmpty() == true) }
    
    var isLoggedIn by remember { mutableStateOf(sharedPrefs.getString("saved_student_name", "")?.isNotEmpty() == true && sharedPrefs.getString("saved_student_name", "") != "Aluno" && sharedPrefs.getString("saved_student_name", "") != "Visitante") }
    var isSyncing by remember { mutableStateOf(false) }
    var isSilentSyncing by remember { mutableStateOf(false) }
    var hasSilentSyncFailed by remember { mutableStateOf(false) }
    
    var autoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login", false)) }
    
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    val checkBatteryOptimization = {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationDialog = true
        }
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (autoLoginEnabled && sharedPrefs.getString("username", "")?.isNotEmpty() == true) {
            if (!isLoggedIn) {
                isSyncing = true
            } else {
                isSilentSyncing = true
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            viewModel.loadDemoData()
        }
    }
    var lastSyncTime by remember { mutableStateOf(sharedPrefs.getString("last_sync_time", "") ?: "") }

    if (isSyncing) {
        LoginScreen(
            savedUsername = sharedPrefs.getString("username", "") ?: "",
            savedPassword = sharedPrefs.getString("password", "") ?: "",
            onBack = { 
                isSyncing = false
            },
            onSaveCredentials = { u, p ->
                sharedPrefs.edit().putString("username", u).putString("password", p).apply()
            },
            onHtmlScraped = { html -> 
                viewModel.parseHtmlAndSave(html)
                val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                val time = sdf.format(java.util.Date())
                sharedPrefs.edit().putString("last_sync_time", time).apply()
                lastSyncTime = time
                isLoggedIn = true 
                isSyncing = false
            },
            onDemoClick = {
                viewModel.loadDemoData()
                val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                val time = sdf.format(java.util.Date())
                sharedPrefs.edit().putString("last_sync_time", time).apply()
                lastSyncTime = time
                isLoggedIn = true
                isSyncing = false
            }
        )
    } else {
        val currentBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStack?.destination?.route ?: "dashboard"
        
        val studentName by viewModel.studentName.collectAsStateWithLifecycle()
        val fullName by viewModel.fullName.collectAsStateWithLifecycle()
        val matricula by viewModel.matricula.collectAsStateWithLifecycle()
        val emailPessoal by viewModel.emailPessoal.collectAsStateWithLifecycle()
        val ingresso by viewModel.ingresso.collectAsStateWithLifecycle()
        val curso by viewModel.curso.collectAsStateWithLifecycle()
        val globalFrequency by viewModel.globalFrequency.collectAsStateWithLifecycle()
        val globalAbsences by viewModel.globalAbsences.collectAsStateWithLifecycle()
        val totalClasses by viewModel.totalClasses.collectAsStateWithLifecycle()
        val passedClasses by viewModel.passedClasses.collectAsStateWithLifecycle()
        val subjects by viewModel.subjects.collectAsStateWithLifecycle()
        val profilePhotoUrl by viewModel.profilePhotoUrl.collectAsStateWithLifecycle()

        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute in listOf("dashboard", "statistics", "notifications") || currentRoute.startsWith("profile")) {
                    Box(modifier = Modifier.navigationBarsPadding().padding(horizontal = 32.dp, vertical = 24.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), RoundedCornerShape(32.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            fun navigateTab(targetRoute: String) {
                                if (currentRoute != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }

                            NavItem(
                                selected = currentRoute == "dashboard",
                                onClick = { navigateTab("dashboard") },
                                icon = if (currentRoute == "dashboard") androidx.compose.material.icons.Icons.Filled.Home else androidx.compose.material.icons.Icons.Outlined.Home,
                                label = "Home"
                            )
                            NavItem(
                                selected = currentRoute == "statistics",
                                onClick = { navigateTab("statistics") },
                                icon = if (currentRoute == "statistics") Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                label = "Estatísticas"
                            )
                            NavItem(
                                selected = currentRoute == "notifications",
                                onClick = { navigateTab("notifications") },
                                icon = if (currentRoute == "notifications") androidx.compose.material.icons.Icons.Filled.Notifications else androidx.compose.material.icons.Icons.Outlined.Notifications,
                                label = "Avisos"
                            )
                            NavItem(
                                selected = currentRoute == "profile",
                                onClick = { navigateTab("profile") },
                                icon = if (currentRoute == "profile") androidx.compose.material.icons.Icons.Filled.Person else androidx.compose.material.icons.Icons.Outlined.Person,
                                label = "Perfil"
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                NavHost(
                    navController = navController, 
                    startDestination = "dashboard",
                    enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
                    exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) },
                    popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
                    popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) }
                ) {
                    composable("dashboard") {
                        val campusMatch = Regex("\\(CAMPUS (.*?)\\)").find(curso)
                        val campusName = campusMatch?.groups?.get(1)?.value?.let { "IFAL • CAMPUS $it" } ?: "INSTITUTO FEDERAL DE ALAGOAS"

                        val hasSigaaCredentials = sigaaCredentialsSaved
                        val hasSuapCredentials = suapCredentialsSaved
                        DashboardScreen(
                            studentName = studentName,
                            campusName = campusName,
                            globalFrequency = globalFrequency,
                            globalAbsences = globalAbsences,
                            totalClasses = totalClasses,
                            passedClasses = passedClasses,
                            subjectsCount = subjects.size,
                            lastSyncTime = lastSyncTime,
                            hasSigaaCredentials = hasSigaaCredentials,
                            hasSuapCredentials = hasSuapCredentials,
                            onNavigateToAssuntos = { 
                                try { navController.navigate("assuntos") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir assuntos: ${e.message}", e) }
                            },
                            onNavigateToBoletim = { 
                                try { navController.navigate("boletim") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir boletim: ${e.message}", e) }
                            },
                            onNavigateToAlmoco = { 
                                try { navController.navigate("sigaa_almoco") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir almoco: ${e.message}", e) }
                            },
                            onNavigateToHorarios = { 
                                try { navController.navigate("horarios") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir horarios: ${e.message}", e) }
                            },
                            onNavigateToProfile = { scrollToSigaa ->
                                try {
                                    navController.navigate("profile?scrollToSigaa=$scrollToSigaa") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } catch (e: Exception) {
                                    com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir perfil: ${e.message}", e)
                                }
                            },
                            onSyncClick = { isSyncing = true },
                            isRefreshing = isSilentSyncing,
                            onRefresh = { isSilentSyncing = true }
                        )
                    }

                    composable("boletim") {
                        BoletimScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToLogs = { 
                                try { navController.navigate("logs") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir logs: ${e.message}", e) }
                            },
                            onSubjectClick = { name, url ->
                                viewModel.selectSubject(name, url)
                                try { 
                                    navController.navigate("subject_topics") 
                                } catch (e: Exception) { 
                                    com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir tópicos da matéria: ${e.message}", e) 
                                }
                            }
                        )
                    }
                    
                    composable("assuntos") {
                        AssuntosScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSubjectClick = { name, url ->
                                viewModel.selectSubject(name, url)
                                try { 
                                    navController.navigate("subject_topics") 
                                } catch (e: Exception) { 
                                    com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir tópicos da matéria: ${e.message}", e) 
                                }
                            }
                        )
                    }

                    composable("subject_topics") {
                        val subjectName by viewModel.selectedSubjectName.collectAsStateWithLifecycle()
                        val subjectUrl by viewModel.selectedSubjectUrl.collectAsStateWithLifecycle()
                        
                        SubjectTopicsScreen(
                            subjectName = subjectName,
                            subjectUrl = subjectUrl,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("credits") {
                        CreditsScreen(
                            onBack = { navController.popBackStack() },
                            onViewLogs = { 
                                try { navController.navigate("logs") } catch (e: Exception) { com.example.util.CrashLogger.log(context, "ERROR", "Erro ao abrir logs: ${e.message}", e) }
                            }
                        )
                    }
                    
                    composable("logs") {
                        LogsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("almoco") {
                        AlmocoScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSigaaLogin = { navController.navigate("sigaa_login") },
                            onNavigateToMarcarAlmoco = { navController.navigate("sigaa_almoco") }
                        )
                    }
                    composable("sigaa_login") {
                        SigaaNativeLoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLoginSuccessAction = {
                                navController.popBackStack()
                                navController.navigate("sigaa_almoco")
                            }
                        )
                    }
                    composable("sigaa_almoco") {
                        SigaaNativeMarcarAlmocoScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("horarios") {
                        HorariosScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("statistics") {
                        StatisticsScreen(viewModel)
                    }
                    composable("notifications") {
                        AvisosScreen(viewModel)
                    }
                    composable(
                        "profile?scrollToSigaa={scrollToSigaa}",
                        arguments = listOf(androidx.navigation.navArgument("scrollToSigaa") {
                            type = androidx.navigation.NavType.BoolType
                            defaultValue = false
                        })
                    ) { backStackEntry ->
                        val scrollToSigaa = backStackEntry.arguments?.getBoolean("scrollToSigaa") ?: false
                        var editMatricula by remember { mutableStateOf(sharedPrefs.getString("username", "") ?: "") }
                        var editSenha by remember { mutableStateOf(sharedPrefs.getString("password", "") ?: "") }
                        var showSavedMsg by remember { mutableStateOf(false) }
                        var editSigaaMatricula by remember { mutableStateOf(sigaaPrefs.getString("username", "") ?: "") }
                        var editSigaaSenha by remember { mutableStateOf(sigaaPrefs.getString("password", "") ?: "") }
                        var showSigaaSavedMsg by remember { mutableStateOf(false) }
                        var notificationEnabled by remember { mutableStateOf(appPrefs.getBoolean("lunch_notification_enabled", false)) }
                        var notificationHour by remember { mutableStateOf(appPrefs.getInt("lunch_notification_hour", 7)) }
                        var notificationMinute by remember { mutableStateOf(appPrefs.getInt("lunch_notification_minute", 30)) }
                        var notificationsExpanded by remember { mutableStateOf(false) }
                        var suapCardExpanded by remember { mutableStateOf(!scrollToSigaa) }
                        var sigaaCardExpanded by remember { mutableStateOf(scrollToSigaa) }
                        var avisosNotifEnabled by remember { mutableStateOf(appPrefs.getBoolean("notifications_enabled", false)) }
                        var classNotifEnabled by remember { mutableStateOf(appPrefs.getBoolean("class_notification_enabled", false)) }

                        val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                android.widget.Toast.makeText(context, "Permissão de notificação concedida!", android.widget.Toast.LENGTH_SHORT).show()
                                if (appPrefs.getBoolean("lunch_notification_enabled", false)) {
                                    com.example.receiver.LunchReminderReceiver.scheduleAlarm(context)
                                }
                                if (appPrefs.getBoolean("notifications_enabled", false)) {
                                    com.example.worker.WorkScheduler.scheduleDailySync(context)
                                }
                                if (appPrefs.getBoolean("class_notification_enabled", false)) {
                                    com.example.receiver.ClassReminderReceiver.scheduleNextClassAlarm(context)
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Permissão de notificação negada.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }

                        var showSenha by remember { mutableStateOf(false) }
                        var showSigaaSenha by remember { mutableStateOf(false) }
                        var termsDialogTarget by remember { mutableStateOf<String?>(null) }
                        var termsAccepted by remember { mutableStateOf(appPrefs.getBoolean("terms_accepted", false)) }

                        val initials = studentName.split(" ").let { 
                            if (it.size >= 2) "${it[0].firstOrNull() ?: ""}${it[1].firstOrNull() ?: ""}" 
                            else "${it.firstOrNull()?.firstOrNull() ?: ""}"
                        }.uppercase()

                        val scrollState = rememberScrollState()
                        
                        LaunchedEffect(scrollToSigaa) {
                            if (scrollToSigaa) {
                                kotlinx.coroutines.delay(100)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            // Header background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .padding(top = 48.dp, bottom = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profilePhotoUrl.isNotEmpty()) {
                                            androidx.compose.foundation.layout.Box(
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                        .data(profilePhotoUrl)
                                                        .crossfade(true)
                                                        .addHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: "")
                                                        .addHeader("Referer", "https://suap.ifal.edu.br/")
                                                        .build(),
                                                    contentDescription = "Profile Photo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initials,
                                                    style = MaterialTheme.typography.displaySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = studentName.ifEmpty { "Perfil do Aluno" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        ProfileField(label = "Nome", value = fullName)
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        ProfileField(label = "Matrícula", value = matricula, isCopyable = true)
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        ProfileField(label = "E-mail Pessoal", value = emailPessoal, isCopyable = true)
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        ProfileField(label = "Ingresso", value = ingresso)
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        ProfileField(label = "Curso", value = curso.replaceFirst(Regex("^\\S+\\s*-\\s*"), ""))
                                    }
                                }
                                
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                    var showThemeDialog by remember { mutableStateOf(false) }
                                    var showThemeCreator by remember { mutableStateOf(false) }
                                    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
                                    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
                                    
                                    if (showThemeCreator) {
                                        CustomThemeCreatorDialog(
                                            onDismissRequest = { showThemeCreator = false },
                                            onSaveTheme = { themeData ->
                                                viewModel.saveCustomTheme(themeData)
                                                viewModel.setAppTheme(context, com.example.ui.theme.AppTheme.Custom(themeData))
                                                showThemeDialog = false
                                            }
                                        )
                                    }
                                    
                                    if (showThemeDialog) {
                                        androidx.compose.material3.AlertDialog(
                                            onDismissRequest = { showThemeDialog = false },
                                            title = { Text("Escolha o Tema", fontWeight = FontWeight.Bold) },
                                            text = {
                                                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                                                    com.example.ui.theme.AppTheme.values().forEach { theme ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { 
                                                                    viewModel.setAppTheme(context, theme)
                                                                    showThemeDialog = false 
                                                                }
                                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            androidx.compose.material3.RadioButton(
                                                                selected = appTheme.id == theme.id,
                                                                onClick = { 
                                                                    viewModel.setAppTheme(context, theme)
                                                                    showThemeDialog = false 
                                                                }
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(theme.displayName, style = MaterialTheme.typography.bodyLarge)
                                                        }
                                                    }
                                                    
                                                    if (customThemes.isNotEmpty()) {
                                                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                                        Text("Temas Personalizados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                                        customThemes.forEach { customTheme ->
                                                            val theme = com.example.ui.theme.AppTheme.Custom(customTheme)
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable { 
                                                                        viewModel.setAppTheme(context, theme)
                                                                        showThemeDialog = false 
                                                                    }
                                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    androidx.compose.material3.RadioButton(
                                                                        selected = appTheme.id == theme.id,
                                                                        onClick = { 
                                                                            viewModel.setAppTheme(context, theme)
                                                                            showThemeDialog = false 
                                                                        }
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Text(theme.displayName, style = MaterialTheme.typography.bodyLarge)
                                                                }
                                                                androidx.compose.material3.IconButton(onClick = { viewModel.removeCustomTheme(customTheme.id, context) }) {
                                                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Excluir")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                androidx.compose.material3.TextButton(onClick = { showThemeCreator = true }) {
                                                    Text("Criar Tema")
                                                }
                                            },
                                            dismissButton = {
                                                androidx.compose.material3.TextButton(onClick = { showThemeDialog = false }) {
                                                    Text("Fechar")
                                                }
                                            }
                                        )
                                    }

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text("Tema: ${appTheme.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                                contentDescription = "Expandir temas",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { notificationsExpanded = !notificationsExpanded }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        text = "Gerenciador de Notificações",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = if (notificationEnabled) "Alertas ativos" else "Lembretes desativados",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (notificationsExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (notificationsExpanded) "Recolher" else "Expandir",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(visible = notificationsExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                                    .padding(bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = "Notificação de Almoço",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Lembrete diário para marcar no SIGAA",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    androidx.compose.material3.Switch(
                                                        checked = notificationEnabled,
                                                        onCheckedChange = { isChecked ->
                                                            notificationEnabled = isChecked
                                                            appPrefs.edit().putBoolean("lunch_notification_enabled", isChecked).apply()
                                                            if (isChecked) {
                                                                checkBatteryOptimization()
                                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                                                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                                ) {
                                                                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                                } else {
                                                                    com.example.receiver.LunchReminderReceiver.scheduleAlarm(context)
                                                                }
                                                            } else {
                                                                com.example.receiver.LunchReminderReceiver.cancelAlarm(context)
                                                            }
                                                        }
                                                    )
                                                }

                                                if (notificationEnabled) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                android.app.TimePickerDialog(context, { _, h, m ->
                                                                    notificationHour = h
                                                                    notificationMinute = m
                                                                    appPrefs.edit().putInt("lunch_notification_hour", h).putInt("lunch_notification_minute", m).apply()
                                                                    com.example.receiver.LunchReminderReceiver.scheduleAlarm(context)
                                                                }, notificationHour, notificationMinute, true).show()
                                                            }
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Horário", style = MaterialTheme.typography.bodyMedium)
                                                        Text(
                                                            text = String.format("%02d:%02d", notificationHour, notificationMinute),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Text("Dias da Semana", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        val days = listOf(
                                                            java.util.Calendar.SUNDAY to "D",
                                                            java.util.Calendar.MONDAY to "S",
                                                            java.util.Calendar.TUESDAY to "T",
                                                            java.util.Calendar.WEDNESDAY to "Q",
                                                            java.util.Calendar.THURSDAY to "Q",
                                                            java.util.Calendar.FRIDAY to "S",
                                                            java.util.Calendar.SATURDAY to "S"
                                                        )
                                                        days.forEach { (day, label) ->
                                                            androidx.compose.runtime.key(day) {
                                                                val defaultEnabled = day != java.util.Calendar.SATURDAY && day != java.util.Calendar.SUNDAY
                                                                var isDayEnabled by remember { mutableStateOf(appPrefs.getBoolean("lunch_notification_day_$day", defaultEnabled)) }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(
                                                                            if (isDayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                                            CircleShape
                                                                        )
                                                                        .clickable {
                                                                            isDayEnabled = !isDayEnabled
                                                                            appPrefs.edit().putBoolean("lunch_notification_day_$day", isDayEnabled).apply()
                                                                            com.example.receiver.LunchReminderReceiver.scheduleAlarm(context)
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = label,
                                                                        color = if (isDayEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }


                                                }
                                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = "Notas e Avisos Novos",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Verificar novas notas e avisos (Diário às 19:00)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    androidx.compose.material3.Switch(
                                                        checked = avisosNotifEnabled,
                                                        onCheckedChange = { isChecked ->
                                                            avisosNotifEnabled = isChecked
                                                            appPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()
                                                             if (isChecked) {
                                                                 checkBatteryOptimization()
                                                                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                                                     androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                                 ) {
                                                                     notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                                 }
                                                                 com.example.worker.WorkScheduler.scheduleDailySync(context)
                                                                 android.widget.Toast.makeText(context, "Sincronização diária ativada (19:00)", android.widget.Toast.LENGTH_SHORT).show()
                                                             } else {
                                                                 com.example.worker.WorkScheduler.cancelSync(context)
                                                                 android.widget.Toast.makeText(context, "Notificações desativadas", android.widget.Toast.LENGTH_SHORT).show()
                                                             }
                                                        }
                                                    )
                                                }

                                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = "Notificação de Início de Aula",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Avisa sobre o início da aula com matéria e horários",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    androidx.compose.material3.Switch(
                                                        checked = classNotifEnabled,
                                                        onCheckedChange = { isChecked ->
                                                            classNotifEnabled = isChecked
                                                            appPrefs.edit().putBoolean("class_notification_enabled", isChecked).apply()
                                                            if (isChecked) {
                                                                checkBatteryOptimization()
                                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                                                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                                ) {
                                                                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                                } else {
                                                                    com.example.receiver.ClassReminderReceiver.scheduleNextClassAlarm(context)
                                                                }
                                                            } else {
                                                                com.example.receiver.ClassReminderReceiver.cancelAlarm(context)
                                                            }
                                                        }
                                                    )
                                                }

                                                /*
                                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            navController.navigate("logs")
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(
                                                            imageVector = androidx.compose.material.icons.Icons.Default.BugReport,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column {
                                                            Text(
                                                                text = "Logs do Aplicativo",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Visualizar e exportar registros do sistema",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                */

                                            }
                                    }
                                }
                                
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { suapCardExpanded = !suapCardExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text("Credenciais SUAP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "Preenchimento automático",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (suapCardExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (suapCardExpanded) "Recolher" else "Expandir",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        androidx.compose.animation.AnimatedVisibility(visible = suapCardExpanded) {
                                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = editMatricula,
                                            onValueChange = { editMatricula = it },
                                            label = { Text("Matrícula") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        androidx.compose.material3.OutlinedTextField(
                                            value = editSenha,
                                            onValueChange = { editSenha = it },
                                            label = { Text("Senha") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            visualTransformation = if (showSenha) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            trailingIcon = {
                                                androidx.compose.material3.IconButton(onClick = { showSenha = !showSenha }) {
                                                    androidx.compose.material3.Text(text = if (showSenha) "🙈" else "👁️")
                                                }
                                            }
                                        )
                                        androidx.compose.material3.Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                                containerColor = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                        ) {
                                            androidx.compose.foundation.layout.Row(
                                                verticalAlignment = Alignment.CenterVertically, 
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        termsAccepted = !termsAccepted 
                                                        appPrefs.edit().putBoolean("terms_accepted", termsAccepted).apply()
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                androidx.compose.material3.Checkbox(
                                                    checked = termsAccepted, 
                                                    onCheckedChange = { 
                                                        termsAccepted = it 
                                                        appPrefs.edit().putBoolean("terms_accepted", it).apply()
                                                    },
                                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                        checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                        uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                androidx.compose.foundation.layout.Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "Concordo com os termos de uso", 
                                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Ler Termos de Uso",
                                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                        ),
                                                        modifier = Modifier
                                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                            .clickable { termsDialogTarget = "SUAP" }
                                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                if (termsAccepted) {
                                                    sharedPrefs.edit().putString("username", editMatricula).putString("password", editSenha).apply()
                                                    suapCredentialsSaved = true
                                                    showSavedMsg = true
                                                } else {
                                                    termsDialogTarget = "SUAP"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (showSavedMsg) "Salvo com sucesso!" else "Salvar SUAP")
                                        }
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text("Login Automático", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Sincroniza automaticamente ao abrir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            androidx.compose.material3.Switch(
                                                checked = autoLoginEnabled,
                                                onCheckedChange = {
                                                    autoLoginEnabled = it
                                                    sharedPrefs.edit().putBoolean("auto_login", it).apply()
                                                }
                                            )
                                        }
                                            }
                                        }

                                    }
                                }
                                
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { sigaaCardExpanded = !sigaaCardExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text("Credenciais SIGAA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "Preenchimento automático",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (sigaaCardExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (sigaaCardExpanded) "Recolher" else "Expandir",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        androidx.compose.animation.AnimatedVisibility(visible = sigaaCardExpanded) {
                                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = editSigaaMatricula,
                                            onValueChange = { editSigaaMatricula = it },
                                            label = { Text("Usuário") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        androidx.compose.material3.OutlinedTextField(
                                            value = editSigaaSenha,
                                            onValueChange = { editSigaaSenha = it },
                                            label = { Text("Senha") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            visualTransformation = if (showSigaaSenha) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            trailingIcon = {
                                                androidx.compose.material3.IconButton(onClick = { showSigaaSenha = !showSigaaSenha }) {
                                                    androidx.compose.material3.Text(text = if (showSigaaSenha) "🙈" else "👁️")
                                                }
                                            }
                                        )
                                        androidx.compose.material3.Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                                containerColor = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                        ) {
                                            androidx.compose.foundation.layout.Row(
                                                verticalAlignment = Alignment.CenterVertically, 
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        termsAccepted = !termsAccepted 
                                                        appPrefs.edit().putBoolean("terms_accepted", termsAccepted).apply()
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                androidx.compose.material3.Checkbox(
                                                    checked = termsAccepted, 
                                                    onCheckedChange = { 
                                                        termsAccepted = it 
                                                        appPrefs.edit().putBoolean("terms_accepted", it).apply()
                                                    },
                                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                        checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                        uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                androidx.compose.foundation.layout.Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "Concordo com os termos de uso", 
                                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Ler Termos de Uso",
                                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                        ),
                                                        modifier = Modifier
                                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                            .clickable { termsDialogTarget = "SIGAA" }
                                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                if (termsAccepted) {
                                                    sigaaPrefs.edit().putString("username", editSigaaMatricula).putString("password", editSigaaSenha).apply()
                                                    sigaaCredentialsSaved = true
                                                    showSigaaSavedMsg = true
                                                } else {
                                                    termsDialogTarget = "SIGAA"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (showSigaaSavedMsg) "Salvo com sucesso!" else "Salvar SIGAA")
                                        }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Luis-Gustavo-Git-Hub/Suap.ifal-Mobile"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Black, contentColor = androidx.compose.ui.graphics.Color.White)
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_github),
                                        contentDescription = "GitHub",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Código Fonte do Aplicativo", fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.clearAllData()
                                            sharedPrefs.edit().clear().apply()
                                            sigaaPrefs.edit().clear().apply()
                                            sigaaCredentialsSaved = false
                                            suapCredentialsSaved = false
                                            isLoggedIn = false
                                            isSyncing = false
                                            navController.navigate("dashboard") {
                                                popUpTo(0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF0000))
                                ) {
                                    Text("Apagar dados", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.TextButton(
                                    onClick = { 
                                        navController.navigate("credits")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Versão do App: ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                        
                        if (termsDialogTarget != null) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { termsDialogTarget = null },
                                title = { Text("Termos de Uso") },
                                text = {
                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = "Termos de Uso e Isenção de Responsabilidade\n\n" +
                                                "1. Natureza do Aplicativo\n" +
                                                "Este aplicativo é um projeto independente e não possui vínculo oficial com a instituição ou com os desenvolvedores dos sistemas originais.\n\n" +
                                                "2. Coleta e Armazenamento de Dados\n" +
                                                "Suas credenciais e demais dados acessados são armazenados exclusivamente de forma local e segura no seu próprio dispositivo. Este aplicativo NÃO envia suas informações para nenhum servidor de terceiros.\n\n" +
                                                "3. Funcionamento\n" +
                                                "O aplicativo atua como uma interface alternativa.\n\n" +
                                                "4. Isenção de Responsabilidade\n" +
                                                "Ao utilizar este aplicativo, você entende e concorda que os desenvolvedores não se responsabilizam por falhas de acesso ou eventuais mudanças estruturais na página.\n\n" +
                                                "Ao aceitar, você declara ter lido, compreendido e aceitado todos os termos descritos acima.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            termsAccepted = true
                                            appPrefs.edit().putBoolean("terms_accepted", true).apply()
                                            if (termsDialogTarget == "SUAP") {
                                                sharedPrefs.edit().putString("username", editMatricula).putString("password", editSenha).apply()
                                                suapCredentialsSaved = true
                                                showSavedMsg = true
                                            } else if (termsDialogTarget == "SIGAA") {
                                                sigaaPrefs.edit().putString("username", editSigaaMatricula).putString("password", editSigaaSenha).apply()
                                                sigaaCredentialsSaved = true
                                                showSigaaSavedMsg = true
                                            }
                                            termsDialogTarget = null
                                        }
                                    ) {
                                        Text("Aceitar")
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { termsDialogTarget = null }
                                    ) {
                                        Text("Recusar")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (isSilentSyncing) {
            Box(modifier = Modifier.size(1.dp).alpha(0f)) {
                LoginScreen(
                    savedUsername = sharedPrefs.getString("username", "") ?: "",
                    savedPassword = sharedPrefs.getString("password", "") ?: "",
                    onBack = { },
                    onSaveCredentials = { u, p ->
                        sharedPrefs.edit().putString("username", u).putString("password", p).apply()
                    },
                    onHtmlScraped = { html -> 
                        viewModel.parseHtmlAndSave(html)
                        val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                        val time = sdf.format(java.util.Date())
                        sharedPrefs.edit().putString("last_sync_time", time).apply()
                        lastSyncTime = time
                        isLoggedIn = true 
                        isSilentSyncing = false
                    },
                    onDemoClick = {
                        isSilentSyncing = false
                    },
                    isSilentSync = true,
                    onSyncFailed = {
                        isSilentSyncing = false
                        hasSilentSyncFailed = true
                        android.widget.Toast.makeText(context, "Falha na sincronização. Verifique sua conexão ou credenciais.", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        
        if (showBatteryOptimizationDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBatteryOptimizationDialog = false },
                title = { Text("Economia de Energia", fontWeight = FontWeight.Bold) },
                text = { Text("Para que as notificações funcionem corretamente em segundo plano, é necessário desativar a economia de energia para este aplicativo.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showBatteryOptimizationDialog = false
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Não foi possível abrir as configurações.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Desativar")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                        Text("Agora Não")
                    }
                }
            )
        }
        
        } // Box wrapper around scaffold
    } // else
}

}

@Composable
fun ProfileField(label: String, value: String, isCopyable: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        if (isCopyable && value.isNotEmpty() && value != "-") {
            androidx.compose.material3.IconButton(
                onClick = {
                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText(label, value)
                    clipboardManager.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "$label copiado!", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    val animationColor by androidx.compose.animation.animateColorAsState(
        targetValue = bgColor,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "bg"
    )
    val contentColorAnim by androidx.compose.animation.animateColorAsState(
        targetValue = contentColor,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "content"
    )
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(animationColor)
            .clickable { onClick() }
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = contentColorAnim, modifier = Modifier.size(22.dp))
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            enter = androidx.compose.animation.expandHorizontally(animationSpec = androidx.compose.animation.core.tween(150)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
            exit = androidx.compose.animation.shrinkHorizontally(animationSpec = androidx.compose.animation.core.tween(150)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColorAnim,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}