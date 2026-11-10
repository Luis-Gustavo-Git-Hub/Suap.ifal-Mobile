package com.example.baselineprofile

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/** Creates isolated, non-personal data so protected dashboard routes are measurable in CI. */
internal object BenchmarkTestData {
    const val TARGET_PACKAGE = "com.aistudio.suap.xkcdz"
    private const val MAIN_ACTIVITY = "$TARGET_PACKAGE/com.example.MainActivity"
    private const val READY_TIMEOUT_MS = 15_000L
    private const val ROOM_SCHEMA_VERSION = 9
    private const val ROOM_IDENTITY_HASH = "5177fbbabb116780cb91fe1358527de9"

    fun prepare(device: UiDevice) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val databaseFile = context.getDatabasePath("suap_database")
        if (appPrefs.getInt("benchmark_data_version", 0) >= 4 && databaseFile.exists()) {
            device.executeShellCommand("am force-stop $TARGET_PACKAGE")
            return
        }

        context.getSharedPreferences("suap_credentials", Context.MODE_PRIVATE).edit()
            .putString("username", "benchmark-user")
            .putString("password", "benchmark-only-password")
            .putString("saved_student_name", "Usuário de Teste")
            .putString("saved_avisos_json", benchmarkAvisosJson())
            .putString("offline_schedule_json", benchmarkScheduleJson())
            .putString("offline_diarios_json", "[]")
            .putBoolean("auto_login", false)
            .putBoolean("suap_tutorial_completed", true)
            .commit()
        appPrefs.edit()
            .putBoolean("terms_accepted", true)
            .commit()

        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        launchApp(device)

