package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.ui.components.*
import com.example.ui.theme.Spacing
import androidx.compose.material3.MaterialTheme

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
import androidx.compose.material3.Text
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha

import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    savedUsername: String = "",
    savedPassword: String = "",
    onBack: () -> Unit = {},
    onSaveCredentials: (String, String) -> Unit,
    onHtmlScraped: (String) -> Unit,
    onDemoClick: () -> Unit,
    isSilentSync: Boolean = false,
    onSyncFailed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    var showWebView by remember { mutableStateOf(false) }
    var hiddenWebView by remember { mutableStateOf(false) }
    var inputMatricula by remember { mutableStateOf(savedUsername) }
    var inputSenha by remember { mutableStateOf(savedPassword) }
    var isLoading by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isSilentSync) {
        if (isSilentSync && inputMatricula.isNotEmpty() && inputSenha.isNotEmpty()) {
            hiddenWebView = true
            isLoading = true
        } else if (isSilentSync) {
            onSyncFailed()
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(20000)
            if (isLoading) {
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
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    // Add a Javascript interface to get the HTML
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun processHTML(html: String) {
                            Log.d("SUAP_HTML", "HTML scraped length: ${html.length}")
                            post {
                                onHtmlScraped(html)
                            }
                        }
                        
                        @JavascriptInterface
                        fun saveCredentials(u: String, p: String) {
                            post {
                                onSaveCredentials(u, p)
                            }
                        }
                        @JavascriptInterface
                        fun showCaptcha() {
                            post {
                                showWebView = true
                                hiddenWebView = false
                                isLoading = false
                            }
                        }
                        @JavascriptInterface
                        fun loginFailed() {
                            post {
                                isLoading = false
                                hiddenWebView = false
                                if (isSilentSync) {
                                    onSyncFailed()
                                }
                            }
                        }

                    }, "HTMLOUT")

                    webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            if (url.contains("/accounts/login")) {
                                view.evaluateJavascript(
                                    "(function() {" +

                                    "    var err = document.querySelector('.errornote') || document.querySelector('.alert-danger');" +
                                    "    if(err && err.innerText.trim().length > 0) {" +
                                    "        window.HTMLOUT.loginFailed();" +
                                    "        return;" +
                                    "    }" +
                                    "    var captcha = document.querySelector('.g-recaptcha') || document.getElementById('g-recaptcha-response');" +
                                    "    if(captcha && captcha.offsetParent !== null) {" +
                                    "        window.HTMLOUT.showCaptcha();" +
                                    "    }" +
                                    "    var form = document.querySelector('form');" +
                                    "    if(form) {" +
                                    "        form.addEventListener('submit', function(e) {" +
                                    "            var u = document.getElementById('id_username');" +
                                    "            var p = document.getElementById('id_password');" +
                                    "            if(u && p) {" +
                                    "                window.HTMLOUT.saveCredentials(u.value, p.value);" +
                                    "            }" +
                                    "        });" +
                                    "    }" +
                                    "    var savedU = '" + inputMatricula.replace("\\", "\\\\").replace("'", "\\'") + "';" +
                                    "    var savedP = '" + inputSenha.replace("\\", "\\\\").replace("'", "\\'") + "';" +
                                    "    if (savedU && savedP) {" +
                                    "        var attempts = 0;" +
                                    "        var interval = setInterval(function() {" +
                                    "            var u = document.getElementById('id_username') || document.querySelector('input[name=\"username\"]');" +
                                    "            var p = document.getElementById('id_password') || document.querySelector('input[name=\"password\"]');" +
                                    "            if(u && p) {" +
                                    "                if(!u.value) u.value = savedU;" +
                                    "                if(!p.value) p.value = savedP;" +
                                    "                var btn = document.querySelector('input[type=\"submit\"], button[type=\"submit\"], button.primary');" +
                                    "                if (btn && " + hiddenWebView + " && (u.value === savedU && p.value === savedP)) btn.click();" +
                                    "                clearInterval(interval);" +
                                    "            }" +
                                    "            attempts++;" +
                                    "            if(attempts > 20) clearInterval(interval);" +
                                    "        }, 100);" +
                                    "    }" +
                                    "})();",
                                    null
                                )
                            }
                            if (url == "https://suap.ifal.edu.br/" || url == "https://suap.ifal.edu.br" || url.contains("/dashboard") || (url.startsWith("https://suap.ifal.edu.br/") && !url.contains("/login") && !url.contains("/edu/aluno/"))) {
                                view.loadUrl("https://suap.ifal.edu.br/edu/aluno/${inputMatricula.trim()}/?tab=boletim")
                            }
                            
                            if (url.contains("/edu/aluno/") && url.contains("tab=boletim")) {
                                view.evaluateJavascript(
                                    "(function() { return document.documentElement.outerHTML; })();"
                                ) { html ->
                                    val unescaped = html?.replace("\\u003C", "<")?.replace("\\\"", "\"")
                                    if (unescaped != null) {
                                        // Pass the HTML back to Android
                                        android.webkit.CookieManager.getInstance().flush()
                                        view.evaluateJavascript("window.HTMLOUT.processHTML(document.documentElement.outerHTML);", null)
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false
                        }
                    }
                    loadUrl("https://suap.ifal.edu.br/accounts/login/?next=/")
                }
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
                        .padding(top = 48.dp, start = Spacing.normal),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.back))
                }
            }
        }
        
        if (!showWebView) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("Entrar no SUAP", onBack)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(Spacing.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.normal)
            ) {
                
                AppTextField(
                    value = inputMatricula,
                    onValueChange = { inputMatricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth()
                )
                

                AppTextField(
                    value = inputSenha,
                    onValueChange = { inputSenha = it },
                    label = { Text("Senha") },
                    password = true, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                
                var termsAccepted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("terms_accepted", false)) }
                var showTermsDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                if (showTermsDialog) {
                    AppDialog(
                        onDismissRequest = { showTermsDialog = false },
                        title = { Text("Termos de Uso") },
                        text = {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = """Termos de Uso e Isenção de Responsabilidade
                                        
1. Natureza do Aplicativo
Este aplicativo (Suap.ifal Mobile Não Oficial) é um projeto independente e não possui vínculo oficial com o Instituto Federal de Alagoas (IFAL) ou com os desenvolvedores do sistema acadêmico SUAP original.

2. Coleta e Armazenamento de Dados
Suas credenciais (matrícula e senha) e demais dados acessados são armazenados exclusivamente de forma local e segura no seu próprio dispositivo. Este aplicativo NÃO envia suas informações para nenhum servidor de terceiros.

3. Funcionamento
O aplicativo atua como uma interface alternativa, realizando o login através do portal SUAP via WebView e extraindo (scraping) as informações estritamente para exibi-las na interface nativa do aplicativo.

4. Isenção de Responsabilidade
Ao utilizar este aplicativo, você entende e concorda que os desenvolvedores não se responsabilizam por:
- Quaisquer falhas de acesso, indisponibilidade temporária ou definitiva da conexão ao SUAP.
- Eventuais mudanças estruturais na página do SUAP que possam desabilitar ou impactar as funcionalidades deste app.
- Quaisquer consequências do uso inadequado do aplicativo ou pelo comprometimento da segurança do seu dispositivo físico.

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
                                Text(androidx.compose.ui.res.stringResource(com.example.R.string.close))
                            }
                        }
                    )
                }
                
                TermsCheckRow(checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it; prefs.edit().putBoolean("terms_accepted", it).apply() },
                        onReadTerms = { showTermsDialog = true })
                
                PrimaryButton(
                    onClick = { 
                        onSaveCredentials(inputMatricula, inputSenha)
                        isLoading = true
                        hiddenWebView = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = termsAccepted
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Entrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { showWebView = true }, enabled = termsAccepted) {
                    Text("Entrar pelo navegador")
                }
                

            }
        }
        }
    }
}

}
