package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Aviso
import com.example.data.Subject
import com.example.data.SubjectRepository
import com.example.util.NotificationHelper
import com.example.util.SuapParser
import okhttp3.Request
import org.jsoup.Jsoup

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val repository: SubjectRepository by lazy {
        SubjectRepository(AppDatabase.getDatabase(appContext).subjectDao())
    }

    override suspend fun doWork(): Result {
        val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val notificationsEnabled = appPrefs.getBoolean("notifications_enabled", false)
        val notifyGrades = appPrefs.getBoolean("notify_grades", false)
        if (!notificationsEnabled && !notifyGrades) return Result.success()

        val credentialStore = com.example.util.CredentialStore.getInstance(applicationContext)
        val username = credentialStore.getSuapUsername()
        val password = credentialStore.getSuapPassword()

        if (username.isEmpty() || password.isEmpty()) return Result.failure()

        com.example.util.AppLog.d("SyncWorker", "Starting background sync at 19h")

        val suapAuthRepository = com.example.network.SuapAuthRepository.getInstance(applicationContext)

        try {
            var shouldRetry = false

            // 1. Fetch Grades (Boletim)
            val boletimUrl = "https://suap.ifal.edu.br/edu/aluno/$username/?tab=boletim"
            val boletimResult = suapAuthRepository.executeRequest(Request.Builder().url(boletimUrl))
            boletimResult.onSuccess { boletimHtml ->
                val parseResult = SuapParser.parseSubjects(Jsoup.parse(boletimHtml))
                parseResult.onSuccess { newSubjects ->
                    checkGradesChanges(newSubjects)
                }.onFailure { error ->
                    com.example.util.AppLog.e("SyncWorker", "Could not parse grades: ${error.message}")
                    shouldRetry = true
                }
            }.onError { message, error ->
                com.example.util.AppLog.e("SyncWorker", "Could not fetch grades: $message", error)
                shouldRetry = true
            }

            // 2. Fetch Avisos
            val avisosUrl = "https://suap.ifal.edu.br/admin/edu/mensagementrada/?tab=tab_todas"
            val avisosResult = suapAuthRepository.executeRequest(Request.Builder().url(avisosUrl))
            avisosResult.onSuccess { avisosHtml ->
                val parseResult = SuapParser.parseAvisos(Jsoup.parse(avisosHtml))
                parseResult.onSuccess { newAvisos ->
                    checkAvisosChanges(newAvisos)
                }.onFailure { error ->
                    com.example.util.AppLog.e("SyncWorker", "Could not parse notices: ${error.message}")
                    shouldRetry = true
                }
            }.onError { message, error ->
                com.example.util.AppLog.e("SyncWorker", "Could not fetch notices: $message", error)
                shouldRetry = true
            }

            return if (shouldRetry) Result.retry() else Result.success()

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            com.example.util.AppLog.e("SyncWorker", "Sync failed", e)
            return Result.retry()
        }
    }

    private suspend fun checkGradesChanges(newSubjects: List<Subject>) {
        val oldSubjects = repository.getAllSubjectsSync()
        if (oldSubjects.isEmpty()) {
            repository.syncSubjects(newSubjects)
            return
        }

        val oldSubjectsByName = oldSubjects.associateBy { it.name.trim().lowercase() }
        val changedSubjects = mutableListOf<String>()
        var gradesChanged = false
        var absencesChanged = false
        for (newSub in newSubjects) {
            val oldSub = oldSubjectsByName[newSub.name.trim().lowercase()]
            if (oldSub != null) {
                val subjectGradesChanged = oldSub.grade1 != newSub.grade1 || oldSub.grade2 != newSub.grade2 ||
                    oldSub.grade3 != newSub.grade3 || oldSub.grade4 != newSub.grade4 ||
                    oldSub.rec1 != newSub.rec1 || oldSub.rec2 != newSub.rec2 ||
                    oldSub.recFinal != newSub.recFinal ||
                    oldSub.finalGrade != newSub.finalGrade
                val subjectAbsencesChanged = oldSub.absences != newSub.absences
                if (subjectGradesChanged || subjectAbsencesChanged) {
                    changedSubjects.add(newSub.name)
                    gradesChanged = gradesChanged || subjectGradesChanged
                    absencesChanged = absencesChanged || subjectAbsencesChanged
                }
            }
        }

        if (changedSubjects.isNotEmpty()) {
            val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val notifyGrades = appPrefs.getBoolean("notify_grades", false)
            if (notifyGrades) {
                val message = when {
                    changedSubjects.size == 1 && gradesChanged && absencesChanged -> "Notas e faltas atualizadas em ${changedSubjects[0]}"
                    changedSubjects.size == 1 && gradesChanged -> "Nova nota ou recuperação em ${changedSubjects[0]}"
                    changedSubjects.size == 1 -> "Faltas atualizadas em ${changedSubjects[0]}"
                    gradesChanged && absencesChanged -> "Notas e faltas atualizadas em ${changedSubjects.size} disciplinas"
                    gradesChanged -> "Notas atualizadas em ${changedSubjects.size} disciplinas"
                    else -> "Faltas atualizadas em ${changedSubjects.size} disciplinas"
                }
                NotificationHelper.sendNotification(applicationContext, "Atualização acadêmica", message, 1001)
            }
        }
        repository.syncSubjects(newSubjects)
    }

    private fun checkAvisosChanges(newAvisos: List<Aviso>) {
        val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // Keep the same bounded offline cache used by the notices screen, including
        // updates discovered while the app is closed.
        try {
            val cached = org.json.JSONArray()
            newAvisos.take(20).forEach { aviso ->
                cached.put(org.json.JSONObject()
                    .put("sender", aviso.sender)
                    .put("subject", aviso.subject)
                    .put("date", aviso.date)
                    .put("isRead", aviso.isRead)
                    .put("detailUrl", aviso.detailUrl))
            }
            applicationContext.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE)
                .edit().putString("saved_avisos_json", cached.toString()).apply()
        } catch (e: Exception) {
            com.example.util.AppLog.w("SyncWorker", "Could not save notices offline", e)
        }
        val latest = newAvisos.firstOrNull()
        if (latest == null) {
            appPrefs.edit()
                .putString("last_aviso_key", "")
                .putString("last_aviso_subject", "")
                .apply()
            return
        }
        val latestKey = latest.detailUrl.ifBlank {
            "${latest.sender}|${latest.subject}|${latest.date}"
        }
        val previousKey = appPrefs.getString("last_aviso_key", null)
        val previousLegacySubject = appPrefs.getString("last_aviso_subject", "").orEmpty()
        val notificationsEnabled = appPrefs.getBoolean("notifications_enabled", false)

        // Save a baseline while this category is off so re-enabling it does not alert on an old notice.
        val isNewNotice = when {
            previousKey != null -> latestKey != previousKey
            previousLegacySubject.isNotBlank() -> latest.subject != previousLegacySubject
            else -> false
        }
        appPrefs.edit()
            .putString("last_aviso_key", latestKey)
            .putString("last_aviso_subject", latest.subject)
            .apply()

        if (notificationsEnabled && isNewNotice) {
            val notificationId = 10_000 + (latestKey.hashCode() and 0x7fffffff) % 900_000
            NotificationHelper.sendNotification(
                applicationContext,
                "Novo aviso: ${latest.sender}",
                latest.subject,
                notificationId
            )
        }
    }
}
