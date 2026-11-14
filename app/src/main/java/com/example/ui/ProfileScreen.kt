package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import com.example.ui.theme.Spacing
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.receiver.ClassReminderReceiver
import com.example.BuildConfig
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.content.Context

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ProfileScreen(
    viewModel: SuapViewModel,
    navController: androidx.navigation.NavHostController,
    scrollToSigaa: Boolean,
    checkBatteryOptimization: () -> Unit,
    studentName: String,
    fullName: String,
    matricula: String,
    emailPessoal: String,
    ingresso: String,
    curso: String,
    profilePhotoUrl: String,
    autoLoginEnabled: Boolean,
    onSigaaCredentialsSavedChange: (Boolean) -> Unit,
    onSuapCredentialsSavedChange: (Boolean) -> Unit,
    onIsLoggedInChange: (Boolean) -> Unit,
    onIsSyncingChange: (Boolean) -> Unit,
    onAutoLoginEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE) }
    val sigaaPrefs = remember { context.getSharedPreferences("sigaa_prefs", Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var editMatricula by remember { mutableStateOf(sharedPrefs.getString("username", "") ?: "") }
    var editSenha by remember { mutableStateOf(sharedPrefs.getString("password", "") ?: "") }
    var showSavedMsg by remember { mutableStateOf(false) }
    var editSigaaMatricula by remember { mutableStateOf(sigaaPrefs.getString("username", "") ?: "") }
    var editSigaaSenha by remember { mutableStateOf(sigaaPrefs.getString("password", "") ?: "") }
    var showSigaaSavedMsg by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(appPrefs.getBoolean("lunch_notification_enabled", false)) }
    var showTimePicker by remember { mutableStateOf(false) }
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

    var termsDialogTarget by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(appPrefs.getBoolean("terms_accepted", false)) }

    if (showTimePicker) {
        AppTimePickerDialog(notificationHour, notificationMinute, { showTimePicker = false },
            { h, m ->
 showTimePicker = false
                                                notificationHour = h
                                                notificationMinute = m
                                                appPrefs.edit().putInt("lunch_notification_hour", h).putInt("lunch_notification_minute", m).apply()
                                                com.example.receiver.LunchReminderReceiver.scheduleAlarm(context)
                                            })
    }
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
        ScreenHeader("Perfil", modifier = Modifier.padding(horizontal = Spacing.ScreenPadding))
        ProfileIdentity(studentName, profilePhotoUrl, initials)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            SectionHeader("Conta")
            ProfileAccount(fullName, matricula, emailPessoal, ingresso, curso)

            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Aparência")
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
                    AppDialog(
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
                                            .padding(vertical = Spacing.medium, horizontal = Spacing.small),
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
                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))
                                    Text("Temas Personalizados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(Spacing.small))
                                    customThemes.forEach { customTheme ->
                                        val theme = com.example.ui.theme.AppTheme.Custom(customTheme)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setAppTheme(context, theme)
                                                    showThemeDialog = false
                                                }
                                                .padding(vertical = Spacing.medium, horizontal = Spacing.small),
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
                                                Icon(androidx.compose.material.icons.Icons.Rounded.Delete, contentDescription = "Excluir")
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
                                Text(androidx.compose.ui.res.stringResource(com.example.R.string.close))
                            }
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true }.padding(Spacing.normal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Tema: ${appTheme.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.ArrowDropDown,
                            contentDescription = "Expandir temas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notificationsExpanded = !notificationsExpanded }
                            .padding(Spacing.normal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Notificações",
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
                            imageVector = if (notificationsExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (notificationsExpanded) "Recolher" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = notificationsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.normal)
                                .padding(bottom = Spacing.normal),
                            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
                        ) {
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            SwitchRow(title = "Notificação de Almoço", description = "Lembrete diário para marcar no SIGAA", checked = notificationEnabled, onCheckedChange = { isChecked ->
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
                                    })

                            if (notificationEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTimePicker = true }
                                        .padding(vertical = Spacing.xs),
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
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            SwitchRow(title = "Notas e Avisos Novos", description = "Verificar novas notas e avisos (Diário às 19:00)", checked = avisosNotifEnabled, onCheckedChange = { isChecked ->
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
                                    })

                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            SwitchRow(title = "Notificação de Início de Aula", description = "Avisa sobre o início da aula com matéria e horários", checked = classNotifEnabled, onCheckedChange = { isChecked ->
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
                                    })



                        }
                }
            }

            SectionHeader("Credenciais")
            AppCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.small, vertical = Spacing.xs),
                shape = MaterialTheme.shapes.extraLarge,
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(Spacing.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { suapCardExpanded = !suapCardExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Credenciais • SUAP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Preenchimento automático",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = if (suapCardExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (suapCardExpanded) "Recolher" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = suapCardExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    AppTextField(
                        value = editMatricula,
                        onValueChange = { editMatricula = it },
                        label = { Text("Matrícula") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    AppTextField(
                        value = editSenha,
                        onValueChange = { editSenha = it },
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        password = true
                    )
                    TermsCheckRow(checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it; appPrefs.edit().putBoolean("terms_accepted", it).apply() },
                        onReadTerms = { termsDialogTarget = "SUAP" })
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        onClick = {
                            if (termsAccepted) {
                                sharedPrefs.edit().putString("username", editMatricula).putString("password", editSenha).apply()
                                onSuapCredentialsSavedChange(true)
                                showSavedMsg = true
                            } else {
                                termsDialogTarget = "SUAP"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showSavedMsg) "Salvo com sucesso!" else "Salvar SUAP")
                    }
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SwitchRow(title = "Login Automático", description = "Sincroniza automaticamente ao abrir", checked = autoLoginEnabled, onCheckedChange = {
                                onAutoLoginEnabledChange(it)
                                sharedPrefs.edit().putBoolean("auto_login", it).apply()
                            })
                        }
                    }

                }
            }

            AppCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.small, vertical = Spacing.xs),
                shape = MaterialTheme.shapes.extraLarge,
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(Spacing.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sigaaCardExpanded = !sigaaCardExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Credenciais • SIGAA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Preenchimento automático",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = if (sigaaCardExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (sigaaCardExpanded) "Recolher" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = sigaaCardExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    AppTextField(
                        value = editSigaaMatricula,
                        onValueChange = { editSigaaMatricula = it },
                        label = { Text("Usuário") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    AppTextField(
                        value = editSigaaSenha,
                        onValueChange = { editSigaaSenha = it },
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        password = true
                    )
                    TermsCheckRow(checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it; appPrefs.edit().putBoolean("terms_accepted", it).apply() },
                        onReadTerms = { termsDialogTarget = "SIGAA" })
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        onClick = {
                            if (termsAccepted) {
                                sigaaPrefs.edit().putString("username", editSigaaMatricula).putString("password", editSigaaSenha).apply()
                                onSigaaCredentialsSavedChange(true)
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
            SectionHeader("Sobre")
            SecondaryButton(onClick = { navController.navigate("logs") }, modifier = Modifier.fillMaxWidth()) {
                Text("Logs do aplicativo")
            }
            PrimaryButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Luis-Gustavo-Git-Hub/Suap.ifal-Mobile"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
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
            DestructiveButton(
                onClick = {
                    scope.launch {
                        viewModel.clearAllData()
                        sharedPrefs.edit().clear().apply()
                        sigaaPrefs.edit().clear().apply()
                        onSigaaCredentialsSavedChange(false)
                        onSuapCredentialsSavedChange(false)
                        onIsLoggedInChange(false)
                        onIsSyncingChange(false)
                        navController.navigate("dashboard") {
                            popUpTo(0)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Apagar dados", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (termsDialogTarget != null) {
        AppDialog(
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
                            onSuapCredentialsSavedChange(true)
                            showSavedMsg = true
                        } else if (termsDialogTarget == "SIGAA") {
                            sigaaPrefs.edit().putString("username", editSigaaMatricula).putString("password", editSigaaSenha).apply()
                            onSigaaCredentialsSavedChange(true)
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
