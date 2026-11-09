package com.example.ui

import com.example.ui.theme.rememberAppFlingBehavior

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
    
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import okio.BufferedSink
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SigaaCertificadoNativeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val credentialStore = remember { com.example.util.CredentialStore.getInstance(context) }

    val savedUsername = remember {
        (credentialStore.getSigaaUsername().takeIf { it.isNotBlank() }
            ?: credentialStore.getSuapUsername().takeIf { it.isNotBlank() } ?: "").trim()
    }
    val savedPassword = remember {
        (credentialStore.getSigaaPassword().takeIf { it.isNotBlank() }
            ?: credentialStore.getSuapPassword().takeIf { it.isNotBlank() } ?: "").trim()
    }

    var isLoadingData by remember { mutableStateOf(true) }
    var loadingStatus by remember { mutableStateOf("Conectando ao SIGAA...") }
    var loginAttempts by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    
    var options by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var viewStateParam by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }
    
    var hasNavigatedToTarget by remember { mutableStateOf(false) }
    var fallbackAttempts by remember { mutableIntStateOf(0) }
    
    var selectedOptionId by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    
    var showDropdown by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    selectedFileName = cursor.getString(nameIndex)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Certificado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // HEADLESS WEBVIEW FOR SCRAPING THE FORM
            if (isLoadingData && loadError == null) {
                key(reloadKey) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowFileAccessFromFileURLs = false
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                                
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, false)

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onFormLoaded(optionsJson: String, viewState: String, action: String) {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            try {
                                                var cleanJson = optionsJson.trim()
                                                if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                                                    cleanJson = cleanJson.substring(1, cleanJson.length - 1).replace("\\\"", "\"")
                                                }
                                                
                                                val jsonArray = JSONArray(cleanJson)
                                                val opts = mutableListOf<Pair<String, String>>()
                                                for (i in 0 until jsonArray.length()) {
                                                    val obj = jsonArray.getJSONObject(i)
                                                    val id = obj.optString("id", "")
                                                    val text = obj.optString("text", "")
                                                    if (id != "0" && id.isNotEmpty()) {
                                                        opts.add(id to text)
                                                    }
                                                }
                                                options = opts
                                                viewStateParam = viewState
                                                actionUrl = action
                                                isLoadingData = false
                                            } catch (e: Exception) {
                                                loadError = "Erro ao ler opções do formulário. Tente novamente."
                                            }
                                        }
                                    }
                                    
                                    @JavascriptInterface
                                    fun onStatusUpdate(status: String) {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            loadingStatus = status
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onLoginFailed() {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            loadError = "Falha no login do SIGAA. Verifique seu usuário e senha no Perfil."
                                        }
                                    }
                                }, "AndroidBridge")

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val urlStr = request?.url?.toString()
                                        if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(urlStr)) {
                                            return true
                                        }
                                        return false
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(url)) {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val currentUrl = url ?: return
                                        if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(currentUrl)) return
                                        CookieManager.getInstance().flush()
                                        
                                        val isLoginPage = currentUrl.contains("verTelaLogin.do") || 
                                                currentUrl.contains("logar.do") || 
                                                currentUrl.contains("modo=classico") ||
                                                (currentUrl.endsWith("/sigaa/") && !currentUrl.contains("discente"))
                                        
                                        if (isLoginPage) {
                                            if (savedUsername.isNotBlank() && savedPassword.isNotBlank() && loginAttempts < 3) {
                                                loginAttempts++
                                                loadingStatus = "Realizando login automático no SIGAA..."
                                                val safeUsername = JSONObject.quote(savedUsername)
                                                val safePassword = JSONObject.quote(savedPassword)
                                                val loginJs = """
                                                    javascript:(function() {
                                                        var userField = document.querySelector('input[name="user.login"]');
                                                        var passField = document.querySelector('input[name="user.senha"]');
                                                        if (userField && passField) {
                                                            userField.value = $safeUsername;
                                                            passField.value = $safePassword;
                                                            userField.dispatchEvent(new Event('input', { bubbles: true }));
                                                            userField.dispatchEvent(new Event('change', { bubbles: true }));
                                                            passField.dispatchEvent(new Event('input', { bubbles: true }));
                                                            passField.dispatchEvent(new Event('change', { bubbles: true }));
                                                            var form = document.querySelector('form');
                                                            if (form) {
                                                                form.submit();
                                                                return;
                                                            }
                                                            var submitBtn = document.querySelector('input[type="submit"]');
                                                            if (submitBtn) submitBtn.click();
                                                            return;
                                                        }
                                                        AndroidBridge.onLoginFailed();
                                                    })()
                                                """.trimIndent()
                                                view?.evaluateJavascript(loginJs, null)
                                            } else {
                                                loadError = "Falha no login do SIGAA. Verifique suas credenciais no Perfil."
                                            }
                                            return
                                        }
                                        
                                        val isPortalDiscente = currentUrl.contains("discente.jsf") || 
                                                currentUrl.contains("verPortalDiscente.do") || 
                                                currentUrl.contains("portais/discente")
                                        
                                        if (isPortalDiscente) {
                                            if (!hasNavigatedToTarget) {
                                                hasNavigatedToTarget = true
                                                loadingStatus = "Acessando área de certificados..."
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(400)
                                                    withContext(Dispatchers.Main) {
                                                        view?.loadUrl("https://sigaa.ifal.edu.br/sigaa/tecnico/registro_atividade_pratica_profissional/lista.jsf")
                                                    }
                                                }
                                            }
                                            return
                                        }
                                        
                                        if (currentUrl.contains("lista.jsf") || currentUrl.contains("form.jsf") || currentUrl.contains("registro_atividade_pratica_profissional") || currentUrl.contains("registro_atividade_autonoma")) {
                                            if (fallbackAttempts >= 6) {
                                                loadError = "Não foi possível carregar o formulário de certificados. Tente novamente mais tarde."
                                                return
                                            }
                                            fallbackAttempts++
                                            val domCheckJs = """
                                                javascript:(function() {
                                                    var select = document.getElementById('form:tipoAtividadeAutonoma');
                                                    if (!select) {
                                                        var selects = document.getElementsByTagName('select');
                                                        if (selects.length > 0) select = selects[0];
                                                    }
                                                    if (select && select.options.length > 0) {
                                                        // We are on the form page, extract options
                                                        AndroidBridge.onStatusUpdate("Extraindo tipos de atividades...");
                                                        var options = [];
                                                        for (var i = 0; i < select.options.length; i++) {
                                                            options.push({id: select.options[i].value, text: select.options[i].text});
                                                        }
                                                        var viewState = document.getElementById('javax.faces.ViewState') ? document.getElementById('javax.faces.ViewState').value : '';
                                                        var action = document.getElementById('form') ? document.getElementById('form').action : '';
                                                        AndroidBridge.onFormLoaded(JSON.stringify(options), viewState, action);
                                                    } else {
                                                        // We are on the list page, find the add button
                                                        AndroidBridge.onStatusUpdate("Abrindo formulário de cadastro...");
                                                        function tryClick() {
                                                            var clicked = false;
                                                            var inputs = document.querySelectorAll('input[type="submit"], input[type="button"], button');
                                                            for (var i = 0; i < inputs.length; i++) {
                                                                var val = (inputs[i].value || inputs[i].innerText || inputs[i].title || '').toLowerCase();
                                                                if (val.indexOf('cadastrar') !== -1 || val.indexOf('novo') !== -1 || val.indexOf('nova atividade') !== -1 || val.indexOf('adicionar') !== -1) {
                                                                    inputs[i].click();
                                                                    clicked = true;
                                                                    break;
                                                                }
                                                            }
                                                            if(!clicked) {
                                                                var links = document.querySelectorAll('a');
                                                                for (var i = 0; i < links.length; i++) {
                                                                    var text = (links[i].innerText || links[i].title || '').toLowerCase();
                                                                    var href = (links[i].href || '').toLowerCase();
                                                                    if (text.indexOf('cadastrar') !== -1 || text.indexOf('novo') !== -1 || text.indexOf('adicionar') !== -1 || href.indexOf('form.jsf') !== -1) {
                                                                        links[i].click();
                                                                        clicked = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if(!clicked) {
                                                                var imgs = document.querySelectorAll('img');
                                                                for (var i = 0; i < imgs.length; i++) {
                                                                    var info = (imgs[i].alt || imgs[i].title || imgs[i].src || '').toLowerCase();
                                                                    if (info.indexOf('cadastrar') !== -1 || info.indexOf('novo') !== -1 || info.indexOf('add') !== -1) {
                                                                        var p = imgs[i].closest('a');
                                                                        if (p) p.click();
                                                                        else imgs[i].click();
                                                                        clicked = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if (!clicked) {
                                                                setTimeout(tryClick, 800);
                                                            }
                                                        }
                                                        tryClick();
                                                    }
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(domCheckJs, null)
                                        }
                                    }
                                }
                                
                                if (savedUsername.isBlank() || savedPassword.isBlank()) {
                                    loadError = "Credenciais não encontradas. Configure seu usuário e senha do SIGAA no Perfil."
                                    isLoadingData = false
                                } else {
                                    loadUrl("https://sigaa.ifal.edu.br/sigaa/verTelaLogin.do")
                                }
                            }
                        },
                        modifier = Modifier.size(1.dp) // Hidden WebView
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingStatus, style = MaterialTheme.typography.bodyLarge)
                }
            } else if (loadError != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadError ?: "", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Voltar")
                        }
                        Button(onClick = {
                            loginAttempts = 0
                            hasNavigatedToTarget = false
                            fallbackAttempts = 0
                            loadError = null
                            loadingStatus = "Reconectando ao SIGAA..."
                            isLoadingData = true
                            reloadKey++
                        }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            } else if (isSuccess) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Certificado Enviado!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sua atividade foi registrada no SIGAA.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Voltar para o Perfil", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // NATIVE FORM UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Dados da Atividade",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row {
                        Text("Tipo da Atividade Prática", fontWeight = FontWeight.Medium)
                        Text("*", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showDropdown = true }
                            .padding(16.dp)
                    ) {
                        val selectedText = options.find { it.first == selectedOptionId }?.second ?: "Selecione a atividade"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedText, 
                                modifier = Modifier.weight(1f), 
                                color = if (selectedOptionId.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    if (showDropdown) {
                        Dialog(
                            onDismissRequest = { showDropdown = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    Column {
                                        TopAppBar(
                                            title = { Text("Tipo de Atividade") },
                                            navigationIcon = {
                                                IconButton(onClick = { showDropdown = false }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                                                }
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            ) { dialogPadding ->
                                androidx.compose.foundation.lazy.LazyColumn(
                flingBehavior = rememberAppFlingBehavior(),
modifier = Modifier
                                        .fillMaxSize()
                                        .padding(dialogPadding),
                                    contentPadding = PaddingValues(bottom = 40.dp)
                                ) {
                                    itemsIndexed(
                                        items = options,
                                        key = { _, it -> it.first },
                                        contentType = { _, _ -> "certificado_option" }
                                    ) { index, option ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedOptionId = option.first
                                                    showDropdown = false
                                                }
                                                .padding(horizontal = 24.dp, vertical = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.second,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (selectedOptionId == option.first) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selecionado",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (index < options.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        Text("Observação / Descrição", fontWeight = FontWeight.Medium)
                        Text("*", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = observacao,
                        onValueChange = { observacao = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Descreva a atividade...") },
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        Text("Comprovante (PDF, Imagem)", fontWeight = FontWeight.Medium)
                        Text("*", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { filePickerLauncher.launch("*/*") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = selectedFileName ?: "Escolher arquivo...",
                                color = if (selectedFileName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            if (selectedOptionId.isEmpty() || selectedFileUri == null) {
                                com.example.util.AppSnackbarManager.show("Preencha o tipo de atividade e anexe o comprovante.")
                                return@Button
                            }
                            isSubmitting = true
                            
                            coroutineScope.launch {
                                val result = submitSigaaMultipart(
                                    context = context,
                                    actionUrl = actionUrl,
                                    viewState = viewStateParam,
                                    tipoAtividadeId = selectedOptionId,
                                    observacao = observacao,
                                    fileUri = selectedFileUri!!
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    isSuccess = true
                                } else {
                                    com.example.util.AppSnackbarManager.show(result.exceptionOrNull()?.message ?: "Erro desconhecido")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text("Em Produção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

suspend fun submitSigaaMultipart(
    context: Context,
    actionUrl: String,
    viewState: String,
    tipoAtividadeId: String,
    observacao: String,
    fileUri: Uri
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val fullActionUrl = if (actionUrl.startsWith("http")) actionUrl else "https://sigaa.ifal.edu.br$actionUrl"
        if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(fullActionUrl)) {
            throw java.io.IOException("Endereço de envio fora do SIGAA oficial bloqueado.")
        }
        val cookie = CookieManager.getInstance().getCookie("https://sigaa.ifal.edu.br") ?: ""
        if (cookie.isBlank()) throw java.io.IOException("Sessão do SIGAA não encontrada. Entre novamente.")
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .addNetworkInterceptor { chain ->
                if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(chain.request().url.toString())) {
                    throw java.io.IOException("Redirecionamento fora do SIGAA oficial bloqueado.")
                }
                chain.proceed(chain.request())
            }
            .build()

        var fileName = "comprovante.pdf"
        var fileLength = -1L
        context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) fileLength = cursor.getLong(sizeIndex)
            }
        }
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        val fileBody = object : RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()
            override fun contentLength() = fileLength
            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(fileUri)
                    ?: throw java.io.IOException("Não foi possível ler o arquivo selecionado.")
                input.use {
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = it.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("form", "form")
            .addFormDataPart("form:tipoAtividadeAutonoma", tipoAtividadeId)
            .addFormDataPart("form:observacoes", observacao)
            .addFormDataPart("form:submeter", "Próximo Passo >>")
            .addFormDataPart("javax.faces.ViewState", viewState)
            .addFormDataPart(
                "form:arquivo",
                fileName,
                fileBody
            )
            .build()

        var request = Request.Builder()
            .url(fullActionUrl)
            .addHeader("User-Agent", userAgent)
            .addHeader("Cookie", cookie)
            .post(requestBody)
            .build()

        var html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("O SIGAA recusou o envio (${response.code}).")
            response.body?.string().orEmpty()
        }

        // Check if there is a confirmation step (Próximo Passo >>)
        if (html.contains("Confirmar") || html.contains("Confirmar Cadastro") || html.contains("form:confirmar") || html.contains("form:btnConfirmar") || html.contains("Confirmar dados")) {
            val viewStateRegex = "name=\"javax\\.faces\\.ViewState\" id=\"javax\\.faces\\.ViewState\" value=\"(.*?)\"".toRegex()
            val newViewState = viewStateRegex.find(html)?.groupValues?.get(1) ?: viewState
            
            // Try to find the exact name of the confirm button
            val confirmRegex = "name=\"([^\"]+)\"[^>]*value=\"Confirmar\"".toRegex()
            val confirmName = confirmRegex.find(html)?.groupValues?.get(1) ?: "form:confirmar"

            val confirmBody = okhttp3.FormBody.Builder()
                .add("form", "form")
                .add(confirmName, "Confirmar")
                .add("javax.faces.ViewState", newViewState)
                .build()

            request = Request.Builder()
                .url(fullActionUrl)
                .addHeader("User-Agent", userAgent)
                .addHeader("Cookie", cookie)
                .post(confirmBody)
                .build()

            html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("O SIGAA recusou a confirmação (${response.code}).")
                response.body?.string().orEmpty()
            }
        }

        if (html.contains("sucesso") || html.contains("cadastrad") || html.contains("realizad") || html.contains("infoMensagensSucesso")) {
            Result.success("Sucesso")
        } else if (html.contains("class=\"infoMensagensErro\"") || html.contains("erro")) {
            Result.failure(Exception("O SIGAA retornou um erro ao cadastrar."))
        } else {
            Result.failure(Exception("O SIGAA não confirmou o cadastro. Verifique a situação antes de tentar novamente."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
