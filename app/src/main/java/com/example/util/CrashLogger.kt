package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val INTERNAL_LOG_FILE = "suap_crash_logs.txt"
    private const val AUTO_SAVE_FILE_NAME = "suap_app_logs_auto_saved.txt"
    private const val MAX_INTERNAL_LOG_BYTES = 1024L * 1024L
    private const val RETAINED_LOG_BYTES = 768 * 1024
    private const val MAX_SHARE_REPORT_BYTES = 128 * 1024
    private const val MAX_CRASH_CAUSES = 5
    private const val MAX_CRASH_FRAMES_PER_CAUSE = 8
    private const val MAX_CRASH_TRACE_CHARS = 4_000
    private val fileLock = Any()
    private var isInitialized = false
    private val emailPattern = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")
    private val urlPattern = Pattern.compile("(?i)https?://[^\\s)]+")
    private val authHeaderPattern = Pattern.compile("(?im)\\b(cookie|set-cookie|authorization)[\"']?\\s*[:=].*$")
    private val quotedSecretPattern = Pattern.compile(
        "(?i)([\"']?\\b(?:csrfmiddlewaretoken|csrftoken|sessionid|access_token|refresh_token|token|password|senha)\\b[\"']?\\s*[:=]\\s*)([\"'])(.*?)\\2"
    )
    private val secretPattern = Pattern.compile(
        "(?i)([\"']?\\b(?:csrfmiddlewaretoken|csrftoken|sessionid|access_token|refresh_token|token|password|senha)\\b[\"']?\\s*[:=]\\s*[\"']?)[^\"'\\s,;}]+"
    )
    private val identifierPattern = Pattern.compile("\\b\\d{8,14}\\b")
    private val cpfPattern = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b")
    private val htmlPattern = Pattern.compile("(?is)<\\s*(html|body|script|table|form|div|input|textarea)\\b")

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(context, thread, throwable)
            oldHandler?.uncaughtException(thread, throwable)
        }

        // Auto save logs upon initialization
        // autoSaveFullLogToDownloads(context)
    }

    fun log(context: Context, level: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logLine = StringBuilder()
            .append("[$timestamp] [$level] ")
            .append(sanitizeDiagnosticText(message).take(2_000))

        if (throwable != null) {
            // Keep type and source locations for diagnosis, but never record messages or values.
            logLine.append("\nException type: ")
                .append(throwable.javaClass.name.take(160))
                .append(diagnosticThrowableSummary(throwable))
        }
        logLine.append("\n")

        val logText = logLine.toString()
        AppLog.e(TAG, logText)

        try {
            val file = File(context.filesDir, INTERNAL_LOG_FILE)
            val bytes = logText.toByteArray()
            synchronized(fileLock) {
                if (file.length() + bytes.size > MAX_INTERNAL_LOG_BYTES) {
                    val tail = if (file.exists()) {
                        java.io.RandomAccessFile(file, "r").use { input ->
                            val length = input.length().coerceAtMost(RETAINED_LOG_BYTES.toLong()).toInt()
                            ByteArray(length).also { buffer ->
                                input.seek((input.length() - length).coerceAtLeast(0L))
                                input.readFully(buffer)
                            }
                        }
                    } else byteArrayOf()
                    FileOutputStream(file, false).use { it.write(tail) }
                }
                FileOutputStream(file, true).use { stream -> stream.write(bytes) }
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
        }

        /*
        try {
            appendDirectToDownloads(context, logText)
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
        }
        */
    }

    private fun logCrash(context: Context, thread: Thread, throwable: Throwable) {
        // Throwable messages may contain credentials, request data or page content.
        val message = "Falha inesperada na thread '${thread.name}'."
        log(context, "FATAL", message, throwable)
        // Flush full log immediately on crash
        try {
            saveFullLogSync(context)
        } catch (_: Throwable) {}
    }

    /** Safe crash detail: exception types and source locations only, never exception messages. */
    internal fun diagnosticThrowableSummary(throwable: Throwable): String {
        val summary = StringBuilder("\nStack trace (exception messages omitted):")
        val seen = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<Throwable, Boolean>()
        )
        var current: Throwable? = throwable
        var causeIndex = 0

        while (current != null && causeIndex < MAX_CRASH_CAUSES && seen.add(current)) {
            if (causeIndex > 0) {
                summary.append("\nCaused by: ").append(current.javaClass.name.take(160))
            }
            current.stackTrace.take(MAX_CRASH_FRAMES_PER_CAUSE).forEach { frame ->
                summary.append("\n at ")
                    .append(frame.className.take(160))
                    .append('.')
                    .append(frame.methodName.take(120))
                frame.fileName?.let { fileName ->
                    summary.append('(').append(fileName.take(80))
                    if (frame.lineNumber >= 0) summary.append(':').append(frame.lineNumber)
                    summary.append(')')
                }
            }
            current = current.cause
                ?: (current as? ExceptionInInitializerError)?.exception
            causeIndex++
        }

        return sanitizeDiagnosticText(summary.toString()).take(MAX_CRASH_TRACE_CHARS)
    }

    fun autoSaveFullLogToDownloads(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                saveFullLogSync(context)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao exportar relatório de diagnóstico")
            }
        }
    }

    private fun saveFullLogSync(context: Context) {
        val fullLogText = getAllLogs(context)
        val fileName = AUTO_SAVE_FILE_NAME
        try {
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!extDir.exists()) extDir.mkdirs()
            val file = File(extDir, fileName)
            FileOutputStream(file, false).use { stream ->
                stream.write(fullLogText.toByteArray())
            }
        } catch (_: Throwable) {}
    }

    fun getAllLogs(context: Context): String {
        val sb = StringBuilder()
        sb.append("=== REGISTRO DE ERROS E LOGS DA APLICAÇÃO SUAP ===\n")
        sb.append("Data do relatório: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
        sb.append("Dados pessoais, conteúdo de páginas e valores de autenticação são removidos deste relatório.\n\n")

        try {
            val file = File(context.filesDir, INTERNAL_LOG_FILE)
            if (file.exists()) {
                sb.append("--- LOGS DE CRASHES E ERROS SALVOS --- \n")
                sb.append(file.readText())
                sb.append("\n------------------------------------\n\n")
            } else {
                sb.append("Nenhum crash registrado anteriormente.\n\n")
            }
        } catch (e: Exception) {
            sb.append("Não foi possível ler os registros locais.\n\n")
        }

        return sanitizeDiagnosticText(sb.toString())
    }

    /** Creates a size-limited report in the only FileProvider-exposed cache directory. */
    fun createShareableReport(context: Context): File {
        val directory = File(context.cacheDir, "diagnostics")
        if (directory.exists()) directory.listFiles()?.forEach { it.delete() }
        directory.mkdirs()
        val report = File(directory, "suap-diagnostic-report.txt")
        val safeText = getAllLogs(context)
        val bytes = safeText.toByteArray(Charsets.UTF_8)
        val header = "Relatório limitado aos últimos registros.\n"
        val headerBytes = header.toByteArray(Charsets.UTF_8)
        val bounded = if (bytes.size <= MAX_SHARE_REPORT_BYTES) {
            bytes
        } else {
            var start = (bytes.size - (MAX_SHARE_REPORT_BYTES - headerBytes.size)).coerceAtLeast(0)
            while (start < bytes.size && (bytes[start].toInt() and 0xC0) == 0x80) start++
            headerBytes + bytes.copyOfRange(start, bytes.size)
        }
        report.writeBytes(bounded)
        return report
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, INTERNAL_LOG_FILE)
            if (file.exists()) file.delete()
        } catch (_: Throwable) {}
    }

    internal fun sanitizeDiagnosticText(value: String): String {
        if (htmlPattern.matcher(value).find()) {
            return "[conteúdo de página omitido]"
        }
        var sanitized = emailPattern.matcher(value).replaceAll("[email removido]")
        sanitized = urlPattern.matcher(sanitized).replaceAll("[URL removida]")
        sanitized = authHeaderPattern.matcher(sanitized).replaceAll("\$1=[cabeçalho removido]")
        sanitized = quotedSecretPattern.matcher(sanitized).replaceAll("\$1\$2[removido]\$2")
        sanitized = secretPattern.matcher(sanitized).replaceAll("\$1[removido]")
        sanitized = cpfPattern.matcher(sanitized).replaceAll("[CPF removido]")
        return identifierPattern.matcher(sanitized).replaceAll("[identificador removido]")
    }
}
