package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import com.example.ui.theme.Spacing
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import android.content.Context


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
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentRoute in listOf("dashboard", "statistics", "notifications") || currentRoute.startsWith("profile")) {
                    Box(modifier = Modifier.navigationBarsPadding().padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.medium)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .shadow(16.dp, MaterialTheme.shapes.extraLarge, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), MaterialTheme.shapes.extraLarge)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.shapes.extraLarge)
                                .padding(horizontal = Spacing.medium),
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
                                icon = if (currentRoute == "dashboard") androidx.compose.material.icons.Icons.Rounded.Home else androidx.compose.material.icons.Icons.Outlined.Home,
                                label = androidx.compose.ui.res.stringResource(com.example.R.string.tab_home)
                            )
                            NavItem(
                                selected = currentRoute == "statistics",
                                onClick = { navigateTab("statistics") },
                                icon = if (currentRoute == "statistics") Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                                label = androidx.compose.ui.res.stringResource(com.example.R.string.tab_statistics)
                            )
                            NavItem(
                                selected = currentRoute == "notifications",
                                onClick = { navigateTab("notifications") },
                                icon = if (currentRoute == "notifications") androidx.compose.material.icons.Icons.Rounded.Notifications else androidx.compose.material.icons.Icons.Outlined.Notifications,
                                label = androidx.compose.ui.res.stringResource(com.example.R.string.tab_notices)
                            )
                            NavItem(
                                selected = currentRoute.startsWith("profile"),
                                onClick = { navigateTab("profile") },
                                icon = if (currentRoute.startsWith("profile")) androidx.compose.material.icons.Icons.Rounded.Person else androidx.compose.material.icons.Icons.Outlined.Person,
                                label = androidx.compose.ui.res.stringResource(com.example.R.string.tab_profile)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(bottom = if (isMainTab(currentRoute)) paddingValues.calculateBottomPadding() else 0.dp).then(if (isMainTab(currentRoute)) Modifier.statusBarsPadding().displayCutoutPadding() else Modifier)) {
                NavHost(
                    navController = navController, 
                    startDestination = "dashboard",
                    enterTransition = { if (isMainTab(initialState.destination.route) && isMainTab(targetState.destination.route)) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180, delayMillis = 60)) else androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { it / 12 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) },
                    exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) },
                    popEnterTransition = { if (isMainTab(initialState.destination.route) && isMainTab(targetState.destination.route)) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180, delayMillis = 60)) else androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { -it / 12 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) },
                    popExitTransition = { if (isMainTab(initialState.destination.route) && isMainTab(targetState.destination.route)) androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) else androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(220)) { it / 12 } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) }
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
                        ProfileScreen(
                            viewModel = viewModel,
                            navController = navController,
                            scrollToSigaa = scrollToSigaa,
                            checkBatteryOptimization = checkBatteryOptimization,
                            studentName = studentName,
                            fullName = fullName,
                            matricula = matricula,
                            emailPessoal = emailPessoal,
                            ingresso = ingresso,
                            curso = curso,
                            profilePhotoUrl = profilePhotoUrl,
                            autoLoginEnabled = autoLoginEnabled,
                            onSigaaCredentialsSavedChange = { sigaaCredentialsSaved = it },
                            onSuapCredentialsSavedChange = { suapCredentialsSaved = it },
                            onIsLoggedInChange = { isLoggedIn = it },
                            onIsSyncingChange = { isSyncing = it },
                            onAutoLoginEnabledChange = { autoLoginEnabled = it }
                        )
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
            AppDialog(
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

@Composable
fun RowScope.NavItem(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val background by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "tab background")
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.weight(1f).heightIn(min = 64.dp).clip(MaterialTheme.shapes.large)
        .background(background).selectable(selected = selected, role = Role.Tab, onClick = onClick)
        .padding(horizontal = Spacing.xs, vertical = Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun isMainTab(route: String?): Boolean = route in listOf("dashboard", "statistics", "notifications") || route?.startsWith("profile") == true
