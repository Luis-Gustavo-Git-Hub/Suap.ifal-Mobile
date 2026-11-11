package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.R
import com.example.receiver.ClassReminderReceiver
import com.example.receiver.LunchReminderReceiver
import com.example.ui.dialogs.FontSelectionDialog
import com.example.ui.dialogs.ThemeSelectionDialog
import com.example.ui.theme.CustomThemeData
import com.example.util.CrashLogger
import com.example.util.CredentialStore
import com.example.worker.WorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NotificationToggle { LUNCH, AVISOS, CLASSES, GRADES }

@Composable
fun ProfileScreen(
    viewModel: SuapViewModel,
    scrollToSigaa: Boolean = false,
    scrollToSuap: Boolean = false,
    onNavigateToCredits: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onResetToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialStore = remember { CredentialStore.getInstance(context) }
    val sharedPrefs = remember { context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val studentName by viewModel.studentName.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val matricula by viewModel.matricula.collectAsStateWithLifecycle()
    val emailPessoal by viewModel.emailPessoal.collectAsStateWithLifecycle()
    val ingresso by viewModel.ingresso.collectAsStateWithLifecycle()
    val curso by viewModel.curso.collectAsStateWithLifecycle()
    val profilePhotoUrl by viewModel.profilePhotoUrl.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val scheduleItems by viewModel.scheduleItems.collectAsStateWithLifecycle()

    var editMatricula by remember { mutableStateOf(credentialStore.getSuapUsername()) }
    var editSenha by remember { mutableStateOf(credentialStore.getSuapPassword()) }
    var showSavedMsg by remember { mutableStateOf(false) }

    var editSigaaMatricula by remember { mutableStateOf(credentialStore.getSigaaUsername()) }
    var editSigaaSenha by remember { mutableStateOf(credentialStore.getSigaaPassword()) }
    var showSigaaSavedMsg by remember { mutableStateOf(false) }

    var notificationEnabled by remember { mutableStateOf(appPrefs.getBoolean("lunch_notification_enabled", false)) }
    var notificationHour by remember { mutableStateOf(appPrefs.getInt("lunch_notification_hour", 7)) }
    var notificationMinute by remember { mutableStateOf(appPrefs.getInt("lunch_notification_minute", 30)) }

    var notificationsExpanded by rememberSaveable { mutableStateOf(false) }
    var suapCardExpanded by rememberSaveable(scrollToSuap) { mutableStateOf(scrollToSuap) }
    var autoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login", true)) }
    var sigaaCardExpanded by rememberSaveable(scrollToSigaa) { mutableStateOf(scrollToSigaa) }

    var avisosNotifEnabled by remember { mutableStateOf(appPrefs.getBoolean("notifications_enabled", false)) }
    var classNotifEnabled by remember { mutableStateOf(appPrefs.getBoolean("class_notification_enabled", false)) }
    var classReminderLeadMinutes by rememberSaveable { mutableIntStateOf(appPrefs.getInt("class_reminder_lead_minutes", 10)) }
    var notifyGradesEnabled by remember { mutableStateOf(appPrefs.getBoolean("notify_grades", false)) }
    var pendingNotificationToggle by remember { mutableStateOf<NotificationToggle?>(null) }
    val hasUsableSchedule = scheduleItems.isNotEmpty() && !appPrefs.getBoolean("offline_schedule_is_demo", false)

    fun applyNotificationToggle(toggle: NotificationToggle, enabled: Boolean) {
        when (toggle) {
            NotificationToggle.LUNCH -> {
                notificationEnabled = enabled
                appPrefs.edit().putBoolean("lunch_notification_enabled", enabled).apply()
                if (enabled) LunchReminderReceiver.scheduleAlarm(context)
                else LunchReminderReceiver.cancelAlarm(context)
            }
            NotificationToggle.AVISOS -> {
                avisosNotifEnabled = enabled
                appPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
                if (enabled) WorkScheduler.scheduleDailySync(context)
                else if (!notifyGradesEnabled) WorkScheduler.cancelSync(context)
            }
            NotificationToggle.CLASSES -> {
                classNotifEnabled = enabled
                appPrefs.edit().putBoolean("class_notification_enabled", enabled).apply()
                if (enabled) ClassReminderReceiver.scheduleNextClassAlarm(context)
                else ClassReminderReceiver.cancelAlarm(context)
            }
            NotificationToggle.GRADES -> {
                notifyGradesEnabled = enabled
                appPrefs.edit().putBoolean("notify_grades", enabled).apply()
                if (enabled) WorkScheduler.scheduleDailySync(context)
                else if (!avisosNotifEnabled) WorkScheduler.cancelSync(context)
                if (enabled) com.example.util.AppSnackbarManager.show("Verificação periódica de notas ativada!")
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.util.AppSnackbarManager.show("Permissão de notificação concedida!")
            pendingNotificationToggle?.let { applyNotificationToggle(it, true) }
        } else {
            pendingNotificationToggle = null
            com.example.util.AppSnackbarManager.show("Permissão de notificação negada.")
        }
        pendingNotificationToggle = null
    }

    fun changeNotificationSetting(toggle: NotificationToggle, enabled: Boolean) {
        if (enabled &&
            toggle in setOf(NotificationToggle.AVISOS, NotificationToggle.GRADES) &&
            !credentialStore.hasSuapCredentials()
        ) {
            com.example.util.AppSnackbarManager.show("Conecte sua conta do SUAP para receber estas notificações.")
            return
        }
        if (!enabled) {
            applyNotificationToggle(toggle, false)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationToggle = toggle
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            applyNotificationToggle(toggle, true)
        }
    }

    var showSenha by remember { mutableStateOf(false) }
    var showSigaaSenha by remember { mutableStateOf(false) }
    var termsDialogTarget by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(appPrefs.getBoolean("terms_accepted", false)) }
    var showManualTutorial by remember { mutableStateOf(false) }

    if (showManualTutorial) {
        SuapTutorialDialog(
            onDismissRequest = { showManualTutorial = false },
            onComplete = {
                showManualTutorial = false
                sharedPrefs.edit().putBoolean("suap_tutorial_completed", true).apply()
            }
        )
    }

    val initials = studentName.split(" ").let { 
        if (it.size >= 2) "${it[0].firstOrNull() ?: ""}${it[1].firstOrNull() ?: ""}" 
        else "${it.firstOrNull()?.firstOrNull() ?: ""}"
    }.uppercase()

    val scrollState = rememberScrollState()
    
    LaunchedEffect(scrollToSigaa, scrollToSuap) {
        if (scrollToSigaa || scrollToSuap) {
            delay(120)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var isClearingData by remember { mutableStateOf(false) }
    val profilePrefs = remember { context.getSharedPreferences("suap_prefs", Context.MODE_PRIVATE) }
    var isCensored by remember {
        mutableStateOf(profilePrefs.getBoolean("profile_info_censored", false))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 600.dp)
        ) {
        // Shared main-tab header with the student-specific identity and privacy controls.
        com.example.ui.components.AppPageHeader(
            eyebrow = "PERFIL ACADÊMICO",
            title = if (isCensored) "••••••••••••••••" else studentName.ifEmpty { "Aluno IFAL" },
            subtitle = if (curso.isNotEmpty() && curso != "-") {
                if (isCensored) "••••••••••••••••••••" else curso.replaceFirst(Regex("^\\S+\\s*-\\s*"), "")
            } else {
                "Seus dados e preferências do SUAP"
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCensored) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        .border(
                            2.dp,
                            if (isCensored) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCensored) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Foto oculta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (profilePhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profilePhotoUrl)
                                .crossfade(true)
                                .apply {
                                    if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(profilePhotoUrl)) {
                                        addHeader("Cookie", CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: "")
                                        addHeader("Referer", "https://suap.ifal.edu.br/")
                                    }
                                }
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials.ifEmpty { "AL" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            },
            trailingContent = {
                IconButton(
                    onClick = {
                        isCensored = !isCensored
                        profilePrefs.edit().putBoolean("profile_info_censored", isCensored).apply()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isCensored) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isCensored) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isCensored) "Revelar informações" else "Ocultar informações",
                        tint = if (isCensored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            supportingContent = {
                if (matricula.isNotEmpty() && matricula != "-") {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isCensored) "••••••••" else "Matrícula $matricula",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. XP System Card
        if (subjects.isNotEmpty()) {
            XpSystemCard(subjects = subjects)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Informações Acadêmicas (Compact Bento Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Informações Acadêmicas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isCensored) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Oculto",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                ProfileField(label = "Nome Completo", value = fullName, icon = Icons.Default.Person, isCensored = isCensored)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ProfileField(label = "Matrícula", value = matricula, icon = Icons.Default.Badge, isCopyable = true, isCensored = isCensored)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ProfileField(label = "E-mail Pessoal", value = emailPessoal, icon = Icons.Default.Email, isCopyable = true, isCensored = isCensored)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ProfileField(label = "Ano / Semestre de Ingresso", value = ingresso, icon = Icons.Default.CalendarToday, isCensored = isCensored)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ProfileField(label = "Curso", value = curso.replaceFirst(Regex("^\\S+\\s*-\\s*"), ""), icon = Icons.Default.AutoStories, isCensored = isCensored)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Aparência & Notificações (Settings Bento Card)
        var showThemeDialog by remember { mutableStateOf(false) }
        var showThemeCreator by remember { mutableStateOf(false) }
        var showFontDialog by remember { mutableStateOf(false) }
        var themeToEdit by remember { mutableStateOf<CustomThemeData?>(null) }
        val fontOverride by viewModel.fontOverride.collectAsStateWithLifecycle()

        if (showThemeCreator) {
            CustomThemeCreatorDialog(
                initialTheme = themeToEdit,
                onDismissRequest = { 
                    showThemeCreator = false
                    themeToEdit = null
                },
                onSaveTheme = { themeData ->
                    viewModel.saveCustomTheme(themeData)
                    viewModel.setAppTheme(context, com.example.ui.theme.AppTheme.Custom(themeData))
                    showThemeCreator = false
                    themeToEdit = null
                    showThemeDialog = false
                }
            )
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                appTheme = appTheme,
                customThemes = customThemes,
                isRobinKawaUnlocked = viewModel.isRobinKawaUnlocked,
                onSelectTheme = { theme ->
                    viewModel.setAppTheme(context, theme)
                    showThemeDialog = false
                },
                onEditCustomTheme = { customTheme ->
                    themeToEdit = customTheme
                    showThemeCreator = true
                },
                onDeleteCustomTheme = { id ->
                    viewModel.removeCustomTheme(id, context)
                },
                onOpenFontDialog = { showFontDialog = true },
                onCreateTheme = { showThemeCreator = true },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showFontDialog) {
            FontSelectionDialog(
                currentFontOverride = fontOverride,
                onSelectFont = { font ->
                    viewModel.setFontOverride(font)
                    showFontDialog = false
                },
                onDismiss = { showFontDialog = false }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Theme selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tema do Aplicativo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = appTheme.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Escolher tema",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

                // Font selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFontDialog = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Fonte da Interface",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = fontOverride?.displayName ?: "Padrão do Tema",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Escolher fonte",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

                // Notifications Expandable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notificationsExpanded = !notificationsExpanded }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Gerenciador de Notificações",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (notificationEnabled || avisosNotifEnabled || classNotifEnabled || notifyGradesEnabled) "Notificações ativas" else "Lembretes desativados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (notificationsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (notificationsExpanded) "Recolher" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = notificationsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Switch 1: Refeitório
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lembrete de Almoço", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Lembrete às ${String.format("%02d:%02d", notificationHour, notificationMinute)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(
                                    onClick = {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                notificationHour = hour
                                                notificationMinute = minute
                                                appPrefs.edit()
                                                    .putInt("lunch_notification_hour", hour)
                                                    .putInt("lunch_notification_minute", minute)
                                                    .apply()
                                                if (notificationEnabled) LunchReminderReceiver.scheduleAlarm(context)
                                            },
                                            notificationHour,
                                            notificationMinute,
                                            true
                                        ).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                                ) {
                                    Text("Alterar horário", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Switch(
                                checked = notificationEnabled,
                                onCheckedChange = { enabled ->
                                    changeNotificationSetting(NotificationToggle.LUNCH, enabled)
                                }
                            )
                        }

                        // Switch 2: Avisos SUAP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Verificação de Avisos SUAP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Verifica novos avisos periodicamente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = avisosNotifEnabled,
                                onCheckedChange = { enabled ->
                                    changeNotificationSetting(NotificationToggle.AVISOS, enabled)
                                }
                            )
                        }

                        // Switch 3: Notificação de Aulas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lembretes de Aulas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (hasUsableSchedule) {
                                        "Notifica $classReminderLeadMinutes min antes da primeira aula do dia"
                                    } else {
                                        "Sincronize seus horários do SUAP para ativar os lembretes"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = classNotifEnabled,
                                onCheckedChange = { enabled ->
                                    changeNotificationSetting(NotificationToggle.CLASSES, enabled)
                                }
                            )
                        }

                        if (classNotifEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Antecedência do lembrete:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 15, 30).forEach { mins ->
                                        val isSelected = classReminderLeadMinutes == mins
                                        Surface(
                                            onClick = {
                                                classReminderLeadMinutes = mins
                                                appPrefs.edit().putInt("class_reminder_lead_minutes", mins).apply()
                                                ClassReminderReceiver.scheduleNextClassAlarm(context)
                                                com.example.util.AppSnackbarManager.show("Lembrete configurado para $mins min antes do primeiro horário!")
                                            },
                                            shape = MaterialTheme.shapes.small,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$mins min",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Switch 4: Mudança de Notas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Alteração de Notas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Notifica quando uma nota ou falta for lançada no SUAP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = notifyGradesEnabled,
                                onCheckedChange = { enabled ->
                                    changeNotificationSetting(NotificationToggle.GRADES, enabled)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Credenciais do SUAP & SIGAA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // SUAP Creds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { suapCardExpanded = !suapCardExpanded }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Credenciais SUAP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(if (credentialStore.hasSuapCredentials()) "Credenciais configuradas" else "Não configuradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = if (suapCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }

                AnimatedVisibility(visible = suapCardExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editMatricula,
                            onValueChange = { editMatricula = it; showSavedMsg = false },
                            label = { Text("Matrícula SUAP") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editSenha,
                            onValueChange = { editSenha = it; showSavedMsg = false },
                            label = { Text("Senha SUAP") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showSenha) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSenha = !showSenha }) {
                                    Icon(if (showSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoLoginEnabled = !autoLoginEnabled
                                    sharedPrefs.edit().putBoolean("auto_login", autoLoginEnabled).apply()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Login automático", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (autoLoginEnabled) "Entra e sincroniza com o SUAP ao abrir o app" else "Desativado: o app abre só com os dados salvos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoLoginEnabled,
                                onCheckedChange = {
                                    autoLoginEnabled = it
                                    sharedPrefs.edit().putBoolean("auto_login", it).apply()
                                }
                            )
                        }

                        if (!termsAccepted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        termsAccepted = !termsAccepted 
                                        appPrefs.edit().putBoolean("terms_accepted", termsAccepted).apply()
                                    }
                                    .padding(8.dp)
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { 
                                        termsAccepted = it 
                                        appPrefs.edit().putBoolean("terms_accepted", it).apply()
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Concordo com os termos de uso", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Ler Termos",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
                                        modifier = Modifier.clickable { termsDialogTarget = "SUAP" }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { 
                                if (termsAccepted) {
                                    if (credentialStore.setSuapCredentials(editMatricula, editSenha)) {
                                        showSavedMsg = true
                                    } else {
                                        com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                                    }
                                } else {
                                    termsDialogTarget = "SUAP"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (showSavedMsg) "Salvo com sucesso!" else "Salvar SUAP", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

                // SIGAA Creds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sigaaCardExpanded = !sigaaCardExpanded }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Credenciais SIGAA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(if (credentialStore.hasSigaaCredentials()) "Credenciais configuradas" else "Não configuradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = if (sigaaCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }

                AnimatedVisibility(visible = sigaaCardExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editSigaaMatricula,
                            onValueChange = { editSigaaMatricula = it; showSigaaSavedMsg = false },
                            label = { Text("Usuário / CPF SIGAA") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editSigaaSenha,
                            onValueChange = { editSigaaSenha = it; showSigaaSavedMsg = false },
                            label = { Text("Senha SIGAA") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showSigaaSenha) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSigaaSenha = !showSigaaSenha }) {
                                    Icon(if (showSigaaSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            }
                        )

                        if (!termsAccepted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        termsAccepted = !termsAccepted 
                                        appPrefs.edit().putBoolean("terms_accepted", termsAccepted).apply()
                                    }
                                    .padding(8.dp)
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { 
                                        termsAccepted = it 
                                        appPrefs.edit().putBoolean("terms_accepted", it).apply()
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Concordo com os termos de uso", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Ler Termos",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
                                        modifier = Modifier.clickable { termsDialogTarget = "SIGAA" }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { 
                                if (termsAccepted) {
                                    if (credentialStore.setSigaaCredentials(editSigaaMatricula, editSigaaSenha)) {
                                        showSigaaSavedMsg = true
                                    } else {
                                        com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                                    }
                                } else {
                                    termsDialogTarget = "SIGAA"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (showSigaaSavedMsg) "Salvo com sucesso!" else "Salvar SIGAA", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Ações do Aplicativo & Sobre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Luis-Gustavo-Git-Hub/Suap.ifal-Mobile"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = "GitHub",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Código", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }

            Button(
                onClick = onNavigateToLogs,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Ver Logs", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Logs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }

            Button(
                onClick = onNavigateToCredits,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = { showDeleteDataDialog = true },
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apagar Dados do Dispositivo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearingData) showDeleteDataDialog = false },
            title = { Text("Apagar dados deste dispositivo?", fontWeight = FontWeight.Bold) },
            text = { Text(if (isClearingData) "Encerrando sincronizações e limpando os dados locais…" else "Remove as credenciais, notas e preferências locais e desconecta sua conta Google neste dispositivo.") },
            confirmButton = {
                Button(
                    enabled = !isClearingData,
                    onClick = {
                        isClearingData = true
                        scope.launch {
                            try {
                                viewModel.clearAllData()
                                FriendsManager.signOut(context)
                                showDeleteDataDialog = false
                                onResetToDashboard()
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                CrashLogger.log(context, "ERROR", "Erro ao limpar dados locais", e)
                                com.example.util.AppSnackbarManager.show("Não foi possível concluir a limpeza. Tente novamente.")
                            } finally {
                                isClearingData = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isClearingData) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isClearingData) "Apagando…" else "Apagar dados locais")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }, enabled = !isClearingData) {
                    Text("Cancelar")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    if (termsDialogTarget != null) {
        AlertDialog(
            onDismissRequest = { termsDialogTarget = null },
            title = { Text("Termos de Uso") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Termos de Uso e Isenção de Responsabilidade\n\n" +
                            "1. Natureza do Aplicativo\n" +
                            "Este aplicativo é um projeto independente, desenvolvido por alunos, e não possui vínculo oficial com a instituição (IFAL) ou com os desenvolvedores dos sistemas acadêmicos originais (SUAP/SIGAA).\n\n" +
                            "2. Coleta e Armazenamento de Dados Locais\n" +
                            "Suas credenciais institucionais (matrícula e senha) e os dados acadêmicos extraídos (boletins, faltas, horários) são armazenados EXCLUSIVAMENTE de forma local e criptografada no seu próprio dispositivo. O aplicativo não envia esses dados para nenhum servidor externo.\n\n" +
                            "3. Funcionalidade de Amigos e Compartilhamento (Firebase)\n" +
                            "Ao utilizar a funcionalidade \"Amigos\", você concorda que:\n" +
                            "- O login é realizado de forma segura através da sua Conta Google.\n" +
                            "- Apenas o seu E-mail e Foto de Perfil (em formato base64) serão armazenados de forma temporária ou persistente no banco de dados em nuvem (Firebase) estritamente para viabilizar a busca e conexão entre usuários.\n" +
                            "- Sua matrícula, nome completo e senha institucionais NUNCA são compartilhados com o Firebase.\n" +
                            "- Os \"Nomes\" (apelidos) definidos para os seus amigos são de sua total responsabilidade e ficam salvos apenas na sua conta local/nuvem privada.\n" +
                            "- O compartilhamento de notas e boletins com amigos é opcional, explícito (requer ação manual) e de sua inteira responsabilidade.\n\n" +
                            "4. Isenção de Responsabilidade\n" +
                            "Ao utilizar este aplicativo, você entende e concorda que os desenvolvedores não se responsabilizam por falhas de acesso aos sistemas institucionais, mudanças estruturais, ou pelo compartilhamento voluntário que você fizer das suas próprias notas.\n\n" +
                            "Ao aceitar, você declara ter lido, compreendido e aceitado todos os termos descritos acima.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        termsAccepted = true
                        appPrefs.edit().putBoolean("terms_accepted", true).apply()
                        if (termsDialogTarget == "SUAP") {
                            if (credentialStore.setSuapCredentials(editMatricula, editSenha)) {
                                showSavedMsg = true
                            } else {
                                com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                            }
                        } else if (termsDialogTarget == "SIGAA") {
                            if (credentialStore.setSigaaCredentials(editSigaaMatricula, editSigaaSenha)) {
                                showSigaaSavedMsg = true
                            } else {
                                com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                            }
                        }
                        termsDialogTarget = null
                    }
                ) {
                    Text("Aceitar")
                }
            },
            dismissButton = {
                TextButton(onClick = { termsDialogTarget = null }) {
                    Text("Recusar")
                }
            }
        )
    }
}
