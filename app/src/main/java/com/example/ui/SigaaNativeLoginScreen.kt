package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigaaNativeLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccessAction: () -> Unit
) {
    val context = LocalContext.current
    val credentialStore = remember { com.example.util.CredentialStore.getInstance(context) }

    var username by remember { mutableStateOf(credentialStore.getSigaaUsername()) }
    var password by remember { mutableStateOf(credentialStore.getSigaaPassword()) }
    var saveCredentials by remember { mutableStateOf(credentialStore.isSigaaSaveCredentialsEnabled()) }

    var isLoggingIn by remember { mutableStateOf(false) }
    var loginSuccess by remember { mutableStateOf(false) }
    
    var showWebView by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showWebView) "Navegador SIGAA" else "Login SIGAA", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showWebView) {
                            showWebView = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showWebView) {
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
                                    if (url != null && com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(url)) {
                                        if (url.contains("logar.do") || url.endsWith("sigaa/") || url.contains("modo=classico") || url.contains("verTelaLogin.do")) {
                                            val safeUsername = org.json.JSONObject.quote(username)
                                            val safePassword = org.json.JSONObject.quote(password)
                                            val js = """
                                                javascript:(function() {
                                                    var userField = document.querySelector('input[name="user.login"]');
                                                    var passField = document.querySelector('input[name="user.senha"]');
                                                    if (userField && userField.value === '') { userField.value = $safeUsername; }
                                                    if (passField && passField.value === '') { passField.value = $safePassword; }
                                                    var form = document.querySelector('form');
                                                    if (form && userField && passField && userField.value !== '' && passField.value !== '') {
                                                        form.submit();
                                                    }
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(js, null)
                                        } else if (url.contains("discente.jsf")) {
                                            CookieManager.getInstance().flush()
                                            coroutineScope.launch {
                                                delay(500) // allow cookies to settle
                                                onLoginSuccessAction()
                                            }
                                        }
                                    }
                                }
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, false)

                            loadUrl("https://sigaa.ifal.edu.br/sigaa/verTelaLogin.do")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isLoggingIn) {
                // Hidden WebView to perform the login
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

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val urlStr = request?.url?.toString()
                                    if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(urlStr)) {
                                        return true
                                    }
                                    return false
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {
                                    if (!com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(url)) {
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url != null && com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(url)) {
                                        if (url.contains("logar.do") || url.endsWith("sigaa/") || url.contains("modo=classico") || url.contains("verTelaLogin.do")) {
                                            // Inject JS to login
                                            val safeUsername = org.json.JSONObject.quote(username)
                                            val safePassword = org.json.JSONObject.quote(password)
                                            val js = """
                                                javascript:(function() {
                                                    var userField = document.querySelector('input[name="user.login"]');
                                                    var passField = document.querySelector('input[name="user.senha"]');
                                                    if (userField) { userField.value = $safeUsername; }
                                                    if (passField) { passField.value = $safePassword; }
                                                    var form = document.querySelector('form');
                                                    if (form && userField && passField) {
                                                        form.submit();
                                                    }
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(js, null)
                                        } else if (url.contains("discente.jsf")) {
                                            // Success!
                                            CookieManager.getInstance().flush()
                                            coroutineScope.launch {
                                                delay(500) // wait for cookies to be saved
                                                loginSuccess = true
                                                isLoggingIn = false
                                                onLoginSuccessAction()
                                            }
                                        }
                                    }
                                }
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, false)

                            loadUrl("https://sigaa.ifal.edu.br/sigaa/verTelaLogin.do")
                        }
                    },
                    modifier = Modifier.size(1.dp) // Basically hidden
                )

                if (loginSuccess) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Login realizado com sucesso!", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Voltar")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Conectando ao SIGAA em 2º plano...")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { isLoggingIn = false }) {
                            Text("Cancelar")
                        }
                    }
                }
            } else {
                if (loginSuccess) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Login realizado com sucesso!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Voltar")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Acesso ao SIGAA",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuário") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var showPassword by remember { mutableStateOf(false) }
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                androidx.compose.material3.IconButton(onClick = { showPassword = !showPassword }) {
                                    androidx.compose.material3.Text(text = if (showPassword) "🙈" else "👁️")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = saveCredentials,
                                onCheckedChange = { saveCredentials = it }
                            )
                            Text("Salvar usuário e senha")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                val credentialsSaved = !saveCredentials ||
                                    credentialStore.setSigaaCredentials(username, password, true)
                                if (!credentialsSaved) {
                                    com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                                } else {
                                    if (!saveCredentials) credentialStore.clearSigaaCredentials()
                                    isLoggingIn = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = username.isNotBlank() && password.isNotBlank()
                        ) {
                            Text("Entrar (Oculto)", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                val credentialsSaved = !saveCredentials ||
                                    credentialStore.setSigaaCredentials(username, password, true)
                                if (!credentialsSaved) {
                                    com.example.util.AppSnackbarManager.show("Não foi possível proteger as credenciais neste dispositivo.")
                                } else {
                                    if (!saveCredentials) credentialStore.clearSigaaCredentials()
                                    showWebView = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Login via Navegador (Clássico)")
                        }
                    }
                }
            }
        }
    }
}
