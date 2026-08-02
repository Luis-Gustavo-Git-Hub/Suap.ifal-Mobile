package com.example.ui
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
                        .padding(top = 48.dp, start = 16.dp),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                Text("Login SUAP", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                
                OutlinedTextField(
                    value = inputMatricula,
                    onValueChange = { inputMatricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showPassword by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                OutlinedTextField(
                    value = inputSenha,
                    onValueChange = { inputSenha = it },
                    label = { Text("Senha") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { showPassword = !showPassword }) {
                            androidx.compose.material3.Text(text = if (showPassword) "🙈" else "👁️")
                        }
                    }
                )
                
                var termsAccepted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("terms_accepted", false)) }
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
                                Text("Fechar")
                            }
                        }
                    )
                }
                
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .clickable { showTermsDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { 
                        onSaveCredentials(inputMatricula, inputSenha)
                        isLoading = true
                        hiddenWebView = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = termsAccepted
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                    } else {
                        Text("Entrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { showWebView = true }, enabled = termsAccepted) {
                    Text("Conectar via navegador")
                }
                

            }
        }
    }
}

}

private fun getDemoHtml(): String {
    return """
        <html>
            <div class="title-container">
                <h2></h2>
            </div>
            <dl class="definition-list">
                <div class="list-item"><dt>Nome</dt><dd></dd></div>
                <div class="list-item"><dt>Matrícula</dt><dd></dd></div>
                <div class="list-item"><dt>Ingresso</dt><dd></dd></div>
                <div class="list-item"><dt>Curso</dt><dd></dd></div>
            </dl>
            <table class="bordered">
                <tbody>
                    <tr>
                        <td></td>
                        <td>Matemática</td><td></td><td></td><td>5</td><td></td><td>Cursando</td><td>80</td><td></td><td>90</td>
                        <td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>85</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td>Física</td><td></td><td></td><td>2</td><td></td><td>Cursando</td><td>70</td><td></td><td>75</td>
                        <td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>72</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td>Programação I</td><td></td><td></td><td>0</td><td></td><td>Cursando</td><td>95</td><td></td><td>100</td>
                        <td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>97</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td>Redes de Computadores</td><td></td><td></td><td>4</td><td></td><td>Cursando</td><td>60</td><td></td><td>65</td>
                        <td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>62</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td headers="th_total_faltas">11</td>
                        <td headers="th_frequencia">93.5 %</td>
                    </tr>
                </tfoot>
            </table>
        </html>
    """.trimIndent()
}
