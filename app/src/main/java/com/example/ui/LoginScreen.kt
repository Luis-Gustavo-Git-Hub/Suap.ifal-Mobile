package com.example.ui

import com.example.util.configureAppWebSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.util.SyncFailureReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LoginMethod { CREDENTIALS, GOV_BR }

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    savedUsername: String = "",
    savedPassword: String = "",
    onBack: () -> Unit = {},
    onSaveCredentials: (String, String) -> Unit,
    onHtmlScraped: (String) -> Unit,
    onGovBrLogin: (String) -> Unit = {},
    onDemoClick: () -> Unit,
    isSilentSync: Boolean = false,
    onSyncFailed: (SyncFailureReason) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    var showWebView by remember { mutableStateOf(false) }
    var hiddenWebView by remember { mutableStateOf(false) }
    var loginMethod by remember { mutableStateOf(LoginMethod.CREDENTIALS) }
    var showMatriculaDialog by remember { mutableStateOf(false) }
    var matriculaManual by remember { mutableStateOf("") }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var inputMatricula by remember { mutableStateOf(savedUsername) }
    var inputSenha by remember { mutableStateOf(savedPassword) }
    var isLoading by remember { mutableStateOf(false) }
    var isProcessingHtml by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(prefs.getBoolean("terms_accepted", false)) }
    val focusManager = LocalFocusManager.current
    val canSubmit = termsAccepted && inputMatricula.isNotBlank() && inputSenha.isNotEmpty() && !isLoading

    fun startLogin() {
        if (!canSubmit) return
        com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: início do login por matrícula/senha; abrindo portal para autenticação.")
        focusManager.clearFocus()
        errorMessage = null
        onSaveCredentials(inputMatricula.trim(), inputSenha)
        isProcessingHtml = false
        loginMethod = LoginMethod.CREDENTIALS
        isLoading = true
        hiddenWebView = true
        showWebView = false
    }

    fun startGovBrLogin() {
        com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: início do login gov.br.")
        errorMessage = null
        loginMethod = LoginMethod.GOV_BR
        isLoading = false
        hiddenWebView = false
        showWebView = true
    }

    fun reportFailure(reason: SyncFailureReason, message: String) {
        com.example.util.CrashLogger.log(context, "WARN", "SUAP_LOGIN_UI: falha reason=$reason, modo=${if (isSilentSync) "silencioso" else loginMethod.name}.")
        isLoading = false
        isProcessingHtml = false
        hiddenWebView = false
        showWebView = false
        errorMessage = message
        if (reason == SyncFailureReason.SESSION_EXPIRED) {
            com.example.util.AppSnackbarManager.showSyncError(reason)
            com.example.util.NotificationHelper.sendNotification(
                context,
                "Sessão do SUAP expirou",
                "Entre novamente para sincronizar seus dados.",
                901
            )
        }
        if (isSilentSync) onSyncFailed(reason)
    }

    androidx.activity.compose.BackHandler(enabled = showWebView) {
        showWebView = false
        hiddenWebView = false
        isLoading = false
    }

    val hasSavedCredentials = savedUsername.isNotBlank() && savedPassword.isNotBlank()
    val tutorialAlreadyCompleted = prefs.getBoolean("suap_tutorial_completed", false)
    var showTutorial by remember {
        mutableStateOf(!hasSavedCredentials && !tutorialAlreadyCompleted && !isSilentSync)
    }

    if (showTutorial) {
        SuapTutorialDialog(
            onDismissRequest = {
                showTutorial = false
                prefs.edit().putBoolean("suap_tutorial_completed", true).apply()
            },
            onComplete = {
                showTutorial = false
                prefs.edit().putBoolean("suap_tutorial_completed", true).apply()
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(isSilentSync) {
        if (isSilentSync && inputMatricula.isNotEmpty() && inputSenha.isNotEmpty()) {
            hiddenWebView = true
            isLoading = true
        } else if (isSilentSync) {
            onSyncFailed(SyncFailureReason.NO_CREDENTIALS)
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLoading, isProcessingHtml) {
        if (isLoading && !isProcessingHtml) {
            kotlinx.coroutines.delay(20000)
            if (isSilentSync) {
                reportFailure(SyncFailureReason.TIMEOUT, "O SUAP demorou para responder.")
            } else {
                showWebView = true
                hiddenWebView = false
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showWebView || hiddenWebView) {
            AndroidView(
            factory = { context ->
                val hasDeliveredHtml = java.util.concurrent.atomic.AtomicBoolean(false)
                val credentialPostSubmitted = java.util.concurrent.atomic.AtomicBoolean(false)
            WebView(context).apply {
                    activeWebView = this
                    configureAppWebSettings(enableJavaScript = true, enableDomStorage = true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(
                        this,
                        loginMethod == LoginMethod.CREDENTIALS
                    )
                    
                    // The gov.br WebView intentionally has no JavaScript bridge.
                    if (loginMethod == LoginMethod.CREDENTIALS) addJavascriptInterface(object {
                        @JavascriptInterface
                        fun processHTML(html: String) {
                            if (!hasDeliveredHtml.compareAndSet(false, true)) return
                            com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: HTML do boletim recebido para leitura; bytes=${html.length}.")
                            com.example.util.AppLog.d("SUAP_HTML", "HTML scraped length: ${html.length}")
                            post {
                                isProcessingHtml = true
                                isLoading = true
                                showWebView = false
                                onHtmlScraped(html)
                            }
                        }
                        
                        @JavascriptInterface
                        fun showCaptcha() {
                            com.example.util.CrashLogger.log(context, "WARN", "SUAP_LOGIN_UI: verificação CAPTCHA detectada na página de login.")
                            post {
                                if (isSilentSync) {
                                    reportFailure(SyncFailureReason.CAPTCHA_REQUIRED, "Conclua a verificação do SUAP para continuar.")
                                } else {
                                    showWebView = true
                                    hiddenWebView = false
                                    isLoading = false
                                }
                            }
                        }
                        @JavascriptInterface
                        fun loginFailed() {
                            com.example.util.CrashLogger.log(context, "WARN", "SUAP_LOGIN_UI: portal exibiu erro no formulário de login.")
                            post {
                                reportFailure(SyncFailureReason.INVALID_CREDENTIALS, "Não foi possível entrar. Confira sua matrícula e senha.")
                            }
                        }
                        @JavascriptInterface
                        fun autoLoginSubmitted() {
                            credentialPostSubmitted.set(true)
                            com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: credenciais preenchidas e formulário enviado automaticamente.")
                        }
                        @JavascriptInterface
                        fun loginFormUnavailable() {
                            com.example.util.CrashLogger.log(context, "ERROR", "SUAP_LOGIN_UI: campos do formulário não encontrados após aguardar o carregamento.")
                            post {
                                reportFailure(SyncFailureReason.GENERIC, "Não foi possível localizar o formulário de login do SUAP. Tente novamente.")
                            }
                        }
                        @JavascriptInterface
                        fun loginNotConfirmed() {
                            com.example.util.CrashLogger.log(context, "ERROR", "SUAP_LOGIN_UI: formulário enviado, mas o portal não confirmou a autenticação.")
                            post {
                                reportFailure(SyncFailureReason.GENERIC, "O SUAP não confirmou o login. Confira suas credenciais e tente novamente.")
                            }
                        }

                    }, "HTMLOUT")

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                com.example.util.CrashLogger.log(context, "ERROR", "SUAP_WEBVIEW: falha de rede no documento principal; código=${error?.errorCode ?: -1}.")
                                val reason = when (error?.errorCode) {
                                    ERROR_HOST_LOOKUP, ERROR_CONNECT -> SyncFailureReason.NO_INTERNET
                                    ERROR_TIMEOUT -> SyncFailureReason.TIMEOUT
                                    else -> SyncFailureReason.GENERIC
                                }
                                reportFailure(reason, "Não foi possível acessar o SUAP. Verifique sua conexão e tente novamente.")
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            if (request?.isForMainFrame == true) {
                                com.example.util.CrashLogger.log(context, "ERROR", "SUAP_WEBVIEW: resposta HTTP não-success no documento principal; status=${errorResponse?.statusCode ?: -1}.")
                                reportFailure(SyncFailureReason.SUAP_UNAVAILABLE, "O SUAP está indisponível no momento. Tente novamente em instantes.")
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            handler?.cancel()
                            reportFailure(SyncFailureReason.GENERIC, "Não foi possível validar a conexão segura. Confira a rede e tente novamente.")
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            if (isProcessingHtml) return
                            if (!com.example.util.UrlSecurityUtils.isAllowedLoginUrl(url)) return
                            val pageUri = android.net.Uri.parse(url)
                            val pagePath = pageUri.path.orEmpty()
                            val safeRoute = when {
                                pageUri.host != "suap.ifal.edu.br" -> "host-nao-oficial"
                                pagePath.startsWith("/accounts/login") -> "/accounts/login"
                                Regex("^/edu/aluno/\\d+/?$").matches(pagePath) -> "/edu/aluno/{matricula}"
                                else -> "/outra-rota"
                            }
                            com.example.util.CrashLogger.log(context, "INFO", "SUAP_WEBVIEW: página carregada; rota=$safeRoute, modo=${if (isSilentSync) "silencioso" else loginMethod.name}.")
                            if (isSilentSync && pagePath.contains("/accounts/login") &&
                                (inputMatricula.isBlank() || inputSenha.isBlank())
                            ) {
                                com.example.util.CrashLogger.log(context, "WARN", "SUAP_LOGIN_UI: sessão expirou e não há credenciais para renovação automática.")
                                reportFailure(SyncFailureReason.NO_CREDENTIALS, "Entre com sua matrícula e senha para renovar a sessão do SUAP.")
                                return
                            } else if (isSilentSync && pagePath.contains("/accounts/login")) {
                                com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: sessão expirada; tentando renovar automaticamente com credenciais salvas.")
                            }
                            if (pagePath.contains("/accounts/login") && loginMethod == LoginMethod.CREDENTIALS) {
                                val username = org.json.JSONObject.quote(inputMatricula.trim())
                                val password = org.json.JSONObject.quote(inputSenha)
                                val submittedBefore = credentialPostSubmitted.get()
                                view.evaluateJavascript(
                                    "(function(){" +
                                        "var alreadySubmitted=$submittedBefore;" +
                                        "if(alreadySubmitted){" +
                                            "var err=document.querySelector('.errornote,.alert-danger,.alert-error,.errorlist');" +
                                            "var message=err&&err.innerText?err.innerText:'';" +
                                            "if(/inválid|incorret|usuário|senha|credencial|invalid|incorrect/i.test(message)){window.HTMLOUT.loginFailed();return 'credentials_rejected';}" +
                                            "window.HTMLOUT.loginNotConfirmed();return 'login_not_confirmed';}" +
                                        "if(window.__suapAutoLoginRunning)return 'already_running';" +
                                        "window.__suapAutoLoginRunning=true;var attempts=0;" +
                                        "var timer=setInterval(function(){" +
                                            "var captcha=document.querySelector('.g-recaptcha')||document.getElementById('g-recaptcha-response');" +
                                            "if(captcha&&captcha.offsetParent!==null){clearInterval(timer);window.HTMLOUT.showCaptcha();return;}" +
                                            "var u=document.querySelector('input[name=username],#id_username');" +
                                            "var p=document.querySelector('input[name=password],#id_password');" +
                                            "if(u&&p){var f=u.form||p.form;if(!f){clearInterval(timer);window.HTMLOUT.loginFormUnavailable();return;}" +
                                                "u.value=$username;p.value=$password;" +
                                                "u.dispatchEvent(new Event('input',{bubbles:true}));" +
                                                "p.dispatchEvent(new Event('input',{bubbles:true}));" +
                                                "window.__suapAutoLoginSubmitted=true;clearInterval(timer);" +
                                                "window.HTMLOUT.autoLoginSubmitted();" +
                                                "var b=f.querySelector('input[type=submit],button[type=submit],button.primary,button.btn-primary');" +
                                                "if(b)b.click();else if(f.requestSubmit)f.requestSubmit();else f.submit();return;}" +
                                            "if(++attempts>=20){clearInterval(timer);window.HTMLOUT.loginFormUnavailable();}" +
                                        "},100);return 'polling';" +
                                    "})()"
                                ) { result ->
                                    val outcome = result?.trim('"') ?: "sem_resposta"
                                    com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: verificação automática do formulário iniciada; resultado=$outcome.")
                                }
                            }
                            val studentPath = Regex("""^/edu/aluno/(\d+)/?""").find(pagePath)
                            val isPostLoginLanding = pagePath == "/" || pagePath.contains("/dashboard") ||
                                (pagePath.startsWith("/") && !pagePath.contains("/login") && !pagePath.startsWith("/edu/aluno/"))
                            if (isPostLoginLanding && inputMatricula.isNotBlank()) {
                                com.example.util.CrashLogger.log(context, "INFO", "SUAP_LOGIN_UI: login redirecionou para área autenticada; abrindo boletim automaticamente.")
                                view.loadUrl("https://suap.ifal.edu.br/edu/aluno/${inputMatricula.trim()}/?tab=boletim")
                                return
                            }
                            if (studentPath != null && pageUri.getQueryParameter("tab") != "boletim") {
                                inputMatricula = studentPath.groupValues[1]
                                view.loadUrl("https://suap.ifal.edu.br/edu/aluno/${studentPath.groupValues[1]}/?tab=boletim")
                            } else if (studentPath != null && pageUri.getQueryParameter("tab") == "boletim") {
                                android.webkit.CookieManager.getInstance().flush()
                                if (loginMethod == LoginMethod.CREDENTIALS) {
                                    view.evaluateJavascript("window.HTMLOUT.processHTML(document.documentElement.outerHTML);", null)
                                } else if (!isProcessingHtml) {
                                    val matricula = studentPath.groupValues[1]
                                    val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
                                    isProcessingHtml = true
                                    val bulletinUrl = "https://suap.ifal.edu.br/edu/aluno/$matricula/?tab=boletim"
                                    view.post {
                                        coroutineScope.launch {
                                            val html = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val request = okhttp3.Request.Builder()
                                                        .url(bulletinUrl)
                                                        .header("Cookie", cookies)
                                                        .build()
                                                    okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                                                        val finalPath = response.request.url.encodedPath
                                                        if (!response.isSuccessful || finalPath.startsWith("/accounts/login")) null
                                                        else response.body?.string()
                                                    }
                                                }.getOrNull()
                                            }
                                            if (html.isNullOrBlank()) {
                                                isProcessingHtml = false
                                                reportFailure(SyncFailureReason.SESSION_EXPIRED, "Não foi possível validar sua sessão. Entre novamente no SUAP.")
                                            } else {
                                                inputMatricula = matricula
                                                onSaveCredentials(matricula, "")
                                                onGovBrLogin(matricula)
                                                showWebView = false
                                                onHtmlScraped(html)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val urlStr = request?.url?.toString()
                            if (!com.example.util.UrlSecurityUtils.isAllowedLoginUrl(urlStr)) {
                                return true
                            }
                            return false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            if (!com.example.util.UrlSecurityUtils.isAllowedLoginUrl(url)) {
                                return true
                            }
                            return false
                        }
                    }
                    if (isSilentSync && savedUsername.matches(Regex("\\d+"))) {
                        loadUrl("https://suap.ifal.edu.br/edu/aluno/$savedUsername/?tab=boletim")
                    } else {
                        loadUrl("https://suap.ifal.edu.br/accounts/login/?next=/")
                    }
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.removeJavascriptInterface("HTMLOUT")
                if (activeWebView === webView) activeWebView = null
                webView.destroy()
            },
            modifier = if (hiddenWebView && !showWebView) Modifier.size(1.dp).alpha(0f) else Modifier.fillMaxSize()
        )
        
            if (showWebView && !hiddenWebView) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { 
                        showWebView = false 
                        isLoading = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 48.dp, start = 16.dp),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                if (loginMethod == LoginMethod.GOV_BR) {
                    TextButton(
                        onClick = { matriculaManual = inputMatricula; showMatriculaDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                    ) { Text("Informar matrícula") }
                }
            }
        }

        if (showMatriculaDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showMatriculaDialog = false },
                title = { Text("Matrícula do SUAP") },
                text = {
                    OutlinedTextField(
                        value = matriculaManual,
                        onValueChange = { matriculaManual = it.filter(Char::isDigit) },
                        label = { Text("Matrícula") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val matricula = matriculaManual.trim()
                        if (matricula.isNotEmpty()) {
                            inputMatricula = matricula
                            activeWebView?.loadUrl("https://suap.ifal.edu.br/edu/aluno/$matricula/?tab=boletim")
                            showMatriculaDialog = false
                        }
                    }) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(onClick = { showMatriculaDialog = false }) { Text("Cancelar") }
                }
            )
        }
        
        if (!showWebView) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 16.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }

            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Login SUAP",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = { showTutorial = true },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tutorial", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                OutlinedTextField(
                    value = inputMatricula,
                    onValueChange = { inputMatricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth().testTag("username_input"),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )

                var showPassword by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                OutlinedTextField(
                    value = inputSenha,
                    onValueChange = { inputSenha = it },
                    label = { Text("Senha") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { startLogin() }),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    }
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showTutorial = true },
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Primeiro acesso? Veja como preencher suas credenciais",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                var showTermsDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                if (showTermsDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showTermsDialog = false },
                        title = { Text("Termos de Uso") },
                        text = {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = """Termos de Uso e Isenção de Responsabilidade

1. Natureza do Aplicativo
Este aplicativo é um projeto independente, desenvolvido por alunos, e não possui vínculo oficial com a instituição (IFAL) ou com os desenvolvedores dos sistemas acadêmicos originais (SUAP/SIGAA).

2. Coleta e Armazenamento de Dados Locais
Suas credenciais institucionais (matrícula e senha) e os dados acadêmicos extraídos (boletins, faltas, horários) são armazenados EXCLUSIVAMENTE de forma local e criptografada no seu próprio dispositivo. O aplicativo não envia esses dados para nenhum servidor externo.

3. Funcionalidade de Amigos e Compartilhamento (Firebase)
Ao utilizar a funcionalidade "Amigos", você concorda que:
- O login é realizado de forma segura através da sua Conta Google.
- Apenas o seu E-mail e Foto de Perfil (em formato base64) serão armazenados de forma temporária ou persistente no banco de dados em nuvem (Firebase) estritamente para viabilizar a busca e conexão entre usuários.
- Sua matrícula, nome completo e senha institucionais NUNCA são compartilhados com o Firebase.
- Os "Nomes" (apelidos) definidos para os seus amigos são de sua total responsabilidade e ficam salvos apenas na sua conta local/nuvem privada.
- O compartilhamento de notas e boletins com amigos é opcional, explícito (requer ação manual) e de sua inteira responsabilidade.

4. Isenção de Responsabilidade
Ao utilizar este aplicativo, você entende e concorda que os desenvolvedores não se responsabilizam por:
- Quaisquer falhas de acesso, indisponibilidade temporária ou definitiva da conexão aos sistemas institucionais.
- Eventuais mudanças estruturais nas páginas oficiais que possam desabilitar ou impactar as funcionalidades deste app.
- Uso indevido do aplicativo, vazamento de notas compartilhadas voluntariamente por você, ou comprometimento da segurança do seu dispositivo físico.

Ao marcar a caixa de concordância, você declara ter lido, compreendido e aceitado todos os termos descritos acima.""".trimIndent(),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    termsAccepted = true
                                    prefs.edit().putBoolean("terms_accepted", true).apply()
                                    showTermsDialog = false 
                                }
                            ) {
                                Text("Aceitar Termos")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showTermsDialog = false }
                            ) {
                                Text("Fechar")
                            }
                        }
                    )
                }
                
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
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
                                prefs.edit().putBoolean("terms_accepted", termsAccepted).apply()
                            }
                            .padding(12.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = termsAccepted, 
                            onCheckedChange = { 
                                termsAccepted = it 
                                prefs.edit().putBoolean("terms_accepted", it).apply()
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Concordo com os termos de uso", 
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Isento o app de responsabilidade", 
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ler Termos de Uso",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    color = if (termsAccepted) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                ),
                                modifier = Modifier
                                    .clip(androidx.compose.material3.MaterialTheme.shapes.extraSmall)
                                    .clickable { showTermsDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { startLogin() },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_button"),
                    enabled = canSubmit
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isProcessingHtml) "Salvando dados…" else "Conectando ao SUAP…")
                    } else {
                        Text("Entrar")
                    }
                }
                
                androidx.compose.material3.OutlinedButton(
                    onClick = { startGovBrLogin() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = termsAccepted && !isLoading
                ) {
                    Text("Entrar com gov.br")
                }
                

            }
        }
    }
}

}
