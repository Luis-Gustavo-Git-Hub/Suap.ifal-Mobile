package com.example.util

import java.net.URI
import android.net.Uri

/**
 * Security validation utility for WebViews with Javascript interfaces.
 * Restricts navigation exclusively to secure HTTPS endpoints under
 * official IFAL SUAP and SIGAA domains.
 */
object UrlSecurityUtils {

    private val loginHosts = setOf(
        "suap.ifal.edu.br",
        "sso.acesso.gov.br",
        "acesso.gov.br",
        "www.google.com",
        "www.gstatic.com",
        "www.recaptcha.net"
    )

    fun isAllowedLoginUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) && uri.host?.lowercase() in loginHosts
        } catch (_: Exception) {
            false
        }
    }

    fun isAllowedSuapUrl(url: String?): Boolean = isAllowedHttpsHost(url, "suap.ifal.edu.br")

    fun isAllowedSigaaUrl(url: String?): Boolean = isAllowedHttpsHost(url, "sigaa.ifal.edu.br")

    fun isAllowedIfalUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return isAllowedHttpsHost(url, "suap.ifal.edu.br", allowSubdomains = true) ||
            isAllowedHttpsHost(url, "sigaa.ifal.edu.br", allowSubdomains = true)
    }

    private fun isAllowedHttpsHost(url: String?, expectedHost: String, allowSubdomains: Boolean = false): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            scheme == "https" && (host == expectedHost || (allowSubdomains && host.endsWith(".$expectedHost")))
        } catch (_: Exception) {
            false
        }
    }
}