        // Room opens the database lazily, and the initial screen does not guarantee a query in
        // this instrumented setup. Create the exact exported Room schema before seeding data.
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        val db = context.openOrCreateDatabase("suap_database", Context.MODE_PRIVATE, null)
        try {
            ensureRoomSchema(db)
            db.execSQL(
                """
                INSERT OR REPLACE INTO subjects (
                    id, name, url, grade1, grade2, rec1, grade3, grade4,
                    classGrade1, classGrade2, classGrade3, classGrade4, rec2,
                    recFinal, finalGrade, absences, status, isFavorite, detailUrl
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "benchmark-subject-1",
                    "Programação de Sistemas Distribuídos",
                    "/edu/aluno_disciplina/12345/",
                    "8", "8", "", "8", "8",
                    "7", "7", "7", "7", "",
                    "", "8", "0", "Em andamento", 0,
                    "/edu/disciplina/12345/"
                )
            )
            val subjectUrl = "https://suap.ifal.edu.br/edu/aluno_disciplina/12345/"
            db.execSQL("DELETE FROM aulas_history WHERE subjectUrl = ?", arrayOf(subjectUrl))
            repeat(30) { index ->
                db.execSQL(
                    """
                    INSERT INTO aulas_history (
                        subjectUrl, date, etapa, numAulas, numFaltas, conteudo,
                        professor, attachmentUrl, attachmentTitle, rawHtml, linksJson
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        subjectUrl,
                        "${(index % 28) + 1}/01/2026",
                        "1",
                        "1",
                        "0",
                        "Conteúdo de teste de desempenho ${index + 1}",
                        "Professor de Teste",
                        "",
                        "",
                        "",
                        ""
                    )
                )
            }
        } finally {
            db.close()
        }
        launchApp(device)
        appPrefs.edit().putInt("benchmark_data_version", 4).commit()
        device.pressHome()
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
    }

    private fun ensureRoomSchema(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS subjects (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, url TEXT NOT NULL,
                    grade1 TEXT NOT NULL, grade2 TEXT NOT NULL, rec1 TEXT NOT NULL,
                    grade3 TEXT NOT NULL, grade4 TEXT NOT NULL, classGrade1 TEXT NOT NULL,
                    classGrade2 TEXT NOT NULL, classGrade3 TEXT NOT NULL, classGrade4 TEXT NOT NULL,
                    rec2 TEXT NOT NULL, recFinal TEXT NOT NULL, finalGrade TEXT NOT NULL,
                    absences TEXT NOT NULL, status TEXT NOT NULL, isFavorite INTEGER NOT NULL,
                    detailUrl TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS academic_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateInfo TEXT NOT NULL,
                    description TEXT NOT NULL, color TEXT NOT NULL, startDateStr TEXT,
                    endDateStr TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS aulas_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectUrl TEXT NOT NULL,
                    date TEXT NOT NULL, etapa TEXT NOT NULL, numAulas TEXT NOT NULL,
                    numFaltas TEXT NOT NULL, conteudo TEXT NOT NULL, professor TEXT NOT NULL,
                    attachmentUrl TEXT NOT NULL, attachmentTitle TEXT NOT NULL,
                    rawHtml TEXT NOT NULL, linksJson TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf(ROOM_IDENTITY_HASH)
            )
            db.version = ROOM_SCHEMA_VERSION
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun waitForDashboard(device: UiDevice) {
        // A first-access dialog can exist on older app data; close it before measuring navigation.
        device.wait(Until.findObject(By.text("Pular")), 500L)?.click()
        val homeTab = By.text("Início")
        for (attempt in 0 until 4) {
            if (device.findObject(homeTab) != null) break
            device.pressBack()
        }
        check(device.wait(Until.hasObject(homeTab), READY_TIMEOUT_MS)) { "Main navigation did not become ready" }
        val dashboardTitle = By.textContains("Olá,")
        if (device.findObject(dashboardTitle) == null) device.findObject(homeTab)?.click()
        check(device.wait(Until.hasObject(dashboardTitle), READY_TIMEOUT_MS)) { "Dashboard did not become ready" }
    }

    fun clickText(device: UiDevice, text: String) = click(device, By.text(text), text)

    fun clickTextContaining(device: UiDevice, text: String) = click(device, By.textContains(text), text)

    private fun click(device: UiDevice, selector: androidx.test.uiautomator.BySelector, label: String) {
        for (attempt in 0 until 8) {
            device.findObject(selector)?.let { target ->
                target.click()
                return
            }
            val scrollable = device.findObject(By.scrollable(true)) ?: break
            if (!scrollable.scroll(Direction.DOWN, 0.7f)) break
        }
        error("Could not find '$label' in the current screen")
    }

    private fun launchApp(device: UiDevice) {
        val launchResult = device.executeShellCommand("am start -W -n $MAIN_ACTIVITY")
        check(launchResult.contains("Status: ok")) {
            "SUAP app did not start for benchmark setup: $launchResult"
        }
        waitForDashboard(device)
    }

    private fun benchmarkAvisosJson(): String {
        val notices = org.json.JSONArray()
        repeat(30) { index ->
            notices.put(
                org.json.JSONObject()
                    .put("sender", "Professor de Teste")
                    .put("subject", "Aviso de teste de desempenho ${index + 1}")
                    .put("date", "10/11/2026")
                    .put("isRead", index % 2 == 0)
                    .put("detailUrl", "")
            )
        }
        return notices.toString()
    }

    private fun benchmarkScheduleJson(): String {
        val schedule = org.json.JSONArray()
        val weekdays = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")
        repeat(8) { index ->
            weekdays.forEach { weekday ->
                val startHour = 7 + index
                schedule.put(
                    org.json.JSONObject()
                        .put("dayOfWeek", weekday)
                        .put("timeSlot", "%02d:00 - %02d:50".format(startHour, startHour))
                        .put("shift", "Matutino")
                        .put("subjectCode", "BENCH${index + 1}")
                        .put("subjectName", "Disciplina de teste ${index + 1}")
                        .put("teacher", "Professor de Teste")
                )
            }
        }
        return schedule.toString()
    }
}
