package com.example.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigaaWebViewScreen(
    title: String,
    url: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                        
                        var autoNavigated = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (title == "Marcar Almoço" && !autoNavigated && url?.contains("discente.jsf") == true) {
                                    autoNavigated = true
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
                        
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
