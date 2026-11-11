package com.example

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.AulaHistoryEntity
import com.example.data.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Seeds synthetic, local-only data when the CI explicitly enables benchmark preparation. */
internal object BenchmarkDataSeeder {
    const val ACTION = "com.aistudio.suap.xkcdz.action.SEED_BENCHMARK"
    const val NAVIGATE_ACTION = "com.aistudio.suap.xkcdz.action.BENCHMARK_NAVIGATE"
    const val ROUTE_EXTRA = "benchmark_route"

    suspend fun seed(context: Context) = withContext(Dispatchers.IO) {
        val credentials = context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE)
        check(
            credentials.edit()
                .putString("username", "benchmark-user")
                .putString("password", "benchmark-only-password")
                .putString("saved_student_name", "Usuário de Teste")
                .putString("saved_avisos_json", benchmarkAvisosJson())
                .putString("offline_schedule_json", benchmarkScheduleJson())
                .putString("offline_diarios_json", "[]")
                .putBoolean("auto_login", false)
                .putBoolean("suap_tutorial_completed", true)
                .commit()
        ) { "Could not save synthetic benchmark preferences" }
        check(
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("terms_accepted", true)
                .commit()
        ) { "Could not save benchmark app preferences" }

        val database = AppDatabase.getDatabase(context)
        database.subjectDao().deleteAll()
        database.academicEventDao().deleteAll()
        database.aulaHistoryDao().deleteAll()
        database.subjectDao().upsertSubject(
            Subject(
                id = "benchmark-subject-1",
                name = "Programação de Sistemas Distribuídos",
                url = "/edu/aluno_disciplina/12345/",
                grade1 = "8",
                grade2 = "8",
                grade3 = "8",
                grade4 = "8",
                classGrade1 = "7",
                classGrade2 = "7",
                classGrade3 = "7",
                classGrade4 = "7",
                finalGrade = "8",
                status = "Em andamento",
                detailUrl = "/edu/disciplina/12345/"
            )
        )
        val subjectUrl = "https://suap.ifal.edu.br/edu/aluno_disciplina/12345/"
        database.aulaHistoryDao().insertAulas(
            (1..30).map { index ->
                AulaHistoryEntity(
                    subjectUrl = subjectUrl,
                    date = "${(index - 1) % 28 + 1}/01/2026",
                    etapa = "1",
                    numAulas = "1",
                    numFaltas = "0",
                    conteudo = "Conteúdo de teste de desempenho $index",
                    professor = "Professor de Teste"
                )
            }
        )
        check(database.subjectDao().getAllSubjectsSync().singleOrNull()?.id == "benchmark-subject-1") {
            "Synthetic benchmark subject was not persisted"
        }
    }

    private fun benchmarkAvisosJson(): String = JSONArray().apply {
        repeat(30) { index ->
            put(
                JSONObject()
                    .put("sender", "Professor de Teste")
                    .put("subject", "Aviso de teste de desempenho ${index + 1}")
                    .put("date", "10/11/2026")
                    .put("isRead", index % 2 == 0)
                    .put("detailUrl", "")
            )
        }
    }.toString()

    private fun benchmarkScheduleJson(): String = JSONArray().apply {
        val weekdays = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")
        repeat(8) { index ->
            weekdays.forEach { weekday ->
                val startHour = 7 + index
                put(
                    JSONObject()
                        .put("dayOfWeek", weekday)
                        .put("timeSlot", "%02d:00 - %02d:50".format(startHour, startHour))
                        .put("shift", "Matutino")
                        .put("subjectCode", "BENCH${index + 1}")
                        .put("subjectName", "Disciplina de teste ${index + 1}")
                        .put("teacher", "Professor de Teste")
                )
            }
        }
    }.toString()
}
