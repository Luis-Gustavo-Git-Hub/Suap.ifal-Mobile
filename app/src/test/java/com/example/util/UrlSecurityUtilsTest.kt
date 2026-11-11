package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityUtilsTest {
    @Test
    fun `login allowlist accepts SUAP gov br and approved captcha hosts over HTTPS`() {
        listOf(
            "https://suap.ifal.edu.br/accounts/login/",
            "https://sso.acesso.gov.br/authorize",
            "https://acesso.gov.br/",
            "https://www.google.com/recaptcha/api.js",
            "https://www.gstatic.com/recaptcha/releases/example/recaptcha__pt_br.js",
            "https://www.recaptcha.net/recaptcha/api.js"
        ).forEach { assertTrue("Should allow $it", UrlSecurityUtils.isAllowedLoginUrl(it)) }
    }

    @Test
    fun `login allowlist blocks HTTP and unapproved or deceptive hosts`() {
        listOf(
            "http://suap.ifal.edu.br/",
            "https://evil.example/",
            "https://suap.ifal.edu.br.evil.example/",
            "https://evilacesso.gov.br/",
            "javascript:alert(1)",
            "file:///etc/passwd"
        ).forEach { assertFalse("Should block $it", UrlSecurityUtils.isAllowedLoginUrl(it)) }
    }
}
