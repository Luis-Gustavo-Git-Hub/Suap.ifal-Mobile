package com.example.ui

import com.example.ui.theme.AppSemanticColors

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JustificativaFaltaScreen(
    onBack: () -> Unit,
    isSubmitting: Boolean = false,
    onSubmit: (String, Int, String, String, Uri?) -> Unit
) {
    var dataInicio by remember { mutableStateOf("") }
    var quantidadeDias by remember { mutableStateOf("1") }
    var motivo by remember { mutableStateOf("") }
    var justificativa by remember { mutableStateOf("") }
    var anexoUri by remember { mutableStateOf<Uri?>(null) }
    var anexoNome by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showMotivoDropdown by remember { mutableStateOf(false) }
    
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val motivos = listOf(
        "1" to "Problema de saúde (doença, consulta, exame)",
        "9" to "Problemas pessoais relevantes (situações emergenciais)",
        "8" to "Participação em evento acadêmico (congresso, curso, competição)",
        "7" to "Falta de transporte público",
        "6" to "Trabalho (plantão, convocação extraordinária)",
        "4" to "Convocação judicial",
        "3" to "Falecimento de familiar (luto)",
        "2" to "Acompanhamento de familiar doente",
        "10" to "Compromissos religiosos (Lei 13796/2019)",
        "5" to "Serviço militar obrigatório"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            anexoUri = it
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    anexoNome = cursor.getString(nameIndex)
                } else {
                    anexoNome = "Arquivo selecionado"
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        val offset = calendar.timeZone.getOffset(calendar.timeInMillis)
                        val date = Date(it - offset)
                        dataInicio = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    }
                    showDatePicker = false
                }) {
                    Text("Confirmar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Justificar Falta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Envio de Atestados & Justificativas no SUAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Bento Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            AppSemanticColors.warning.copy(alpha = if (isDark) 0.15f else 0.12f)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                AppSemanticColors.warning.copy(alpha = 0.5f)
                            ),
                            MaterialTheme.shapes.extraLarge
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    AppSemanticColors.warning.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppSemanticColors.warning,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Atenção ao Prazo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "O atestado ou comprovante deve ser anexado. Caso a ausência ultrapasse 15 dias, você pode ter direito a atendimento domiciliar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 1: Dados Gerais Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Período da Ausência",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Data Inicio Picker Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dataInicio,
                                onValueChange = {},
                                label = { Text("Data de Início *") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                placeholder = { Text("Toque para escolher a data") },
                                supportingText = { Text("Formato: AAAA-MM-DD") }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { showDatePicker = true }
                            )
                        }

                        OutlinedTextField(
                            value = quantidadeDias,
                            onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) quantidadeDias = it },
                            label = { Text("Quantidade de Dias de Ausência *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }

                // Section 2: Detalhamento Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Motivo e Detalhamento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = showMotivoDropdown,
                            onExpandedChange = { showMotivoDropdown = !showMotivoDropdown }
                        ) {
                            OutlinedTextField(
                                value = motivos.find { it.first == motivo }?.second ?: "Selecione o motivo da falta...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Motivo da Justificativa *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMotivoDropdown)
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showMotivoDropdown,
                                onDismissRequest = { showMotivoDropdown = false }
                            ) {
                                motivos.forEach { (id, desc) ->
                                    DropdownMenuItem(
                                        text = { Text(desc, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            motivo = id
                                            showMotivoDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = justificativa,
                            onValueChange = { justificativa = it },
                            label = { Text("Informação Complementar (Opcional)") },
                            placeholder = { Text("Descreva mais detalhes se necessário...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = MaterialTheme.shapes.large,
                            maxLines = 5,
                            supportingText = { Text("${justificativa.length} caracteres informados.") }
                        )
                    }
                }

                // Section 3: Anexo File Picker Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Anexo de Documento *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(
                                    if (anexoUri != null) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        if (anexoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    MaterialTheme.shapes.large
                                )
                                .clickable { filePickerLauncher.launch("*/*") }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (anexoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (anexoUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = if (anexoUri != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = anexoNome ?: "Toque para Anexar Comprovante (Atestado/Declaração)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (anexoNome != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tamanho máx: 10 MB. Suportados: PDF, JPEG, PNG",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Large Button
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        if (dataInicio.isEmpty() || quantidadeDias.isEmpty() || motivo.isEmpty() || anexoUri == null) {
                            com.example.util.AppSnackbarManager.show("Preencha a data, o motivo e selecione o anexo obrigatório.")
                        } else {
                            onSubmit(dataInicio, quantidadeDias.toIntOrNull() ?: 1, motivo, justificativa, anexoUri)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Enviando ao SUAP...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Justificativa de Falta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
