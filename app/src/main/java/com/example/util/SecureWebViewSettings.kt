package com.example.util

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

/** Applies the common hardening policy to remote SUAP/SIGAA WebViews. */
@Suppress("DEPRECATION") // Explicitly disable file URL access as defense in depth.
fun WebView.configureAppWebSettings(
    enableJavaScript: Boolean,
    enableDomStorage: Boolean
) {
    settings.apply {
        javaScriptEnabled = enableJavaScript
        domStorageEnabled = enableDomStorage
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }
    }
}
