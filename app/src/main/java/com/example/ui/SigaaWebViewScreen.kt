package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigaaWebViewScreen(
    title: String,
    url: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val credentialStore = remember { com.example.util.CredentialStore.getInstance(context) }

    // Read credentials from SIGAA settings, falling back to SUAP if SIGAA is empty
    val savedUsername = remember {
        (credentialStore.getSigaaUsername().takeIf { it.isNotBlank() }
            ?: credentialStore.getSuapUsername().takeIf { it.isNotBlank() } ?: "").trim()
    }
    val savedPassword = remember {
        (credentialStore.getSigaaPassword().takeIf { it.isNotBlank() }
            ?: credentialStore.getSuapPassword().takeIf { it.isNotBlank() } ?: "").trim()
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>("Conectando ao SIGAA...") }
    var loginAttempts by remember { mutableIntStateOf(0) }
    var hasRedirectedToTargetAfterLogin by remember { mutableStateOf(false) }
    var hasAutoClickedNovoRegistro by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentFilePathCallback?.onReceiveValue(arrayOf(uri))
        } else {
            currentFilePathCallback?.onReceiveValue(null)
        }
        currentFilePathCallback = null
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (!statusMessage.isNullOrEmpty()) {
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        loginAttempts = 0
                        hasRedirectedToTargetAfterLogin = false
                        hasAutoClickedNovoRegistro = false
                        webViewInstance?.reload()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = false
                        settings.allowFileAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

                        var autoNavigatedAlmoco = false

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

                            override fun onPageStarted(view: WebView?, currentUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, currentUrl, favicon)
                                isLoading = true
                                if (currentUrl != null) {
                                    if (currentUrl.contains("verTelaLogin.do") || currentUrl.contains("logar.do")) {
                                        statusMessage = "Fazendo login com credenciais salvas..."
                                    } else if (currentUrl.contains("discente.jsf") || currentUrl.contains("verPortalDiscente.do")) {
                                        statusMessage = "Autenticado! Carregando formulário..."
                                    } else if (currentUrl.contains("lista.jsf")) {
                                        statusMessage = "Acessando cadastro de certificado..."
                                    } else if (currentUrl.contains("form.jsf")) {
                                        statusMessage = null
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, currentUrl: String?) {
                                super.onPageFinished(view, currentUrl)
                                isLoading = false
                                CookieManager.getInstance().flush()

                                if (currentUrl == null) return

                                val isLoginPage = currentUrl.contains("verTelaLogin.do") ||
                                        currentUrl.contains("logar.do") ||
                                        currentUrl.contains("modo=classico") ||
                                        (currentUrl.endsWith("/sigaa/") && !currentUrl.contains("discente"))

                                // 1. Auto-login on SIGAA login page
                                if (isLoginPage) {
                                    if (savedUsername.isNotBlank() && savedPassword.isNotBlank()) {
                                        if (loginAttempts < 2) {
                                            loginAttempts++
                                            statusMessage = "Fazendo login no SIGAA..."
                                            val escapedUser = org.json.JSONObject.quote(savedUsername)
                                            val escapedPass = org.json.JSONObject.quote(savedPassword)
                                            val loginJs = """
                                                javascript:(function() {
                                                    var userField = document.querySelector('input[name="user.login"]');
                                                    var passField = document.querySelector('input[name="user.senha"]');
                                                    if (userField && passField) {
                                                        userField.value = $escapedUser;
                                                        passField.value = $escapedPass;
                                                        userField.dispatchEvent(new Event('input', { bubbles: true }));
                                                        userField.dispatchEvent(new Event('change', { bubbles: true }));
                                                        passField.dispatchEvent(new Event('input', { bubbles: true }));
                                                        passField.dispatchEvent(new Event('change', { bubbles: true }));
                                                        var submitBtn = document.querySelector('input[type="submit"]');
                                                        if (submitBtn) {
                                                            submitBtn.click();
                                                        } else {
                                                            var form = document.querySelector('form');
                                                            if (form) form.submit();
                                                        }
                                                    }
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(loginJs, null)
                                        } else {
                                            statusMessage = null
                                            com.example.util.AppSnackbarManager.show("Verifique seu usuário e senha do SIGAA no Perfil", actionLabel = "Ver Perfil")
                                        }
                                    } else {
                                        statusMessage = null
                                        com.example.util.AppSnackbarManager.show("Configure suas credenciais do SIGAA no Perfil para entrar sozinho", actionLabel = "Configurar")
                                    }
                                }

                                // 2. Handle redirection after successful login for "Adicionar Certificado"
                                if (title == "Adicionar Certificado") {
                                    val isPortalDiscente = currentUrl.contains("discente.jsf") ||
                                            currentUrl.contains("verPortalDiscente.do") ||
                                            currentUrl.contains("portais/discente")

                                    if (isPortalDiscente && !hasRedirectedToTargetAfterLogin) {
                                        hasRedirectedToTargetAfterLogin = true
                                        statusMessage = "Abrindo tela de Atividades Práticas..."
                                        view?.loadUrl("https://sigaa.ifal.edu.br/sigaa/tecnico/registro_atividade_pratica_profissional/lista.jsf")
                                        return
                                    }

                                    // 3. Automatically click "Cadastrar Novo Registro" on the list page
                                    if (currentUrl.contains("lista.jsf") && !currentUrl.contains("form.jsf") && !hasAutoClickedNovoRegistro) {
                                        hasAutoClickedNovoRegistro = true
                                        statusMessage = "Abrindo formulário de cadastro..."
                                        val clickJs = """
                                            javascript:(function() {
                                                function tryClick() {
                                                    var inputs = document.querySelectorAll('input[type="submit"], input[type="button"], button');
                                                    for (var i = 0; i < inputs.length; i++) {
                                                        var val = (inputs[i].value || inputs[i].innerText || inputs[i].title || '').toLowerCase();
                                                        if (val.indexOf('cadastrar novo') !== -1 || val.indexOf('novo registro') !== -1 || val.indexOf('cadastrar') !== -1 || val.indexOf('nova atividade') !== -1) {
                                                            inputs[i].click();
                                                            return true;
                                                        }
                                                    }
                                                    var links = document.querySelectorAll('a');
                                                    for (var i = 0; i < links.length; i++) {
                                                        var text = (links[i].innerText || links[i].title || '').toLowerCase();
                                                        var href = (links[i].href || '').toLowerCase();
                                                        if (text.indexOf('cadastrar novo') !== -1 || text.indexOf('novo registro') !== -1 || text.indexOf('cadastrar atividade') !== -1 || text.indexOf('cadastrar') !== -1 || href.indexOf('form.jsf') !== -1) {
                                                            links[i].click();
                                                            return true;
                                                        }
                                                    }
                                                    var imgs = document.querySelectorAll('img');
                                                    for (var i = 0; i < imgs.length; i++) {
                                                        var info = (imgs[i].alt || imgs[i].title || imgs[i].src || '').toLowerCase();
                                                        if (info.indexOf('cadastrar') !== -1 || info.indexOf('novo') !== -1 || info.indexOf('add') !== -1) {
                                                            var p = imgs[i].closest('a');
                                                            if (p) {
                                                                p.click();
                                                                return true;
                                                            }
                                                            imgs[i].click();
                                                            return true;
                                                        }
                                                    }
                                                    return false;
                                                }
                                                if (!tryClick()) {
                                                    setTimeout(tryClick, 400);
                                                    setTimeout(tryClick, 1000);
                                                    setTimeout(tryClick, 2000);
                                                }
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(clickJs, null)
                                    }

                                    if (currentUrl.contains("form.jsf")) {
                                        statusMessage = null
                                    }
                                }

                                // 4. Retain Marcar Almoco compatibility
                                if (title == "Marcar Almoço" && !autoNavigatedAlmoco && currentUrl.contains("discente.jsf")) {
                                    autoNavigatedAlmoco = true
                                    val js = """
                                        javascript:(function() {
                                            var form = document.forms['menu:form_menu_discente'];
                                            if (form) {
                                                var action = 'menu_form_menu_discente_j_id_jsp_1051041857_101_menu:A]#{ comparecimentoRefeitorioMBean.iniciar }';
                                                var scripts = document.getElementsByTagName('script');
                                                for (var i = 0; i < scripts.length; i++) {
                                                    var match = scripts[i].innerHTML.match(/'([^']+#\{ comparecimentoRefeitorioMBean\.iniciar \})/);
                                                    if (match) { action = match[1]; break; }
                                                }
                                                form.elements['jscook_action'].value = action;
                                                form.submit();
                                            }
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                currentFilePathCallback?.onReceiveValue(null)
                                currentFilePathCallback = filePathCallback
                                filePickerLauncher.launch("*/*")
                                return true
                            }
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, false)

                        if (com.example.util.UrlSecurityUtils.isAllowedSigaaUrl(url)) {
                            loadUrl(url)
                        } else {
                            isLoading = false
                            statusMessage = "Endereço bloqueado por segurança."
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            AnimatedVisibility(
                visible = !statusMessage.isNullOrEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

