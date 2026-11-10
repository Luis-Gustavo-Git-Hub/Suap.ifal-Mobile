package com.example.ui

import com.example.util.configureAppWebSettings

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SuapWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        configureAppWebSettings(enableJavaScript = true, enableDomStorage = true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val urlStr = request?.url?.toString()
                                if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(urlStr)) {
                                    return true
                                }
                                return false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(url)) {
                                    return true
                                }
                                return false
                            }
                        }
                        if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(url)) {
                            loadUrl(url)
                        }
                    }
                },
                update = { webView ->
                    // nothing to update
                }
            )
        }
    }
}
