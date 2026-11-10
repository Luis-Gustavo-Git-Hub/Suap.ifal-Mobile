package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLoggerSanitizationTest {
    @Test
    fun removesPersonalIdentifiersSecretsUrlsAndPageMarkup() {
        val sanitized = CrashLogger.sanitizeDiagnosticText(
            "Aluno 202512345678, CPF 123.456.789-10, aluno@ifal.edu.br, " +
                "https://suap.ifal.edu.br/perfil/202512345678"
        )
        val secretSanitized = CrashLogger.sanitizeDiagnosticText("cookie=abc123; sessionid=hidden")
        val isolatedSessionSanitized = CrashLogger.sanitizeDiagnosticText("sessionid=standalone-secret")
        val jsonSecretsSanitized = CrashLogger.sanitizeDiagnosticText(
            "{\"access_token\":\"json-secret\", \"cookie\":\"cookie-secret\"}"
        )
        val quotedPasswordSanitized = CrashLogger.sanitizeDiagnosticText("{\"senha\": \"com espaço secreto\"}")

        assertFalse(sanitized.contains("202512345678"))
        assertFalse(sanitized.contains("aluno@ifal.edu.br"))
        assertFalse(sanitized.contains("123.456.789-10"))
        assertFalse(sanitized.contains("https://suap"))
        assertFalse(secretSanitized.contains("abc123"))
        assertFalse(secretSanitized.contains("sessionid=hidden"))
        assertFalse(isolatedSessionSanitized.contains("standalone-secret"))
        assertFalse(jsonSecretsSanitized.contains("json-secret"))
        assertFalse(jsonSecretsSanitized.contains("cookie-secret"))
        assertFalse(quotedPasswordSanitized.contains("espaço secreto"))
        assertTrue(sanitized.contains("[identificador removido]"))
        assertTrue(sanitized.contains("[URL removida]"))
        assertTrue(CrashLogger.sanitizeDiagnosticText("<html><body>dados</body></html>").contains("omitido"))
    }

    @Test
    fun keepsSanitizedReportsLongEnoughForTheShareSizeLimit() {
        val sanitized = CrashLogger.sanitizeDiagnosticText("log ".repeat(3_000))

        assertTrue(sanitized.length > 2_000)
    }
}
