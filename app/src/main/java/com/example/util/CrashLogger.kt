package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val INTERNAL_LOG_FILE = "suap_crash_logs.txt"
    private const val AUTO_SAVE_FILE_NAME = "suap_app_logs_auto_saved.txt"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(context, thread, throwable)
            oldHandler?.uncaughtException(thread, throwable)
        }

        log(context, "INFO", "CrashLogger ativado. Gravando erros e logs automaticamente.")
        
        // Auto save logs upon initialization
        // autoSaveFullLogToDownloads(context)
    }

    fun log(context: Context, level: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logLine = StringBuilder()
            .append("[$timestamp] [$level] ")
            .append(message)

        if (throwable != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            logLine.append("\nStacktrace:\n").append(sw.toString())
        }
        logLine.append("\n")

        val logText = logLine.toString()
        Log.e(TAG, logText)

        try {
            val file = File(context.filesDir, INTERNAL_LOG_FILE)
            FileOutputStream(file, true).use { stream ->
                stream.write(logText.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        /*
        try {
            appendDirectToDownloads(context, logText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */
    }

    private fun logCrash(context: Context, thread: Thread, throwable: Throwable) {
        val message = "CRASH INESPERADO na Thread '${thread.name}': ${throwable.localizedMessage ?: throwable.message}"
        log(context, "FATAL", message, throwable)
        // Flush full log immediately on crash
        try {
            saveFullLogSync(context)
        } catch (_: Throwable) {}
    }

    private fun appendDirectToDownloads(context: Context, text: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val logFile = File(downloadsDir, AUTO_SAVE_FILE_NAME)
            FileOutputStream(logFile, true).use { stream ->
                stream.write(text.toByteArray())
            }
        } catch (_: Throwable) {
            try {
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                val file = File(extDir, AUTO_SAVE_FILE_NAME)
                FileOutputStream(file, true).use { stream ->
                    stream.write(text.toByteArray())
                }
            } catch (_: Throwable) {}
        }
    }

    fun autoSaveFullLogToDownloads(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                saveFullLogSync(context)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao auto-salvar logs em Downloads: ${e.message}")
            }
        }
    }

    private fun saveFullLogSync(context: Context) {
        val fullLogText = getAllLogs(context)
        val fileName = AUTO_SAVE_FILE_NAME

        // 1. MediaStore API for Android 10+ Downloads folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(fullLogText.toByteArray())
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. Direct File API in Downloads directory
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file, false).use { stream ->
                stream.write(fullLogText.toByteArray())
            }
        } catch (_: Throwable) {}

        // 3. Fallback to app External Documents
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
            sb.append("Erro ao ler arquivo local: ${e.message}\n\n")
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "500"))
            val bufferedReader = process.inputStream.bufferedReader()
            sb.append("--- MENSAGENS DE LOGCAT DO SISTEMA ---\n")
            var count = 0
            bufferedReader.forEachLine { line ->
                if (count < 600) {
                    sb.append(line).append("\n")
                    count++
                }
            }
            try { process.destroy() } catch (_: Throwable) {}
        } catch (e: Exception) {
            sb.append("Logcat indisponível: ${e.message}\n")
        }

        return sb.toString()
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, INTERNAL_LOG_FILE)
            if (file.exists()) file.delete()
        } catch (_: Throwable) {}
    }
}
