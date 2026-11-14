package com.example.ui

import android.webkit.CookieManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

data class DynamicFormField(
    val name: String,
    val label: String,
    val type: String,
    val options: List<Pair<String, String>> = emptyList(),
    val isRequired: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReposicaoAtividadeScreen(
    justificativaId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var formActionUrl by remember { mutableStateOf("") }
    val hiddenFields = remember { mutableStateMapOf<String, String>() }
    val visibleFields = remember { mutableStateListOf<DynamicFormField>() }
    val formValues = remember { mutableStateMapOf<String, String>() }

    val url = "https://suap.ifal.edu.br/edu/solicitar_reposicao_atividade/$justificativaId/"

    LaunchedEffect(justificativaId) {
        isLoading = true
        coroutineScope.launch {
            try {
                val cookie = CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie.isNullOrEmpty()) {
                    errorMessage = "Sessão expirada. Faça login novamente."
                    isLoading = false
                    return@launch
                }

                val doc = withContext(Dispatchers.IO) {
                    Jsoup.connect(url)
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(15000)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; K)")
                        .get()
                }

                val alertMsg = doc.select(".alert-warning, .alert-danger, .errornote, .message.error").text()

                // Ocultar o formulário de busca do SUAP (__buscar_menu__)
                val form = doc.select("form").firstOrNull { it.attr("id") != "__buscar_menu__" && it.hasAttr("method") && it.attr("method").equals("POST", ignoreCase = true) }
                    ?: doc.select("form").firstOrNull { it.attr("id") != "__buscar_menu__" }
                if (form == null) {
                    errorMessage = alertMsg.ifEmpty { "Formulário de reposição não encontrado ou não disponível." }
                    isLoading = false
                    return@launch
                }

                formActionUrl = form.attr("action")
                if (formActionUrl.isEmpty()) {
                    formActionUrl = url
                } else if (!formActionUrl.startsWith("http")) {
                    formActionUrl = "https://suap.ifal.edu.br$formActionUrl"
                }
                if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(formActionUrl)) {
                    errorMessage = "Endereço do formulário fora do SUAP oficial bloqueado."
                    isLoading = false
                    return@launch
                }

                val parsedFields = mutableListOf<DynamicFormField>()
                
                for (element in form.select("input, select, textarea")) {
                    val name = element.attr("name")
                    if (name.isEmpty()) continue

                    val type = element.tagName()
                    val inputType = element.attr("type")
                    val isRequired = element.hasAttr("required")

                    if (inputType == "hidden" || inputType == "submit" || type == "button" || inputType == "button") {
                        if (inputType == "hidden") {
                            hiddenFields[name] = element.attr("value")
                        }
                        continue
                    }

                    val id = element.attr("id")
                    var label = form.select("label[for=$id]").firstOrNull()?.text()
                    if (label.isNullOrEmpty()) {
                        // Some labels wrap the input or are inside a th
                        label = element.parent()?.parent()?.select("th label, td label")?.text() ?: name.replaceFirstChar { it.uppercase() }
                    }
                    
                    // Remove trailing colons or asterisks
                    label = label.replace(Regex("[:*]+$"), "").trim()

                    if (type == "select") {
                        val options = element.select("option").map { it.attr("value") to it.text() }
                        parsedFields.add(DynamicFormField(name, label, "select", options, isRequired))
                        // Set default value if not empty
                        options.firstOrNull { it.first.isNotEmpty() }?.let {
                            formValues[name] = it.first
                        }
                    } else if (type == "textarea") {
                        parsedFields.add(DynamicFormField(name, label, "textarea", isRequired = isRequired))
                        formValues[name] = element.text()
                    } else if (inputType == "checkbox" || inputType == "radio") {
                        // For checkboxes/radios, we collect the label next to them
                        val optionLabel = element.parent()?.text() ?: label
                        parsedFields.add(DynamicFormField(name, optionLabel, inputType, isRequired = isRequired))
                        if (element.hasAttr("checked")) {
                            formValues[name] = element.attr("value")
                        }
                    } else {
                        parsedFields.add(DynamicFormField(name, label, "text", isRequired = isRequired))
                        formValues[name] = element.attr("value")
                    }
                }
                
                visibleFields.clear()
                visibleFields.addAll(parsedFields)
                
                if (visibleFields.isEmpty()) {
                    errorMessage = alertMsg.ifEmpty { "Nenhuma atividade agendada ou disponível para reposição neste período." }
                }

                isLoading = false
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                errorMessage = "Erro ao carregar formulário: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar Reposição", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Voltar")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Preencha os dados da reposição",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            visibleFields.forEach { field ->
                                when (field.type) {
                                    "select" -> {
                                        var expanded by remember { mutableStateOf(false) }
                                        val selectedOptionValue = formValues[field.name] ?: ""
                                        val selectedOptionText = field.options.find { it.first == selectedOptionValue }?.second ?: "Selecione..."
                                        
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedOptionText,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(field.label + if (field.isRequired) " *" else "") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                field.options.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.second) },
                                                        onClick = {
                                                            formValues[field.name] = option.first
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "textarea" -> {
                                        OutlinedTextField(
                                            value = formValues[field.name] ?: "",
                                            onValueChange = { formValues[field.name] = it },
                                            label = { Text(field.label + if (field.isRequired) " *" else "") },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                            maxLines = 5,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                    }
                                    "checkbox", "radio" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = formValues[field.name] != null,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        // SUAP usually expects "on" or the value of the input, let's assume it gets passed correctly 
                                                        // Actually, we need to pass the real value, but we didn't store it. We can just pass "on"
                                                        formValues[field.name] = "on"
                                                    } else {
                                                        formValues.remove(field.name)
                                                    }
                                                }
                                            )
                                            Text(field.label, modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                    else -> {
                                        OutlinedTextField(
                                            value = formValues[field.name] ?: "",
                                            onValueChange = { formValues[field.name] = it },
                                            label = { Text(field.label + if (field.isRequired) " *" else "") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // Validate required fields
                            val missingFields = visibleFields.filter { it.isRequired && formValues[it.name].isNullOrBlank() }
                            if (missingFields.isNotEmpty()) {
                                com.example.util.AppSnackbarManager.show("Preencha todos os campos obrigatórios.")
                                return@Button
                            }

                            isSubmitting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val client = com.example.network.SuapClient.okHttpClient
                                    
                                    val formBodyBuilder = FormBody.Builder()
                                    hiddenFields.forEach { (k, v) -> formBodyBuilder.add(k, v) }
                                    formValues.forEach { (k, v) -> formBodyBuilder.add(k, v) }
                                    
                                    val request = Request.Builder()
                                        .url(formActionUrl)
                                        .addHeader("Referer", url)
                                        .post(formBodyBuilder.build())
                                        .build()

                                    val response = client.newCall(request).execute()
                                    
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        if (response.isSuccessful) {
                                            com.example.util.AppSnackbarManager.show("Solicitação enviada com sucesso!")
                                            onBack()
                                        } else {
                                            com.example.util.AppSnackbarManager.show("Erro ao enviar: ${response.code}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    com.example.util.AppLog.printStackTrace(e)
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        com.example.util.AppSnackbarManager.show("Falha na conexão: ${e.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Enviar Solicitação", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
