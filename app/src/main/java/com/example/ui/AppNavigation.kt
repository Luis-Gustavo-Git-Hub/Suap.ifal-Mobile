package com.example.ui

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.AcademicCalculator
import com.example.ui.navigation.NavRoute
import com.example.ui.theme.AppTheme
import com.example.ui.theme.IconPack
import com.example.ui.theme.NavBarStyle
import com.example.util.CrashLogger
import com.example.util.CredentialStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppNavigation(viewModel: SuapViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        com.example.util.AppSnackbarManager.hostState = snackbarHostState
        com.example.util.AppSnackbarManager.scope = coroutineScope
    }

    val sharedPrefs = remember { context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE) }
    val credentialStore = remember { CredentialStore.getInstance(context) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val termsAccepted by remember { mutableStateOf(appPrefs.getBoolean("terms_accepted", false)) }

    var sigaaCredentialsSaved by remember { mutableStateOf(credentialStore.hasSigaaCredentials()) }
    var suapCredentialsSaved by remember { mutableStateOf(credentialStore.hasSuapCredentials()) }

    var isLoggedIn by remember {
        mutableStateOf(
            sharedPrefs.getString("saved_student_name", "")?.isNotEmpty() == true &&
                sharedPrefs.getString("saved_student_name", "") != "Aluno" &&
                sharedPrefs.getString("saved_student_name", "") != "Visitante"
        )
    }
    var isSyncing by remember { mutableStateOf(false) }
    var isSilentSyncing by remember { mutableStateOf(false) }
    var pendingNavigationRoute by remember { mutableStateOf<NavRoute?>(null) }
    var hasSilentSyncFailed by remember { mutableStateOf(false) }

    val autoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login", true)) }

    LaunchedEffect(Unit) {
        if (autoLoginEnabled && credentialStore.hasSuapCredentials()) {
            isLoggedIn = true
            isSilentSyncing = true
        }
    }

    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val statisticsSnapshot by viewModel.statisticsSnapshot.collectAsStateWithLifecycle()

    val hasSavedCredentials = credentialStore.hasSuapCredentials()
    val tutorialCompleted = sharedPrefs.getBoolean("suap_tutorial_completed", false)

    var showFirstAccessTutorial by remember {
        mutableStateOf(!hasSavedCredentials && !tutorialCompleted)
    }

    if (showFirstAccessTutorial) {
        SuapTutorialDialog(
            onDismissRequest = {
                showFirstAccessTutorial = false
                sharedPrefs.edit().putBoolean("suap_tutorial_completed", true).apply()
            },
            onComplete = {
                showFirstAccessTutorial = false
                sharedPrefs.edit().putBoolean("suap_tutorial_completed", true).apply()
                try {
                    navController.navigate(NavRoute.Profile(scrollToSuap = true)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                } catch (e: Exception) {
                    CrashLogger.log(context, "ERROR", "Erro ao navegar para perfil: ${e.message}", e)
                }
            }
        )
    }

    if (isSyncing) {
        LoginScreen(
            savedUsername = credentialStore.getSuapUsername(),
            savedPassword = credentialStore.getSuapPassword(),
            onBack = { isSyncing = false },
            onSaveCredentials = { u, p ->
                if (credentialStore.setSuapCredentials(u, p)) {
                    suapCredentialsSaved = credentialStore.hasSuapCredentials()
                } else {
                    com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                }
            },
            onGovBrLogin = { matricula ->
                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putString("suap_login_method", "gov_br").putString("saved_matricula", matricula).apply()
            },
            onHtmlScraped = { html ->
                viewModel.parseHtmlAndSave(html) { success ->
                    isSyncing = false
                    if (success) {
                        isLoggedIn = true
                        com.example.util.AppSnackbarManager.show("Dados acadêmicos atualizados.")
                    } else {
                        com.example.util.AppSnackbarManager.show(
                            "Não foi possível ler o boletim. Seus dados anteriores foram mantidos.",
                            actionLabel = "Tentar novamente",
                            onAction = { isSyncing = true }
                        )
                    }
                }
            },
            onDemoClick = {
                viewModel.loadDemoData()
                val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                val time = sdf.format(Date())
                viewModel.updateLastSyncTime(time)
                isLoggedIn = true
                isSyncing = false
            }
        )
    } else {
        val currentBackStack by navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStack?.destination

        val isDashboard = currentDestination?.hasRoute<NavRoute.Dashboard>() == true
        val isAvisos = currentDestination?.hasRoute<NavRoute.Avisos>() == true
        val isStatistics = currentDestination?.hasRoute<NavRoute.Statistics>() == true
        val isProfile = currentDestination?.hasRoute<NavRoute.Profile>() == true
        val isMainTab = isDashboard || isAvisos || isStatistics || isProfile

        val studentName by viewModel.studentName.collectAsStateWithLifecycle()
        val curso by viewModel.curso.collectAsStateWithLifecycle()
        val globalFrequency by viewModel.globalFrequency.collectAsStateWithLifecycle()
        val globalAbsences by viewModel.globalAbsences.collectAsStateWithLifecycle()
        val totalClasses by viewModel.totalClasses.collectAsStateWithLifecycle()
        val passedClasses by viewModel.passedClasses.collectAsStateWithLifecycle()
        val subjects by viewModel.subjects.collectAsStateWithLifecycle()
        val scheduleItems by viewModel.scheduleItems.collectAsStateWithLifecycle()
        val avisos by viewModel.avisos.collectAsStateWithLifecycle()
        val professorContacts by viewModel.professorContacts.collectAsStateWithLifecycle()
        val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
        val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

        var swipeOffset by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isMainTab) {
                    if (isMainTab) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > 50f) {
                                    if (isAvisos) {
                                        navController.navigate(NavRoute.Dashboard) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else if (isProfile) {
                                        navController.navigate(NavRoute.Avisos) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                } else if (swipeOffset < -50f) {
                                    if (isDashboard) {
                                        navController.navigate(NavRoute.Avisos) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else if (isAvisos) {
                                        navController.navigate(NavRoute.Profile()) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                swipeOffset = 0f
                            }
                        ) { _, dragAmount ->
                            swipeOffset += dragAmount
                        }
                    }
                }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = if (isMainTab) 72.dp else 16.dp)
                    )
                },
                bottomBar = {
                    if (isMainTab) {
                        val isFloating = (appTheme as? AppTheme.Custom)?.data?.navBarStyle == NavBarStyle.FLOATING
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = if (isFloating) 16.dp else 10.dp, top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val barHPadding = if (maxWidth < 360.dp) 8.dp else 16.dp
                            val navShape = when {
                                appTheme is AppTheme.CYBERPUNK || ((appTheme as? AppTheme.Custom)?.data?.navBarStyle == NavBarStyle.CUT) -> CutCornerShape(16.dp)
                                appTheme is AppTheme.DRACULA -> MaterialTheme.shapes.medium
                                appTheme is AppTheme.NATURE || appTheme is AppTheme.ROBIN_KAWA || ((appTheme as? AppTheme.Custom)?.data?.navBarStyle == NavBarStyle.PILL) -> MaterialTheme.shapes.extraLarge
                                (appTheme as? AppTheme.Custom)?.data?.navBarStyle == NavBarStyle.FLOATING -> MaterialTheme.shapes.extraLarge
                                else -> MaterialTheme.shapes.extraLarge
                            }
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 460.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = barHPadding)
                                    .height(64.dp)
                                    .shadow(16.dp, navShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), navShape)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), navShape)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                fun navigateTab(targetRoute: NavRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                NavItem(
                                    modifier = Modifier.weight(1f),
                                    appTheme = appTheme,
                                    selected = isDashboard,
                                    onClick = { navigateTab(NavRoute.Dashboard) },
                                    icon = getTabIcon("dashboard", isDashboard, appTheme),
                                    label = "Início"
                                )
                                NavItem(
                                    modifier = Modifier.weight(1f),
                                    appTheme = appTheme,
                                    selected = isAvisos,
                                    onClick = { navigateTab(NavRoute.Avisos) },
                                    icon = getTabIcon("notifications", isAvisos, appTheme),
                                    label = "Avisos"
                                )
                                NavItem(
                                    modifier = Modifier.weight(1f),
                                    appTheme = appTheme,
                                    selected = isStatistics,
                                    onClick = { navigateTab(NavRoute.Statistics()) },
                                    icon = getTabIcon("statistics", isStatistics, appTheme),
                                    label = "Estatística"
                                )
                                NavItem(
                                    modifier = Modifier.weight(1f),
                                    appTheme = appTheme,
                                    selected = isProfile,
                                    onClick = { navigateTab(NavRoute.Profile()) },
                                    icon = getTabIcon("profile", isProfile, appTheme),
                                    label = "Perfil"
                                )
                            }
                        }
                    }
                }
            ) { _ ->
                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Dashboard,
                    enterTransition = {
                        slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(animationSpec = tween(240))
                    },
                    exitTransition = {
                        slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(animationSpec = tween(240))
                    },
                    popEnterTransition = {
                        slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(animationSpec = tween(240))
                    },
                    popExitTransition = {
                        slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width } + fadeOut(animationSpec = tween(240))
                    }
                ) {
                    composable<NavRoute.Dashboard> {
                        val campusMatch = Regex("\\(CAMPUS (.*?)\\)").find(curso)
                        val campusName = campusMatch?.groups?.get(1)?.value?.let { "IFAL • CAMPUS $it" } ?: "INSTITUTO FEDERAL DE ALAGOAS"

                        val totalXp = remember(subjects) {
                            subjects.sumOf { subject ->
                                val g1 = AcademicCalculator.parseGrade(subject.grade1)
                                val g2 = AcademicCalculator.parseGrade(subject.grade2)
                                val g3 = AcademicCalculator.parseGrade(subject.grade3)
                                val g4 = AcademicCalculator.parseGrade(subject.grade4)
                                AcademicCalculator.calculateSubjectXp(g1, g2, g3, g4)
                            }
                        }
                        DashboardScreen(
                            studentName = studentName,
                            campusName = campusName,
                            globalFrequency = globalFrequency,
                            globalAbsences = globalAbsences,
                            totalClasses = totalClasses,
                            passedClasses = passedClasses,
                            subjectsCount = subjects.size,
                            lastSyncTime = lastSyncTime,
                            hasSigaaCredentials = sigaaCredentialsSaved,
                            hasSuapCredentials = suapCredentialsSaved,
                            subjects = subjects,
                            scheduleItems = scheduleItems,
                            avisos = avisos,
                            professorContacts = professorContacts,
                            evaluations = evaluations,
                            totalXp = totalXp,
                            statistics = statisticsSnapshot,
                            termsAccepted = termsAccepted,
                            onNavigateToAssuntos = {
                                try { navController.navigate(NavRoute.Assuntos) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir assuntos: ${e.message}", e) }
                            },
                            onNavigateToBoletim = {
                                try { navController.navigate(NavRoute.Boletim) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir boletim: ${e.message}", e) }
                            },
                            onNavigateToAlmoco = {
                                try { navController.navigate(NavRoute.SigaaAlmoco) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir almoco: ${e.message}", e) }
                            },
                            onNavigateToHorarios = {
                                try { navController.navigate(NavRoute.Horarios) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir horarios: ${e.message}", e) }
                            },
                            onNavigateToAvisos = {
                                try {
                                    navController.navigate(NavRoute.Avisos) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir avisos: ${e.message}", e)
                                }
                            },
                            onNavigateToStatistics = {
                                try {
                                    navController.navigate(NavRoute.Statistics()) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir estatísticas: ${e.message}", e)
                                }
                            },
                            onNavigateToJustificativa = {
                                if (!suapCredentialsSaved) {
                                    com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para solicitar justificativa de falta.")
                                    navController.navigate(NavRoute.Profile(scrollToSuap = true))
                                    return@DashboardScreen
                                }
                                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                                if (cookie.isNullOrEmpty() || !cookie.contains("sessionid")) {
                                    pendingNavigationRoute = NavRoute.JustificativaFalta
                                    isSilentSyncing = true
                                    com.example.util.AppSnackbarManager.show("Conectando ao SUAP...")
                                } else {
                                    try { navController.navigate(NavRoute.JustificativaFalta) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir justificativa: ${e.message}", e) }
                                }
                            },
                            onNavigateToSituacao = {
                                if (!suapCredentialsSaved) {
                                    com.example.util.AppSnackbarManager.show("Entre no SUAP no Perfil para ver a situação das justificativas.")
                                    navController.navigate(NavRoute.Profile(scrollToSuap = true))
                                    return@DashboardScreen
                                }
                                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                                if (cookie.isNullOrEmpty() || !cookie.contains("sessionid")) {
                                    pendingNavigationRoute = NavRoute.SituacaoJustificativa
                                    isSilentSyncing = true
                                    com.example.util.AppSnackbarManager.show("Conectando ao SUAP...")
                                } else {
                                    try { navController.navigate(NavRoute.SituacaoJustificativa) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir situacao: ${e.message}", e) }
                                }
                            },
                            onNavigateToFriends = {
                                try { navController.navigate(NavRoute.Friends) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir friends: ${e.message}", e) }
                            },
                            onNavigateToProfile = { scrollToSigaa ->
                                try {
                                    navController.navigate(NavRoute.Profile(scrollToSigaa = scrollToSigaa)) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir perfil: ${e.message}", e)
                                }
                            },
                            onSyncClick = { isSyncing = true },
                            isRefreshing = isSilentSyncing,
                            onRefresh = { isSilentSyncing = true }
                        )
                    }

                    composable<NavRoute.Boletim> {
                        BoletimScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToLogs = {
                                try { navController.navigate(NavRoute.Logs) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir logs: ${e.message}", e) }
                            },
                            onSubjectClick = { name, url ->
                                viewModel.selectSubject(name, url)
                                try {
                                    navController.navigate(NavRoute.SubjectTopics(name = name, detailUrl = url))
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir tópicos da matéria: ${e.message}", e)
                                }
                            },
                            isRefreshing = isSilentSyncing,
                            onRefresh = { isSilentSyncing = true }
                        )
                    }

                    composable<NavRoute.Aulas> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.Aulas>()
                        val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: ""

                        AulasScreen(
                            subjectName = route.title,
                            url = route.url,
                            cookie = cookie,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.Assuntos> {
                        AssuntosScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onMaterialsClick = { name, url ->
                                viewModel.selectSubject(name, url)
                                try {
                                    navController.navigate(NavRoute.SubjectTopics(name = name, detailUrl = url))
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir tópicos da matéria: ${e.message}", e)
                                }
                            },
                            onHistoryClick = { name, detailUrl ->
                                try {
                                    val fullUrl = if (detailUrl.startsWith("http")) detailUrl else "https://suap.ifal.edu.br$detailUrl"
                                    navController.navigate(NavRoute.Aulas(title = name, url = fullUrl))
                                } catch (e: Exception) {
                                    CrashLogger.log(context, "ERROR", "Erro ao abrir histórico: ${e.message}", e)
                                }
                            }
                        )
                    }

                    composable<NavRoute.SuapWebView> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.SuapWebView>()
                        SuapWebViewScreen(
                            title = route.title,
                            url = route.url,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.SubjectTopics> {
                        val subjectName by viewModel.selectedSubjectName.collectAsStateWithLifecycle()
                        val subjectUrl by viewModel.selectedSubjectUrl.collectAsStateWithLifecycle()

                        SubjectTopicsScreen(
                            subjectName = subjectName,
                            subjectUrl = subjectUrl,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onViewPdfClick = { title, url ->
                                navController.navigate(NavRoute.PdfViewer(title = title, url = url))
                            }
                        )
                    }

                    composable<NavRoute.PdfViewer> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.PdfViewer>()
                        PdfViewerScreen(
                            title = route.title,
                            url = route.url,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.Credits> {
                        CreditsScreen(
                            onBack = { navController.popBackStack() },
                            onViewLogs = {
                                try { navController.navigate(NavRoute.Logs) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir logs: ${e.message}", e) }
                            }
                        )
                    }

                    composable<NavRoute.Logs> {
                        LogsScreen(onBack = { navController.popBackStack() })
                    }

                    composable<NavRoute.Almoco> {
                        AlmocoScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSigaaLogin = { navController.navigate(NavRoute.SigaaLogin(next = "sigaa_almoco")) },
                            onNavigateToMarcarAlmoco = { navController.navigate(NavRoute.SigaaAlmoco) }
                        )
                    }

                    composable<NavRoute.SigaaLogin> {
                        SigaaNativeLoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLoginSuccessAction = {
                                sigaaCredentialsSaved = true
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<NavRoute.SigaaAlmoco> {
                        SigaaNativeMarcarAlmocoScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.SigaaCertificado> {
                        SigaaCertificadoNativeScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.Friends> {
                        FriendsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.Horarios> {
                        val academicEvents by viewModel.academicEvents.collectAsStateWithLifecycle()
                        HorariosScreen(
                            viewModel = viewModel,
                            academicEvents = academicEvents,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.SituacaoJustificativa> {
                        SituacaoJustificativaScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetalhes = { id ->
                                navController.navigate(NavRoute.DetalhesJustificativa(id = id))
                            }
                        )
                    }

                    composable<NavRoute.DetalhesJustificativa> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.DetalhesJustificativa>()
                        DetalhesJustificativaScreen(
                            justificativaId = route.id,
                            onBack = { navController.popBackStack() },
                            onNavigateToReposicao = { id ->
                                navController.navigate(NavRoute.ReposicaoJustificativa(id = id))
                            }
                        )
                    }

                    composable<NavRoute.ReposicaoJustificativa> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.ReposicaoJustificativa>()
                        ReposicaoAtividadeScreen(
                            justificativaId = route.id,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NavRoute.JustificativaFalta> {
                        var isSubmittingJustificativa by remember { mutableStateOf(false) }
                        JustificativaFaltaScreen(
                            onBack = { navController.popBackStack() },
                            isSubmitting = isSubmittingJustificativa,
                            onSubmit = { dataInicio, dias, motivo, justificativa, anexoUri ->
                                if (anexoUri == null) {
                                    com.example.util.AppSnackbarManager.show("Selecione o anexo obrigatório.")
                                } else {
                                    isSubmittingJustificativa = true
                                    viewModel.submitJustificativa(
                                        dataInicio = dataInicio,
                                        quantidadeDias = dias,
                                        motivo = motivo,
                                        justificativa = justificativa,
                                        anexoUri = anexoUri,
                                        context = context,
                                        onSuccess = {
                                            isSubmittingJustificativa = false
                                            com.example.util.AppSnackbarManager.show("Justificativa enviada com sucesso!")
                                            navController.popBackStack()
                                            try { navController.navigate(NavRoute.SituacaoJustificativa) } catch (e: Exception) {
                                                CrashLogger.log(context, "ERROR", "Erro ao abrir situacao: ${e.message}", e)
                                            }
                                        },
                                        onError = { msg ->
                                            isSubmittingJustificativa = false
                                            com.example.util.AppSnackbarManager.show(msg)
                                        }
                                    )
                                }
                            }
                        )
                    }

                    composable<NavRoute.Statistics> {
                        StatisticsScreen(
                            viewModel = viewModel
                        )
                    }

                    composable<NavRoute.Avisos> {
                        AvisosScreen(
                            viewModel = viewModel
                        )
                    }

                    composable<NavRoute.Profile> { backStackEntry ->
                        val route = backStackEntry.toRoute<NavRoute.Profile>()
                        ProfileScreen(
                            viewModel = viewModel,
                            scrollToSigaa = route.scrollToSigaa,
                            scrollToSuap = route.scrollToSuap,
                            onNavigateToCredits = { navController.navigate(NavRoute.Credits) },
                            onNavigateToLogs = {
                                try { navController.navigate(NavRoute.Logs) } catch (e: Exception) { CrashLogger.log(context, "ERROR", "Erro ao abrir logs: ${e.message}", e) }
                            },
                            onResetToDashboard = {
                                sigaaCredentialsSaved = false
                                suapCredentialsSaved = false
                                isLoggedIn = false
                                isSyncing = false
                                isSilentSyncing = false
                                pendingNavigationRoute = null
                                navController.navigate(NavRoute.Dashboard) {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isSilentSyncing) {
            Box(modifier = Modifier.size(1.dp).alpha(0f)) {
                LoginScreen(
                    savedUsername = credentialStore.getSuapUsername(),
                    savedPassword = credentialStore.getSuapPassword(),
                    onBack = { },
                    onSaveCredentials = { u, p ->
                        if (credentialStore.setSuapCredentials(u, p)) {
                            suapCredentialsSaved = credentialStore.hasSuapCredentials()
                        } else {
                            com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                        }
                    },
                    onGovBrLogin = { matricula ->
                        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putString("suap_login_method", "gov_br").putString("saved_matricula", matricula).apply()
                    },
                    onHtmlScraped = { html ->
                        viewModel.parseHtmlAndSave(html) { success ->
                            isSilentSyncing = false
                            if (success) {
                                isLoggedIn = true
                                hasSilentSyncFailed = false
                                pendingNavigationRoute?.let { target ->
                                    pendingNavigationRoute = null
                                    try { navController.navigate(target) } catch (_: Exception) { }
                                }
                            } else {
                                hasSilentSyncFailed = true
                                pendingNavigationRoute = null
                                com.example.util.AppSnackbarManager.show(
                                    "Não foi possível ler o boletim. Seus dados anteriores foram mantidos.",
                                    actionLabel = "Tentar novamente",
                                    onAction = { isSyncing = true }
                                )
                            }
                        }
                    },
                    onDemoClick = {
                        isSilentSyncing = false
                    },
                    isSilentSync = true,
                    onSyncFailed = { reason ->
                        isSilentSyncing = false
                        hasSilentSyncFailed = true
                        pendingNavigationRoute = null
                        com.example.util.AppSnackbarManager.showSyncError(reason, onRetry = {
                            when (reason) {
                                com.example.util.SyncFailureReason.CAPTCHA_REQUIRED,
                                com.example.util.SyncFailureReason.INVALID_CREDENTIALS,
                                com.example.util.SyncFailureReason.NO_CREDENTIALS -> isSyncing = true
                                else -> isSilentSyncing = true
                            }
                        })
                    }
                )
            }
        }
    }
}

@Composable
fun getTabIcon(route: String, selected: Boolean, appTheme: AppTheme): ImageVector {
    val isCyberpunk = appTheme is AppTheme.CYBERPUNK || ((appTheme as? AppTheme.Custom)?.data?.iconPack == IconPack.TECH)
    val isNature = appTheme is AppTheme.NATURE || ((appTheme as? AppTheme.Custom)?.data?.iconPack == IconPack.NATURE)

    return when {
        isCyberpunk -> when (route) {
            "dashboard" -> if (selected) Icons.Filled.Terminal else Icons.Outlined.Terminal
            "statistics" -> if (selected) Icons.Filled.Insights else Icons.Outlined.Insights
            "notifications" -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
            "profile" -> if (selected) Icons.Filled.Engineering else Icons.Outlined.Engineering
            else -> Icons.Default.Home
        }
        isNature -> when (route) {
            "dashboard" -> if (selected) Icons.Filled.Park else Icons.Outlined.Park
            "statistics" -> if (selected) Icons.Filled.Insights else Icons.Outlined.Insights
            "notifications" -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
            "profile" -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
            else -> Icons.Default.Home
        }
        else -> when (route) {
            "dashboard" -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Home
            "statistics" -> if (selected) Icons.Filled.Insights else Icons.Outlined.Insights
            "notifications" -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
            "profile" -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
            else -> Icons.Default.Home
        }
    }
}

@Composable
fun NavItem(
    modifier: Modifier = Modifier,
    appTheme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Column(
        modifier = modifier
            .height(56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .widthIn(min = 64.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
