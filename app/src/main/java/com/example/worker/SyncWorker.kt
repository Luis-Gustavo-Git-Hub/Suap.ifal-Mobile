package com.example.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Aviso
import com.example.data.Subject
import com.example.data.SubjectRepository
import com.example.util.NotificationHelper
import com.example.util.SuapParser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val repository: SubjectRepository by lazy {
        SubjectRepository(AppDatabase.getDatabase(appContext).subjectDao())
    }

    override suspend fun doWork(): Result {
        val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val credPrefs = applicationContext.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE)

        val notificationsEnabled = appPrefs.getBoolean("notifications_enabled", false)
        if (!notificationsEnabled) return Result.success()

        val username = credPrefs.getString("username", "") ?: ""
        val password = credPrefs.getString("password", "") ?: ""

        if (username.isEmpty() || password.isEmpty()) return Result.failure()

        Log.d("SyncWorker", "Starting background sync at 19h for $username")

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            // 1. Get Login Page for CSRF Token
            val loginPageRequest = Request.Builder()
                .url("https://suap.ifal.edu.br/accounts/login/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            val loginPageResponse = client.newCall(loginPageRequest).execute()
            if (!loginPageResponse.isSuccessful) return Result.retry()
            
            val loginHtml = loginPageResponse.body?.string() ?: ""
            val csrfToken = Jsoup.parse(loginHtml).select("input[name=csrfmiddlewaretoken]").`val`()
            
            val cookies = loginPageResponse.headers("Set-Cookie")
            val sessionCookie = cookies.find { it.contains("csrftoken") } ?: ""

            // 2. Perform Login
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("csrfmiddlewaretoken", csrfToken)
                .add("this_is_the_login_form", "1")
                .add("next", "/")
                .build()

            val loginRequest = Request.Builder()
                .url("https://suap.ifal.edu.br/accounts/login/")
                .post(formBody)
                .header("Cookie", sessionCookie)
                .header("Referer", "https://suap.ifal.edu.br/accounts/login/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            val loginResponse = client.newCall(loginRequest).execute()
            
            // Collect cookies from login response
            val authCookies = loginResponse.headers("Set-Cookie").joinToString("; ") { it.split(";")[0] }
            val finalCookie = if (authCookies.isNotEmpty()) authCookies else sessionCookie

            // 3. Fetch Grades (Boletim)
            val boletimRequest = Request.Builder()
                .url("https://suap.ifal.edu.br/edu/aluno/$username/?tab=boletim")
                .header("Cookie", finalCookie)
                .build()
            
            val boletimResponse = client.newCall(boletimRequest).execute()
            if (boletimResponse.isSuccessful) {
                val boletimHtml = boletimResponse.body?.string() ?: ""
                val newSubjects = SuapParser.parseSubjects(Jsoup.parse(boletimHtml))
                checkGradesChanges(newSubjects)
            }

            // 4. Fetch Avisos
            val avisosRequest = Request.Builder()
                .url("https://suap.ifal.edu.br/admin/edu/mensagementrada/?tab=tab_todas")
                .header("Cookie", finalCookie)
                .build()

            val avisosResponse = client.newCall(avisosRequest).execute()
            if (avisosResponse.isSuccessful) {
                val avisosHtml = avisosResponse.body?.string() ?: ""
                val newAvisos = SuapParser.parseAvisos(Jsoup.parse(avisosHtml))
                checkAvisosChanges(newAvisos)
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            return Result.retry()
        }
    }

    private suspend fun checkGradesChanges(newSubjects: List<Subject>) {
        val oldSubjects = repository.getAllSubjectsSync()
        if (oldSubjects.isEmpty()) {
            repository.insertAll(newSubjects)
            return
        }

        val changes = mutableListOf<String>()
        for (newSub in newSubjects) {
            val oldSub = oldSubjects.find { it.name == newSub.name }
            if (oldSub != null) {
                if (oldSub.grade1 != newSub.grade1 || oldSub.grade2 != newSub.grade2 || 
                    oldSub.grade3 != newSub.grade3 || oldSub.grade4 != newSub.grade4 ||
                    oldSub.finalGrade != newSub.finalGrade) {
                    changes.add(newSub.name)
                }
            }
        }

        if (changes.isNotEmpty()) {
            val message = if (changes.size == 1) "Nova nota em ${changes[0]}" else "Novas notas em ${changes.size} disciplinas"
            NotificationHelper.sendNotification(applicationContext, "Atualização de Notas", message, 1001)
            repository.clearAll()
            repository.insertAll(newSubjects)
        }
    }

    private fun checkAvisosChanges(newAvisos: List<Aviso>) {
        val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastAvisoSubject = appPrefs.getString("last_aviso_subject", "") ?: ""
        if (newAvisos.isNotEmpty()) {
            val latest = newAvisos[0]
            if (latest.subject != lastAvisoSubject) {
                NotificationHelper.sendNotification(
                    applicationContext, 
                    "Novo Aviso: ${latest.sender}", 
                    latest.subject, 
                    1002
                )
                appPrefs.edit().putString("last_aviso_subject", latest.subject).apply()
            }
        }
    }
}
