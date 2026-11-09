package com.example.ui

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull



import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import com.example.ui.theme.AppTheme
import androidx.lifecycle.viewModelScope
import com.example.data.Aviso
import com.example.data.EtapaDetail
import com.example.data.EvaluationItem
import com.example.data.GradeDetailRow
import com.example.data.ProfessorContact
import com.example.data.Subject
import com.example.data.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.Locale

private val SUBJECT_CODE_PATTERN = Regex("(?i)INT\\.\\d+")
private val SUBJECT_TAG_PATTERN = Regex("(?i)SATAGO\\w+")
private val SUBJECT_LEVEL_PATTERN = Regex("(?i)Médio|\\[.*?\\]")
private val SUBJECT_PUNCTUATION_PATTERN = Regex("[^a-zA-ZÀ-ÿ0-9\\s]")
private val SUBJECT_WHITESPACE_PATTERN = Regex("\\s+")

data class ChartBar(val subjectName: String, val studentGrade: Double, val classGrade: Double)

data class ScheduleItem(
    val dayOfWeek: String,
    val timeSlot: String,
    val shift: String,
    val subjectCode: String,
    val subjectName: String,
    val teacher: String
)


data class AcademicEvent(
    val dateInfo: String,
    val description: String,
    val color: String,
    val startDate: java.time.LocalDate? = null,
    val endDate: java.time.LocalDate? = null
)

data class DiarioItem(
    val code: String,
    val component: String,
    val teacher: String,
    val room: String,
    val horarioCode: String
)

class SuapViewModel(private val applicationContext: android.content.Context, private val repository: SubjectRepository, private val sharedPreferences: SharedPreferences) : ViewModel() {

    // Prevent overlapping DOM parses and database/network fan-out if the user triggers sync twice.
    private val htmlSyncMutex = Mutex()

    private fun createDataScope() = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job])
    )

    @Volatile
    private var dataScope = createDataScope()

    private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems
    private val _isLoadingSchedule = MutableStateFlow(false)
    val isLoadingSchedule = _isLoadingSchedule.asStateFlow()
    private val _scheduleError = MutableStateFlow<String?>(null)
    val scheduleError = _scheduleError.asStateFlow()

    private val _diarioItems = MutableStateFlow<List<DiarioItem>>(emptyList())
    val diarioItems: StateFlow<List<DiarioItem>> = _diarioItems

    private val _professorContacts = MutableStateFlow<List<ProfessorContact>>(emptyList())
    val professorContacts: StateFlow<List<ProfessorContact>> = _professorContacts

    private val _evaluations = MutableStateFlow<List<EvaluationItem>>(emptyList())
    val evaluations: StateFlow<List<EvaluationItem>> = _evaluations


    private val _studentName = MutableStateFlow(sharedPreferences.getString("saved_student_name", "Aluno") ?: "Aluno")
    val studentName: StateFlow<String> = _studentName

    private val _fullName = MutableStateFlow(sharedPreferences.getString("saved_full_name", "-") ?: "-")
    val fullName: StateFlow<String> = _fullName

    private val _matricula = MutableStateFlow(sharedPreferences.getString("saved_matricula", "-") ?: "-")
    val matricula: StateFlow<String> = _matricula

    private val _ingresso = MutableStateFlow(sharedPreferences.getString("saved_ingresso", "-") ?: "-")
    val ingresso: StateFlow<String> = _ingresso

    private val _curso = MutableStateFlow(sharedPreferences.getString("saved_curso", "-") ?: "-")
    val curso: StateFlow<String> = _curso

    private val _emailPessoal = MutableStateFlow(sharedPreferences.getString("saved_email", "-") ?: "-")
    val emailPessoal: StateFlow<String> = _emailPessoal

    private val _globalFrequency = MutableStateFlow(sharedPreferences.getString("saved_global_frequency", "-") ?: "-")
    val globalFrequency: StateFlow<String> = _globalFrequency

    private val _globalAbsences = MutableStateFlow(sharedPreferences.getString("saved_global_absences", "-") ?: "-")
    val globalAbsences: StateFlow<String> = _globalAbsences

    private val _totalClasses = MutableStateFlow(sharedPreferences.getString("saved_total_classes", "-") ?: "-")
    val totalClasses: StateFlow<String> = _totalClasses

    private val _passedClasses = MutableStateFlow(sharedPreferences.getString("saved_passed_classes", "-") ?: "-")
    val passedClasses: StateFlow<String> = _passedClasses
    
    private val _boletimPdfUrl = MutableStateFlow(sharedPreferences.getString("saved_boletim_url", "") ?: "")
    val boletimPdfUrl: StateFlow<String> = _boletimPdfUrl
    
    private val _profilePhotoUrl = MutableStateFlow(sharedPreferences.getString("saved_profile_photo_url", "") ?: "")
    val profilePhotoUrl: StateFlow<String> = _profilePhotoUrl

    private val _studentAverage = MutableStateFlow(sharedPreferences.getString("saved_student_average", "-") ?: "-")
    val studentAverage: StateFlow<String> = _studentAverage

    private val _classAverage = MutableStateFlow(sharedPreferences.getString("saved_class_average", "-") ?: "-")
    val classAverage: StateFlow<String> = _classAverage

    private val _lastSyncTime = MutableStateFlow(sharedPreferences.getString("last_sync_time", "") ?: "")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    fun updateLastSyncTime(time: String) {
        sharedPreferences.edit().putString("last_sync_time", time).apply()
        _lastSyncTime.value = time
    }

    private val _appTheme = MutableStateFlow<AppTheme>(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme

    private val _fontOverride = MutableStateFlow<com.example.ui.theme.AppFontFamily?>(null)
    val fontOverride: StateFlow<com.example.ui.theme.AppFontFamily?> = _fontOverride

    private val _customThemes = MutableStateFlow<List<com.example.ui.theme.CustomThemeData>>(emptyList())
    val customThemes: StateFlow<List<com.example.ui.theme.CustomThemeData>> = _customThemes

    init {
        val themesJson = sharedPreferences.getString("custom_themes", "[]") ?: "[]"
        _customThemes.value = deserializeCustomThemes(themesJson)
        
        val savedThemeId = sharedPreferences.getString("saved_app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        if (savedThemeId.startsWith("CUSTOM_")) {
            val customData = _customThemes.value.find { "CUSTOM_${it.id}" == savedThemeId }
            if (customData != null) {
                _appTheme.value = AppTheme.Custom(customData)
            } else {
                _appTheme.value = AppTheme.SYSTEM
            }
        } else {
            _appTheme.value = AppTheme.valueOf(savedThemeId)
        }

        val savedFontOverride = sharedPreferences.getString("saved_font_override", null)
        if (!savedFontOverride.isNullOrBlank()) {
            try {
                _fontOverride.value = com.example.ui.theme.AppFontFamily.valueOf(savedFontOverride)
            } catch (e: Exception) {
                _fontOverride.value = null
            }
        }
    }

    fun setFontOverride(font: com.example.ui.theme.AppFontFamily?) {
        _fontOverride.value = font
        if (font != null) {
            sharedPreferences.edit().putString("saved_font_override", font.name).apply()
        } else {
            sharedPreferences.edit().remove("saved_font_override").apply()
        }
    }

    fun saveCustomTheme(theme: com.example.ui.theme.CustomThemeData) {
        val current = _customThemes.value.toMutableList()
        val index = current.indexOfFirst { it.id == theme.id }
        if (index != -1) {
            current[index] = theme
        } else {
            current.add(theme)
        }
        _customThemes.value = current
        sharedPreferences.edit().putString("custom_themes", serializeCustomThemes(current)).apply()
    }

    fun removeCustomTheme(themeId: String, context: android.content.Context) {
        val current = _customThemes.value.toMutableList()
        current.removeAll { it.id == themeId }
        _customThemes.value = current
        sharedPreferences.edit().putString("custom_themes", serializeCustomThemes(current)).apply()
        
        if (_appTheme.value.id == "CUSTOM_$themeId") {
            setAppTheme(context, AppTheme.SYSTEM)
        }
    }

    private fun serializeCustomThemes(themes: List<com.example.ui.theme.CustomThemeData>): String {
        val array = org.json.JSONArray()
        themes.forEach {
            val obj = org.json.JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("primaryColor", it.primaryColor.name)
            obj.put("iconColor", it.iconColor.name)
            it.profileColor?.let { pc -> obj.put("profileColor", pc.name) }
            obj.put("isDark", it.isDark)
            obj.put("iconPack", it.iconPack.name)
            obj.put("navBarStyle", it.navBarStyle.name)
            obj.put("fontFamily", it.fontFamily.name)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCustomThemes(json: String): List<com.example.ui.theme.CustomThemeData> {
        val result = mutableListOf<com.example.ui.theme.CustomThemeData>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val profileColor = try {
                    if (obj.has("profileColor")) com.example.ui.theme.ThemeColor.valueOf(obj.getString("profileColor")) else null
                } catch (e: Exception) { null }

                val iconPack = try {
                    if (obj.has("iconPack")) com.example.ui.theme.IconPack.valueOf(obj.getString("iconPack")) else com.example.ui.theme.IconPack.DEFAULT
                } catch (e: Exception) { com.example.ui.theme.IconPack.DEFAULT }

                val navBarStyle = try {
                    if (obj.has("navBarStyle")) com.example.ui.theme.NavBarStyle.valueOf(obj.getString("navBarStyle")) else com.example.ui.theme.NavBarStyle.DEFAULT
                } catch (e: Exception) { com.example.ui.theme.NavBarStyle.DEFAULT }

                val fontFamily = try {
                    if (obj.has("fontFamily")) com.example.ui.theme.AppFontFamily.valueOf(obj.getString("fontFamily")) else com.example.ui.theme.AppFontFamily.DEFAULT
                } catch (e: Exception) { com.example.ui.theme.AppFontFamily.DEFAULT }

                result.add(
                    com.example.ui.theme.CustomThemeData(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primaryColor = com.example.ui.theme.ThemeColor.valueOf(obj.getString("primaryColor")),
                        iconColor = com.example.ui.theme.ThemeColor.valueOf(obj.getString("iconColor")),
                        profileColor = profileColor,
                        isDark = obj.getBoolean("isDark"),
                        iconPack = iconPack,
                        navBarStyle = navBarStyle,
                        fontFamily = fontFamily
                    )
                )
            }
        } catch(e: Exception) {}
        return result
    }

    fun setAppTheme(context: android.content.Context, theme: AppTheme) {
        _appTheme.value = theme
        sharedPreferences.edit().putString("saved_app_theme", theme.id).apply()
        if (theme == AppTheme.ROBIN_KAWA) {
            sharedPreferences.edit().putBoolean("robin_kawa_unlocked", true).apply()
        }
        
        // Update App Icon
        val manager = context.packageManager
        val packageName = context.packageName
        
        val targetAliasName = if (theme is AppTheme.Custom) {
            theme.data.iconColor.aliasName
        } else {
            val aliases = mapOf(
                AppTheme.SYSTEM to "Default",
                AppTheme.LIGHT to "Light",
                AppTheme.DARK to "Dark",
                AppTheme.DEEP_BLUE to "DeepBlue",
                AppTheme.NATURE to "Nature",
                AppTheme.AMOLED to "Amoled",
                AppTheme.DRACULA to "Dracula",
                AppTheme.SUNSET to "Sunset",
                AppTheme.CYBERPUNK to "Cyberpunk",
                AppTheme.ROBIN_KAWA to "Cyberpunk" // Use pink launcher icon
            )
            aliases[theme] ?: "Default"
        }
        
        val targetAlias = "com.example.MainActivity${targetAliasName}"
        
        val allAliases = listOf("Default", "Light", "Dark", "DeepBlue", "Nature", "Amoled", "Dracula", "Sunset", "Cyberpunk", "Red", "Teal", "Indigo", "Amber", "DeepPurple", "Cyan", "Brown", "BlueGrey", "LightBlue")
        
        allAliases.forEach { aliasSuffix ->
            val aliasName = "com.example.MainActivity$aliasSuffix"
            val state = if (aliasName == targetAlias) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            val component = android.content.ComponentName(packageName, aliasName)
            if (manager.getComponentEnabledSetting(component) != state) {
                manager.setComponentEnabledSetting(
                    component,
                    state,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    val isRobinKawaUnlocked: Boolean
        get() = sharedPreferences.getBoolean("robin_kawa_unlocked", false)

    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subjectUiModels: StateFlow<List<SubjectUiModel>> = repository.allSubjects
        .map { list -> list.map { buildSubjectUiModel(it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statisticsSnapshot: StateFlow<com.example.domain.StatisticsSnapshot> = StatisticsViewModel(
        subjects = subjects,
        globalFrequency = globalFrequency,
        globalAbsences = globalAbsences,
        totalClasses = totalClasses,
        passedClasses = passedClasses,
        scope = viewModelScope
    ).snapshot

    private val _chartBars = MutableStateFlow<List<ChartBar>>(emptyList())
    val chartBars: StateFlow<List<ChartBar>> = _chartBars.asStateFlow()


    fun parseHtmlAndSave(html: String, onComplete: (Boolean) -> Unit = {}) {
        // SUAP pages can be large; keep DOM parsing and row processing off the UI thread.
        dataScope.launch(Dispatchers.Default) {
            var saved = false
            htmlSyncMutex.withLock {
                try {
                    val document = Jsoup.parse(html)
                    val baseSubjects = when (val result = com.example.util.SuapParser.parseSubjects(document)) {
                        is com.example.util.ParserResult.Success -> result.data
                        is com.example.util.ParserResult.Error -> throw IllegalArgumentException(result.message)
                    }
                
                    val titleElement = document.selectFirst("div.title-container h2")
                    if (titleElement != null) {
                        var nameText = titleElement.text()
                        nameText = nameText.substringBefore(" (").trim()
                        val firstName = nameText.split(" ").firstOrNull() ?: "Aluno"
                        _studentName.value = firstName
                    }
                
                    val photoImg = document.select("img").firstOrNull { 
                        it.attr("src").contains("/media/fotos/") || 
                        it.attr("src").contains("/media/alunos/") ||
                        it.attr("src").contains("foto")
                    }
                    if (photoImg != null) {
                        val url = photoImg.attr("src")
                        if (url.isNotEmpty()) {
                            _profilePhotoUrl.value = if (url.startsWith("http")) url else "https://suap.ifal.edu.br" + url
                        }
                    }

                    val printBtn = document.select("a[href*=/emitir_boletim_pdf/]").first() ?: document.select("a").find { it.text().contains("Imprimir (Retrato)", ignoreCase = true) } ?: document.select("a").find { it.text().contains("Imprimir (Boletim)", ignoreCase = true) } ?: document.select("a[href*=/imprimir_boletim]").first() ?: document.select("a[href*=/imprimir/]").first()
                    if (printBtn != null) {
                        val url = printBtn.attr("href")
                        if (url.isNotEmpty()) {
                            _boletimPdfUrl.value = if (url.startsWith("http")) url else "https://suap.ifal.edu.br" + url
                        }
                    }
                
                    val dlItems = document.select("dl.definition-list div.list-item")
                    for (item in dlItems) {
                        val dt = item.selectFirst("dt")?.text()?.trim()
                        val dd = item.selectFirst("dd")?.text()?.trim()
                        if (dt != null && dd != null) {
                            when (dt) {
                                "Nome" -> _fullName.value = dd
                                "Matrícula" -> _matricula.value = dd
                                "Ingresso" -> _ingresso.value = dd
                                "Curso" -> _curso.value = dd.replaceFirst(Regex("^\\S+\\s*-\\s*"), "")
                                "E-mail Acadêmico" -> _emailPessoal.value = dd
                                "E-mail Pessoal" -> _emailPessoal.value = dd
                                "E-mail" -> _emailPessoal.value = dd
                            }
                        }
                    }

                    val tfootTds = document.select("tfoot tr td")
                    val chNode = document.selectFirst("tfoot td[headers=th_ch]")
                    val totalAulasNode = document.selectFirst("tfoot td[headers=th_total_aulas]") ?: document.selectFirst("tfoot td[headers=th_ch_cumprida]")
                
                    if (chNode != null) {
                        _totalClasses.value = chNode.text().replace(Regex("[^0-9]"), "")
                    } else if (tfootTds.size >= 5) {
                        // based on screenshot: Total:, 1440 aulas (C.H.), 633 (Total de Aulas), 118, 81%
                        _totalClasses.value = tfootTds[1].text().replace(Regex("[^0-9]"), "")
                    } else if (tfootTds.size >= 4) {
                        _totalClasses.value = tfootTds[0].text().replace(Regex("[^0-9]"), "")
                    }
                
                    if (totalAulasNode != null) {
                        _passedClasses.value = totalAulasNode.text().replace(Regex("[^0-9]"), "")
                    } else if (tfootTds.size >= 5) {
                        _passedClasses.value = tfootTds[2].text().replace(Regex("[^0-9]"), "")
                    } else if (tfootTds.size >= 4) {
                        _passedClasses.value = tfootTds[1].text().replace(Regex("[^0-9]"), "")
                    }

                    val footerFaltas = document.selectFirst("tfoot td[headers=th_total_faltas]")
                    if (footerFaltas != null) {
                        _globalAbsences.value = footerFaltas.text().replace(Regex("[^0-9]"), "")
                    }
                
                    val footerFreq = document.selectFirst("tfoot td[headers=th_frequencia]") 
                        ?: document.selectFirst("tfoot td:matchesOwn(\\d+([.,]\\d+)?\\s*%)")
                    if (footerFreq != null && footerFreq.text().isNotBlank()) {
                        val match = Regex("""\d+([.,]\d+)?""").find(footerFreq.text())
                        if (match != null) {
                            _globalFrequency.value = match.value
                        } else {
                            _globalFrequency.value = footerFreq.text().replace("%", "").trim()
                        }
                    } else {
                        val total = _passedClasses.value.toIntOrNull() ?: _totalClasses.value.toIntOrNull() ?: 0
                        val faltas = _globalAbsences.value.toIntOrNull() ?: 0
                        if (total > 0) {
                            val calculated = ((total - faltas).coerceAtLeast(0).toFloat() / total.toFloat()) * 100f
                            _globalFrequency.value = if (calculated % 1f == 0f) "${calculated.toInt()}" else String.format(java.util.Locale.US, "%.1f", calculated).replace(".", ",")
                        } else if (faltas == 0) {
                            _globalFrequency.value = "100"
                        }
                    }
                


                    val textBoletim = document.body().text()
                
                    val matchAluno = Regex("M[eé]dia do aluno:?\\s*([0-9]+[.,][0-9]+)", RegexOption.IGNORE_CASE).find(textBoletim)
                    if (matchAluno != null) {
                        _studentAverage.value = matchAluno.groupValues[1]
                    } else {
                        val matchIRA = Regex("Índice de Rendimento[^0-9]*([0-9]+[.,][0-9]+)", RegexOption.IGNORE_CASE).find(textBoletim)
                        if (matchIRA != null) {
                            _studentAverage.value = matchIRA.groupValues[1]
                        }
                    }
                
                    val matchTurma = Regex("M[eé]dia da turma:?\\s*([0-9]+[.,][0-9]+)", RegexOption.IGNORE_CASE).find(textBoletim)
                    if (matchTurma != null) {
                        _classAverage.value = matchTurma.groupValues[1]
                    }

                    val avgMap = extractClassAverages(document)
                    val selectedStage = document.selectFirst("a.tab.selected, li.active a")
                    val currentEtapa = selectedStage?.attr("href")?.let { href ->
                        Regex("etapa=([0-9]+)").find(href)?.groupValues?.get(1)?.toIntOrNull()
                    } ?: 1
                    val averageKeys = avgMap.keys.sortedByDescending { it.length }
                    val parsedSubjects = baseSubjects.map { subject ->
                        val key = averageKeys.firstOrNull { subject.name.contains(it, ignoreCase = true) }
                        val average = key?.let { avgMap[it] }?.takeIf { it > 0.0 }?.toString() ?: ""
                        when (currentEtapa) {
                            1 -> subject.copy(classGrade1 = average)
                            2 -> subject.copy(classGrade2 = average)
                            3 -> subject.copy(classGrade3 = average)
                            4 -> subject.copy(classGrade4 = average)
                            else -> subject
                        }
                    }

                    // Compare with previous subjects for notifications
                    val notifyGrades = applicationContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).getBoolean("notify_grades", false)
                    if (notifyGrades) {
                        val oldSubjects = repository.getAllSubjectsSync()
                        val oldSubjectsByName = oldSubjects.associateBy { it.name.trim().lowercase() }
                        val changedSubjects = mutableListOf<String>()
                        var gradesChanged = false
                        var absencesChanged = false
                        for (newSub in parsedSubjects) {
                            val oldSub = oldSubjectsByName[newSub.name.trim().lowercase()]
                            if (oldSub != null) {
                                val subjectGradesChanged = newSub.grade1 != oldSub.grade1 || newSub.grade2 != oldSub.grade2 ||
                                    newSub.grade3 != oldSub.grade3 || newSub.grade4 != oldSub.grade4 ||
                                    newSub.rec1 != oldSub.rec1 || newSub.rec2 != oldSub.rec2 ||
                                    newSub.recFinal != oldSub.recFinal || newSub.finalGrade != oldSub.finalGrade
                                val subjectAbsencesChanged = newSub.absences != oldSub.absences
                                if (subjectGradesChanged || subjectAbsencesChanged) {
                                    changedSubjects.add(newSub.name)
                                    gradesChanged = gradesChanged || subjectGradesChanged
                                    absencesChanged = absencesChanged || subjectAbsencesChanged
                                }
                            }
                        }
                        if (changedSubjects.isNotEmpty()) {
                            val message = when {
                                changedSubjects.size == 1 && gradesChanged && absencesChanged -> "Notas e faltas atualizadas em ${changedSubjects[0]}"
                                changedSubjects.size == 1 && gradesChanged -> "Nova nota ou recuperação em ${changedSubjects[0]}"
                                changedSubjects.size == 1 -> "Faltas atualizadas em ${changedSubjects[0]}"
                                gradesChanged && absencesChanged -> "Notas e faltas atualizadas em ${changedSubjects.size} disciplinas"
                                gradesChanged -> "Notas atualizadas em ${changedSubjects.size} disciplinas"
                                else -> "Faltas atualizadas em ${changedSubjects.size} disciplinas"
                            }
                            com.example.util.NotificationHelper.sendNotification(
                                applicationContext,
                                "Atualização acadêmica",
                                message,
                                1001
                            )
                        }
                    }

                    if (parsedSubjects.isNotEmpty()) {
                        repository.syncSubjects(parsedSubjects)
                        detailedSubjects.clear()
                        _subjectDetails.value = emptyMap()
                        syncDisciplinasUrls()
                    }

                    // Parse schedule tables from HTML
                    extractChartBars(document)

                    val (parsedSchedule, parsedDiarios) = parseScheduleFromHtml(document)
                    if (parsedSchedule.isNotEmpty() || parsedDiarios.isNotEmpty()) {
                        saveScheduleOffline(parsedSchedule, parsedDiarios)
                    }
                    parseAndSyncProfessorsFromHtml(document, parsedDiarios)
                
                    fetchAcademicCalendar()
                    fetchClassAveragesInBackground()

                    sharedPreferences.edit()
                        .putString("saved_student_name", _studentName.value)
                        .putString("saved_full_name", _fullName.value)
                        .putString("saved_matricula", _matricula.value)
                        .putString("saved_ingresso", _ingresso.value)
                        .putString("saved_curso", _curso.value)
                        .putString("saved_email", _emailPessoal.value)
                        .putString("saved_global_frequency", _globalFrequency.value)
                        .putString("saved_global_absences", _globalAbsences.value)
                        .putString("saved_total_classes", _totalClasses.value)
                        .putString("saved_passed_classes", _passedClasses.value)
                        .putString("saved_student_average", _studentAverage.value)
                        .putString("saved_class_average", _classAverage.value)
                        .putString("saved_boletim_url", _boletimPdfUrl.value)
                        .putString("saved_profile_photo_url", _profilePhotoUrl.value)
                        .apply()

                    val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                    val time = sdf.format(java.util.Date())
                    updateLastSyncTime(time)
                    saved = true
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) { onComplete(saved) }
        }
    }


    fun unescapeUnicode(str: String): String {
        var res = str
        val regex = Regex("\\\\u([0-9a-fA-F]{4})")
        regex.findAll(str).forEach { match ->
            val hex = match.groupValues[1]
            val char = hex.toInt(16).toChar()
            res = res.replace(match.value, char.toString())
        }
        return res
    }

    private fun extractChartBars(html: String) {
        extractChartBars(org.jsoup.Jsoup.parse(html))
    }

    private fun extractChartBars(doc: org.jsoup.nodes.Document) {
        try {
            val scripts = doc.select("script")
            var scriptContent = ""
            for (script in scripts) {
                if (script.html().contains("categories") && script.html().contains("da Turma")) {
                    scriptContent = script.html()
                    break
                }
            }
            if (scriptContent.isNotEmpty()) {
                val catMatch = Regex("""['"]?categories['"]?\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(scriptContent)
                val notaMatch = Regex("""['"]?name['"]?\s*:\s*['"]Nota do Aluno['"].*?['"]?data['"]?\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(scriptContent)
                val mediaMatch = Regex("""['"]?name['"]?\s*:\s*['"]M.*?dia da Turma['"].*?['"]?data['"]?\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(scriptContent)
                
                if (catMatch != null && notaMatch != null && mediaMatch != null) {
                    val categories = catMatch.groupValues[1].split("\",\"").map { unescapeUnicode(it.replace("\"", "").trim()) }
                    val notas = notaMatch.groupValues[1].split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                    val medias = mediaMatch.groupValues[1].split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                    
                    val bars = mutableListOf<ChartBar>()
                    for (i in categories.indices) {
                        val c = categories[i]
                        val n = notas.getOrNull(i) ?: 0.0
                        val m = medias.getOrNull(i) ?: 0.0
                        bars.add(ChartBar(c, n, m))
                    }
                    _chartBars.value = bars
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    private fun extractClassAverages(html: String): Map<String, Double> {
        return extractClassAverages(org.jsoup.Jsoup.parse(html))
    }

    private fun extractClassAverages(doc: org.jsoup.nodes.Document): Map<String, Double> {
        val classAvgMap = mutableMapOf<String, Double>()
        try {
            val scripts = doc.select("script")
            var scriptContent = ""
            for (script in scripts) {
                if (script.html().contains("categories") && script.html().contains("da Turma")) {
                    scriptContent = script.html()
                    break
                }
            }
            if (scriptContent.isNotEmpty()) {
                val categoriesMatch = Regex("""['"]?categories['"]?\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(scriptContent)
                val mediaTurmaMatch = Regex("""['"]?name['"]?\s*:\s*['"]M.*?dia da Turma['"].*?['"]?data['"]?\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(scriptContent)
                
                if (categoriesMatch != null && mediaTurmaMatch != null) {
                    val categoriesStr = categoriesMatch.groupValues[1]
                    val mediaTurmaStr = mediaTurmaMatch.groupValues[1]
                    
                    val categories = categoriesStr.split(",").map { it.replace("\"", "").trim() }
                    val averages = mediaTurmaStr.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                    
                    for (i in categories.indices) {
                        if (i < averages.size) {
                            val cat = unescapeUnicode(categories[i])
                            classAvgMap[cat] = averages[i]
                        }
                    }
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
        return classAvgMap
    }

    fun fetchScheduleFromSuap(matriculaVal: String = _matricula.value) {
        if (matriculaVal.isEmpty() || matriculaVal == "-") {
            _scheduleError.value = "Entre no SUAP para carregar seus horários."
            return
        }
        if (!_isLoadingSchedule.compareAndSet(false, true)) return
        _scheduleError.value = null
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = "https://suap.ifal.edu.br/edu/aluno/$matriculaVal/?tab=locais_aula_aluno"
                com.example.util.AppLog.d("SUAP_SCHEDULE", "Fetching schedule")
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                val result = repo.executeRequest(okhttp3.Request.Builder().url(url))
                result.onSuccess { html ->
                    val doc = org.jsoup.Jsoup.parse(html)
                    val (parsedSchedule, parsedDiarios) = parseScheduleFromHtml(doc)
                    if (parsedSchedule.isNotEmpty() || parsedDiarios.isNotEmpty()) {
                        saveScheduleOffline(parsedSchedule, parsedDiarios)
                    } else {
                        _scheduleError.value = "Não foi possível localizar os horários na resposta do SUAP."
                    }
                }.onError { message, _ -> _scheduleError.value = message }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                com.example.util.AppLog.e("SUAP_SCHEDULE", "Error fetching schedule", e)
                _scheduleError.value = "Não foi possível atualizar os horários. Verifique sua conexão."
            } finally {
                _isLoadingSchedule.value = false
            }
        }
    }

    fun fetchClassAveragesInBackground() {
        val matriculaVal = _matricula.value
        if (matriculaVal == "-" || matriculaVal.isEmpty()) return
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                // Two concurrent pages shorten total wait time without keeping all
                // four large SUAP responses in memory at once.
                val requestLimit = Semaphore(2)
                val etapaAvgMaps = coroutineScope {
                    (1..4).map { etapa ->
                        async {
                            requestLimit.withPermit {
                                try {
                                    val url = "https://suap.ifal.edu.br/edu/aluno/$matriculaVal/?tab=boletim&etapa=$etapa"
                                    com.example.util.AppLog.d("SUAP_AVG", "Fetching etapa $etapa")
                                    val result = repo.executeRequest(okhttp3.Request.Builder().url(url))
                                    val avgMap = when (result) {
                                        is com.example.network.NetworkResult.Success -> extractClassAverages(result.data)
                                        is com.example.network.NetworkResult.Error -> emptyMap()
                                    }
                                    etapa to avgMap
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    com.example.util.AppLog.e("SUAP_AVG", "Error fetching etapa $etapa", e)
                                    etapa to emptyMap()
                                }
                            }
                        }
                    }
                }.awaitAll().toMap()
                
                val currentSubjects = repository.getAllSubjectsSync()
                val averageKeysByStage = etapaAvgMaps.mapValues { (_, averages) ->
                    averages.keys.sortedByDescending { it.length }
                }
                val updatedList = currentSubjects.map { subject ->
                    var subjToUpdate = subject
                    for (etapa in 1..4) {
                        val avgMap = etapaAvgMaps[etapa] ?: continue
                        val matchedKey = averageKeysByStage[etapa]
                            ?.firstOrNull { category -> subject.name.contains(category, ignoreCase = true) }
                        if (matchedKey != null) {
                            val avg = avgMap[matchedKey]
                            if (avg != null && avg > 0.0) {
                                subjToUpdate = when (etapa) {
                                    1 -> subjToUpdate.copy(classGrade1 = avg.toString())
                                    2 -> subjToUpdate.copy(classGrade2 = avg.toString())
                                    3 -> subjToUpdate.copy(classGrade3 = avg.toString())
                                    4 -> subjToUpdate.copy(classGrade4 = avg.toString())
                                    else -> subjToUpdate
                                }
                            }
                        }
                    }
                    subjToUpdate
                }
                val changedSubjects = updatedList.filterIndexed { index, subject ->
                    subject != currentSubjects[index]
                }
                if (changedSubjects.isNotEmpty()) repository.updateClassAverages(changedSubjects)
                com.example.util.AppLog.d("SUAP_AVG", "Updated DB subjects with class averages")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                com.example.util.AppLog.e("SUAP_AVG", "Error updating class averages", e)
            }
        }
    }

    fun downloadBoletim(context: android.content.Context) {
        val imprimirUrl = _boletimPdfUrl.value
        if (imprimirUrl.isEmpty()) {
            com.example.util.AppSnackbarManager.show("URL do boletim não encontrada. Tente sincronizar novamente.")
            return
        }
        
        com.example.util.AppSnackbarManager.show("Iniciando download do boletim...")
        
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = com.example.network.SuapClient.okHttpClient
                var request = okhttp3.Request.Builder()
                    .url(imprimirUrl)
                    .build()
                
                var response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        com.example.util.AppSnackbarManager.show("Erro ao acessar boletim", actionLabel = "Tentar de novo", onAction = { downloadBoletim(context) })
                    }
                    return@launch
                }
                
                var contentType = response.header("Content-Type") ?: ""
                var pdfUrl = imprimirUrl
                
                if (contentType.contains("text/html")) {
                    val html = response.body?.string() ?: ""
                    val doc = org.jsoup.Jsoup.parse(html)
                    
                    val pdfLink = doc.select("a[href]").find { 
                        it.text().contains("PDF", ignoreCase = true) || 
                        it.text().contains("Download", ignoreCase = true) ||
                        it.text().contains("Baixar", ignoreCase = true) ||
                        it.text().contains("Imprimir (Boletim)", ignoreCase = true) ||
                        it.attr("href").contains("pdf", ignoreCase = true)
                    }
                    
                    if (pdfLink != null) {
                        var finalUrl = pdfLink.attr("href")
                        if (finalUrl.startsWith("/")) {
                            finalUrl = "https://suap.ifal.edu.br" + finalUrl
                        } else if (!finalUrl.startsWith("http")) {
                            finalUrl = "$imprimirUrl$finalUrl"
                        }
                        pdfUrl = finalUrl
                    } else {
                        // Let's find ANY link that might be it
                        val fallbackLink = doc.select("a[href]").find { 
                            it.attr("href").contains("gerar", ignoreCase = true) ||
                            it.attr("href").contains("pdf", ignoreCase = true) ||
                            it.attr("href").contains("download", ignoreCase = true)
                        }
                        if (fallbackLink != null) {
                            var finalUrl = fallbackLink.attr("href")
                            if (finalUrl.startsWith("/")) {
                                finalUrl = "https://suap.ifal.edu.br" + finalUrl
                            } else if (!finalUrl.startsWith("http")) {
                                finalUrl = "$imprimirUrl$finalUrl"
                            }
                            pdfUrl = finalUrl
                        } else {
                            val allLinks = doc.select("a[href]").joinToString(", ") { it.text() + "=" + it.attr("href") }
                            val allButtons = doc.select("button").joinToString(", ") { it.text() }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.app.AlertDialog.Builder(context)
                                    .setTitle("Debug: Links na página")
                                    .setMessage("Links: $allLinks\n\nButtons: $allButtons")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            return@launch
                        }
                    }
                    
                    // Fetch the actual PDF URL
                    request = okhttp3.Request.Builder()
                        .url(pdfUrl)
                        .build()
                    response = client.newCall(request).execute()
                    contentType = response.header("Content-Type") ?: ""
                }
                
                if (response.isSuccessful && contentType.contains("application/pdf")) {
                    // Save to Downloads
                    val inputStream = response.body?.byteStream()
                    if (inputStream != null) {
                        val fileName = "Boletim_SUAP_${System.currentTimeMillis()}.pdf"
                        
                        val resolver = context.contentResolver
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                        
                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                com.example.util.AppSnackbarManager.show("Boletim salvo em Downloads!")
                            }
                        }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        com.example.util.AppSnackbarManager.show("Erro: Não foi possível obter o PDF do boletim")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                com.example.util.AppLog.printStackTrace(e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.example.util.AppSnackbarManager.show("Erro ao baixar boletim", actionLabel = "Tentar de novo", onAction = { downloadBoletim(context) })
                }
            }
        }
    }

    fun loadDemoData() {
        dataScope.launch {
            _studentName.value = "Visitante"
            _fullName.value = "-"
            _matricula.value = "-"
            _ingresso.value = "-"
            _curso.value = "-"
            _globalFrequency.value = "-"
            _globalAbsences.value = "-"
            _totalClasses.value = "-"
            _passedClasses.value = "-"
            _studentAverage.value = "-"
            _classAverage.value = "-"
            repository.clearAll()

            sharedPreferences.edit()
                .putString("saved_student_name", _studentName.value)
                .putString("saved_full_name", _fullName.value)
                .putString("saved_matricula", _matricula.value)
                .putString("saved_ingresso", _ingresso.value)
                .putString("saved_curso", _curso.value)
                .putString("saved_global_frequency", _globalFrequency.value)
                .putString("saved_global_absences", _globalAbsences.value)
                .putString("saved_total_classes", _totalClasses.value)
                .putString("saved_passed_classes", _passedClasses.value)
                .putString("saved_student_average", _studentAverage.value)
                .putString("saved_class_average", _classAverage.value)
                .apply()

            val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
            val time = sdf.format(java.util.Date())
            updateLastSyncTime(time)
        }
    }
    
    fun toggleFavorite(subject: Subject) {
        dataScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(subject.id)
        }
    }

    fun enableDemoMode() {
        dataScope.launch(Dispatchers.IO) {
            val currentSubjects = repository.getAllSubjectsSync()
            val targetSubjects = if (currentSubjects.isEmpty()) {
                listOf(
                    Subject(name = "Matemática", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "2"),
                    Subject(name = "Língua Portuguesa", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "0"),
                    Subject(name = "Física", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "4"),
                    Subject(name = "Química", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "0"),
                    Subject(name = "Biologia", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "2"),
                    Subject(name = "História", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "0"),
                    Subject(name = "Geografia", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "0"),
                    Subject(name = "Programação de Sistemas", grade1 = "14", grade2 = "14", grade3 = "14", grade4 = "14", finalGrade = "14", absences = "0")
                )
            } else {
                currentSubjects.map {
                    it.copy(
                        grade1 = "14",
                        grade2 = "14",
                        grade3 = "14",
                        grade4 = "14",
                        finalGrade = "14"
                    )
                }
            }
            repository.clearAll()
            repository.insertAll(targetSubjects)

            val newDetails = mutableMapOf<String, List<EtapaDetail>>()
            targetSubjects.forEach { s ->
                val etapas = listOf(
                    EtapaDetail("1º Bimestre", listOf(GradeDetailRow("AV1", "Avaliação 1", "Prova Bimestral", "1.0", "14"), GradeDetailRow("AV2", "Avaliação 2", "Trabalho", "1.0", "14"))),
                    EtapaDetail("2º Bimestre", listOf(GradeDetailRow("AV1", "Avaliação 1", "Prova Bimestral", "1.0", "14"), GradeDetailRow("AV2", "Avaliação 2", "Trabalho", "1.0", "14"))),
                    EtapaDetail("3º Bimestre", listOf(GradeDetailRow("AV1", "Avaliação 1", "Prova Bimestral", "1.0", "14"), GradeDetailRow("AV2", "Avaliação 2", "Trabalho", "1.0", "14"))),
                    EtapaDetail("4º Bimestre", listOf(GradeDetailRow("AV1", "Avaliação 1", "Prova Bimestral", "1.0", "14"), GradeDetailRow("AV2", "Avaliação 2", "Trabalho", "1.0", "14")))
                )
                newDetails[s.name] = etapas
            }
            _subjectDetails.value = newDetails
            _studentAverage.value = "14.0"
            _classAverage.value = "14.0"
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        // Wait for cancelled writers before deleting so old requests cannot repopulate the cache.
        val previousScope = dataScope
        previousScope.cancel()
        try {
            previousScope.coroutineContext[Job]?.join()
            androidx.work.WorkManager.getInstance(applicationContext).cancelAllWork()
                .result.get(10, java.util.concurrent.TimeUnit.SECONDS)
            com.example.util.CredentialStore.getInstance(applicationContext).apply {
                clearSuapCredentials()
                clearSigaaCredentials()
            }
            sharedPreferences.edit().clear().apply()
            applicationContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            applicationContext.getSharedPreferences("suap_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            com.example.util.JustificativaCache.clear(applicationContext)

            val db = com.example.data.AppDatabase.getDatabase(applicationContext)
            db.withTransaction {
                repository.clearAll()
                db.academicEventDao().deleteAll()
                db.aulaHistoryDao().deleteAll()
            }
            _studentName.value = "Visitante"
            _fullName.value = "-"
            _matricula.value = "-"
            _ingresso.value = "-"
            _curso.value = "-"
            _globalAbsences.value = "-"
            _globalFrequency.value = "-"
            _totalClasses.value = "-"
            _passedClasses.value = "-"
            _studentAverage.value = "-"
            _classAverage.value = "-"
            _emailPessoal.value = ""
            _boletimPdfUrl.value = ""
            _profilePhotoUrl.value = ""
            _lastSyncTime.value = ""
            _scheduleItems.value = emptyList()
            _diarioItems.value = emptyList()
            _professorContacts.value = emptyList()
            _evaluations.value = emptyList()
            _academicEvents.value = emptyList()
            _avisos.value = emptyList()
            _subjectDetails.value = emptyMap()
            detailedSubjects.clear()
            _chartBars.value = emptyList()
            clearMaterials()
            _scheduleError.value = null
            _calendarError.value = null
            _avisosError.value = null
            _isLoadingSchedule.value = false
            _isLoadingCalendar.value = false
            _isLoadingAvisos.value = false
            _appTheme.value = AppTheme.SYSTEM
            _fontOverride.value = null
            _customThemes.value = emptyList()

            com.example.receiver.ClassReminderReceiver.scheduleNextClassAlarm(applicationContext)
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
            applicationContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().remove("suap_login_method").remove("saved_matricula").apply()
            applicationContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        } finally {
            dataScope = createDataScope()
        }
    }


    private fun saveAvisosToPrefs(avisosList: List<Aviso>) {
        try {
            val array = org.json.JSONArray()
            for (a in avisosList) {
                val obj = org.json.JSONObject()
                obj.put("sender", a.sender)
                obj.put("subject", a.subject)
                obj.put("date", a.date)
                obj.put("isRead", a.isRead)
                obj.put("detailUrl", a.detailUrl)
                array.put(obj)
            }
            sharedPreferences.edit().putString("saved_avisos_json", array.toString()).apply()
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    private fun loadSavedAvisos(): List<Aviso> {
        val list = mutableListOf<Aviso>()
        try {
            val jsonStr = sharedPreferences.getString("saved_avisos_json", "") ?: ""
            if (jsonStr.isNotEmpty()) {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Aviso(
                            sender = obj.optString("sender", ""),
                            subject = obj.optString("subject", ""),
                            date = obj.optString("date", ""),
                            isRead = obj.optBoolean("isRead", false),
                            detailUrl = obj.optString("detailUrl", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
        return list
    }

    private fun saveMessageDetailToPrefs(urlPath: String, detail: com.example.data.MessageDetail) {
        try {
            val key = "aviso_detail_${urlPath.hashCode()}"
            val obj = org.json.JSONObject()
            obj.put("content", detail.content)
            val attachArray = org.json.JSONArray()
            for (att in detail.attachments) {
                val aObj = org.json.JSONObject()
                aObj.put("name", att.name)
                aObj.put("url", att.url)
                attachArray.put(aObj)
            }
            obj.put("attachments", attachArray)
            sharedPreferences.edit().putString(key, obj.toString()).apply()
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    private fun loadSavedMessageDetail(urlPath: String): com.example.data.MessageDetail? {
        try {
            val key = "aviso_detail_${urlPath.hashCode()}"
            val jsonStr = sharedPreferences.getString(key, "") ?: ""
            if (jsonStr.isNotEmpty()) {
                val obj = org.json.JSONObject(jsonStr)
                val content = obj.optString("content", "")
                val attachArray = obj.optJSONArray("attachments")
                val attachments = mutableListOf<com.example.data.Attachment>()
                if (attachArray != null) {
                    for (i in 0 until attachArray.length()) {
                        val aObj = attachArray.getJSONObject(i)
                        attachments.add(
                            com.example.data.Attachment(
                                name = aObj.optString("name", ""),
                                url = aObj.optString("url", "")
                            )
                        )
                    }
                }
                return com.example.data.MessageDetail(content, attachments)
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
        return null
    }

    private val _avisos = MutableStateFlow<List<Aviso>>(loadSavedAvisos())
    val avisos: StateFlow<List<Aviso>> = _avisos

    private val _isLoadingAvisos = MutableStateFlow(false)
    val isLoadingAvisos: StateFlow<Boolean> = _isLoadingAvisos
    private val _avisosError = MutableStateFlow<String?>(null)
    val avisosError = _avisosError.asStateFlow()

    
    private val _academicEvents = kotlinx.coroutines.flow.MutableStateFlow<List<AcademicEvent>>(emptyList())
    val academicEvents: kotlinx.coroutines.flow.StateFlow<List<AcademicEvent>> = _academicEvents.asStateFlow()
    private val _isLoadingCalendar = MutableStateFlow(false)
    val isLoadingCalendar = _isLoadingCalendar.asStateFlow()
    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError = _calendarError.asStateFlow()

    private fun loadAcademicCalendarOffline() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cached = loadAcademicCalendarFromPrefs()
                _academicEvents.value = cached
                val db = com.example.data.AppDatabase.getDatabase(applicationContext)
                db.academicEventDao().getAllEvents().collect { entities ->
                    val events = entities.mapNotNull {
                        try {
                            AcademicEvent(
                                dateInfo = it.dateInfo,
                                description = it.description,
                                color = it.color,
                                startDate = it.startDateStr?.takeIf(String::isNotBlank)?.let(java.time.LocalDate::parse),
                                endDate = it.endDateStr?.takeIf(String::isNotBlank)?.let(java.time.LocalDate::parse)
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    // Observing a cache must never write fallback data back to that cache.
                    _academicEvents.value = discardGeneratedCalendar(events).ifEmpty { loadAcademicCalendarFromPrefs() }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
            }
        }
    }

    private fun discardGeneratedCalendar(events: List<AcademicEvent>): List<AcademicEvent> {
        val year = events.firstOrNull()?.startDate?.year ?: return events
        val generated = getDemoAcademicEvents(year)
        return if (events.size == generated.size && events.toSet() == generated.toSet()) emptyList() else events
    }

    private fun saveAcademicCalendarToRoom(events: List<AcademicEvent>) {
        try {
            val db = com.example.data.AppDatabase.getDatabase(applicationContext)
            dataScope.launch(Dispatchers.IO) {
                try {
                    val entities = events.map {
                        com.example.data.AcademicEventEntity(
                            dateInfo = it.dateInfo,
                            description = it.description,
                            color = it.color,
                            startDateStr = it.startDate?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                            endDateStr = it.endDate?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        )
                    }
                    db.academicEventDao().replaceAll(entities)
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    private fun saveAcademicCalendarToPrefs(events: List<AcademicEvent>) {
        try {
            val array = org.json.JSONArray()
            for (ev in events) {
                val obj = org.json.JSONObject()
                obj.put("dateInfo", ev.dateInfo)
                obj.put("description", ev.description)
                obj.put("color", ev.color)
                obj.put("startDate", ev.startDate?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
                obj.put("endDate", ev.endDate?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
                array.put(obj)
            }
            sharedPreferences.edit().putString("offline_academic_calendar_json", array.toString()).apply()
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    private fun loadAcademicCalendarFromPrefs(): List<AcademicEvent> {
        val json = sharedPreferences.getString("offline_academic_calendar_json", "") ?: ""
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<AcademicEvent>()
        try {
            val array = org.json.JSONArray(json)
            val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sDateStr = obj.optString("startDate")
                val eDateStr = obj.optString("endDate")
                list.add(
                    AcademicEvent(
                        dateInfo = obj.optString("dateInfo"),
                        description = obj.optString("description"),
                        color = obj.optString("color", "#B3E5FC"),
                        startDate = if (sDateStr.isNotBlank()) java.time.LocalDate.parse(sDateStr, formatter) else null,
                        endDate = if (eDateStr.isNotBlank()) java.time.LocalDate.parse(eDateStr, formatter) else null
                    )
                )
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
        return discardGeneratedCalendar(list)
    }

    private fun parseCalendarDateStr(str: String, currentYear: Int): java.time.LocalDate? {
        if (str.isBlank()) return null
        return try {
            val parts = str.trim().split("/")
            when (parts.size) {
                3 -> {
                    val d = parts[0].trim().toInt()
                    val m = parts[1].trim().toInt()
                    val y = parts[2].trim().toInt().let { if (it < 100) it + 2000 else it }
                    java.time.LocalDate.of(y, m, d)
                }
                2 -> {
                    val d = parts[0].trim().toInt()
                    val m = parts[1].trim().toInt()
                    java.time.LocalDate.of(currentYear, m, d)
                }
                else -> {
                    val isoMatch = Regex("""\d{4}-\d{2}-\d{2}""").find(str)?.value
                    if (isoMatch != null) java.time.LocalDate.parse(isoMatch) else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getDemoAcademicEvents(currentYear: Int = java.time.LocalDate.now().year): List<AcademicEvent> {
        return listOf(
            AcademicEvent(
                dateInfo = "05/02/$currentYear",
                description = "Início do Semestre Letivo",
                color = "#C8E6C9",
                startDate = java.time.LocalDate.of(currentYear, 2, 5),
                endDate = java.time.LocalDate.of(currentYear, 2, 5)
            ),
            AcademicEvent(
                dateInfo = "12/02/$currentYear a 14/02/$currentYear",
                description = "Recesso de Carnaval",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 2, 12),
                endDate = java.time.LocalDate.of(currentYear, 2, 14)
            ),
            AcademicEvent(
                dateInfo = "21/03/$currentYear",
                description = "Sábado Letivo (Horário de Terça)",
                color = "#FFF9C4",
                startDate = java.time.LocalDate.of(currentYear, 3, 21),
                endDate = java.time.LocalDate.of(currentYear, 3, 21)
            ),
            AcademicEvent(
                dateInfo = "29/03/$currentYear",
                description = "Sexta-feira Santa (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 3, 29),
                endDate = java.time.LocalDate.of(currentYear, 3, 29)
            ),
            AcademicEvent(
                dateInfo = "21/04/$currentYear",
                description = "Tiradentes (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 4, 21),
                endDate = java.time.LocalDate.of(currentYear, 4, 21)
            ),
            AcademicEvent(
                dateInfo = "01/05/$currentYear",
                description = "Dia do Trabalhador (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 5, 1),
                endDate = java.time.LocalDate.of(currentYear, 5, 1)
            ),
            AcademicEvent(
                dateInfo = "18/05/$currentYear",
                description = "Sábado Letivo (Horário de Quarta)",
                color = "#FFF9C4",
                startDate = java.time.LocalDate.of(currentYear, 5, 18),
                endDate = java.time.LocalDate.of(currentYear, 5, 18)
            ),
            AcademicEvent(
                dateInfo = "30/05/$currentYear",
                description = "Corpus Christi (Ponto Facultativo)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 5, 30),
                endDate = java.time.LocalDate.of(currentYear, 5, 30)
            ),
            AcademicEvent(
                dateInfo = "24/06/$currentYear",
                description = "São João (Feriado Estadual)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 6, 24),
                endDate = java.time.LocalDate.of(currentYear, 6, 24)
            ),
            AcademicEvent(
                dateInfo = "05/07/$currentYear a 15/07/$currentYear",
                description = "Período de Exames Finais (1º Semestre)",
                color = "#D1C4E9",
                startDate = java.time.LocalDate.of(currentYear, 7, 5),
                endDate = java.time.LocalDate.of(currentYear, 7, 15)
            ),
            AcademicEvent(
                dateInfo = "16/07/$currentYear a 31/07/$currentYear",
                description = "Férias Docentes / Recesso Escolar",
                color = "#B3E5FC",
                startDate = java.time.LocalDate.of(currentYear, 7, 16),
                endDate = java.time.LocalDate.of(currentYear, 7, 31)
            ),
            AcademicEvent(
                dateInfo = "01/08/$currentYear",
                description = "Início das Aulas do 2º Semestre Letivo",
                color = "#C8E6C9",
                startDate = java.time.LocalDate.of(currentYear, 8, 1),
                endDate = java.time.LocalDate.of(currentYear, 8, 1)
            ),
            AcademicEvent(
                dateInfo = "07/09/$currentYear",
                description = "Independência do Brasil (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 9, 7),
                endDate = java.time.LocalDate.of(currentYear, 9, 7)
            ),
            AcademicEvent(
                dateInfo = "16/09/$currentYear",
                description = "Emancipação Política de Alagoas",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 9, 16),
                endDate = java.time.LocalDate.of(currentYear, 9, 16)
            ),
            AcademicEvent(
                dateInfo = "12/10/$currentYear",
                description = "Nossa Senhora Aparecida (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 10, 12),
                endDate = java.time.LocalDate.of(currentYear, 10, 12)
            ),
            AcademicEvent(
                dateInfo = "15/10/$currentYear",
                description = "Dia do Professor (Recesso Escolar)",
                color = "#FFCC80",
                startDate = java.time.LocalDate.of(currentYear, 10, 15),
                endDate = java.time.LocalDate.of(currentYear, 10, 15)
            ),
            AcademicEvent(
                dateInfo = "20/10/$currentYear a 24/10/$currentYear",
                description = "Semana de Avaliações Parciais (Provas)",
                color = "#FF4081",
                startDate = java.time.LocalDate.of(currentYear, 10, 20),
                endDate = java.time.LocalDate.of(currentYear, 10, 24)
            ),
            AcademicEvent(
                dateInfo = "28/10/$currentYear",
                description = "Dia do Servidor Público",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 10, 28),
                endDate = java.time.LocalDate.of(currentYear, 10, 28)
            ),
            AcademicEvent(
                dateInfo = "02/11/$currentYear",
                description = "Finados (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 11, 2),
                endDate = java.time.LocalDate.of(currentYear, 11, 2)
            ),
            AcademicEvent(
                dateInfo = "05/11/$currentYear",
                description = "Avaliações Bimestrais - Prova 1",
                color = "#FF4081",
                startDate = java.time.LocalDate.of(currentYear, 11, 5),
                endDate = java.time.LocalDate.of(currentYear, 11, 5)
            ),
            AcademicEvent(
                dateInfo = "12/11/$currentYear",
                description = "Avaliações Bimestrais - Prova 2",
                color = "#FF4081",
                startDate = java.time.LocalDate.of(currentYear, 11, 12),
                endDate = java.time.LocalDate.of(currentYear, 11, 12)
            ),
            AcademicEvent(
                dateInfo = "15/11/$currentYear",
                description = "Proclamação da República (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 11, 15),
                endDate = java.time.LocalDate.of(currentYear, 11, 15)
            ),
            AcademicEvent(
                dateInfo = "20/11/$currentYear",
                description = "Dia Nacional de Zumbi e da Consciência Negra",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 11, 20),
                endDate = java.time.LocalDate.of(currentYear, 11, 20)
            ),
            AcademicEvent(
                dateInfo = "15/12/$currentYear a 22/12/$currentYear",
                description = "Exames Finais do 2º Semestre",
                color = "#D1C4E9",
                startDate = java.time.LocalDate.of(currentYear, 12, 15),
                endDate = java.time.LocalDate.of(currentYear, 12, 22)
            ),
            AcademicEvent(
                dateInfo = "25/12/$currentYear",
                description = "Natal (Feriado Nacional)",
                color = "#FFCDD2",
                startDate = java.time.LocalDate.of(currentYear, 12, 25),
                endDate = java.time.LocalDate.of(currentYear, 12, 25)
            )
        )
    }

    fun fetchAcademicCalendar() {
        if (!_isLoadingCalendar.compareAndSet(false, true)) return
        _calendarError.value = null
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                val allEvents = mutableListOf<AcademicEvent>()

                // 1. Discover calendar link or use standard SUAP endpoints
                var calUrl = "https://suap.ifal.edu.br/edu/calendarioacademico/24/"
                val mainResult = repo.executeRequest(okhttp3.Request.Builder().url("https://suap.ifal.edu.br/"))
                mainResult.onSuccess { mainHtml ->
                    val docMain = org.jsoup.Jsoup.parse(mainHtml)
                    val linkEl = docMain.select("a:contains(Calendário Completo)").first()
                        ?: docMain.select("a:contains(Calendário Acadêmico)").first()
                        ?: docMain.select("a[href*=/edu/calendarioacademico/]").first()
                        ?: docMain.select("a[href*=/edu/calendario]").first()
                    if (linkEl != null) {
                        val href = linkEl.attr("href")
                        calUrl = if (href.startsWith("http")) href else "https://suap.ifal.edu.br$href"
                    }
                }

                // 2. Fetch and parse Academic Calendar page
                val calResult = repo.executeRequest(okhttp3.Request.Builder().url(calUrl))
                calResult.onSuccess { calHtml ->
                    val doc = org.jsoup.Jsoup.parse(calHtml)
                    val parsed = com.example.util.SuapParser.parseAcademicCalendar(doc)
                    if (parsed.isNotEmpty()) {
                        allEvents.addAll(parsed)
                    }
                }

                // If first URL returned empty, try direct /edu/calendarioacademico/
                if (allEvents.isEmpty() && !calUrl.endsWith("/edu/calendarioacademico/")) {
                    val fallbackResult = repo.executeRequest(okhttp3.Request.Builder().url("https://suap.ifal.edu.br/edu/calendarioacademico/"))
                    fallbackResult.onSuccess { fbHtml ->
                        val doc = org.jsoup.Jsoup.parse(fbHtml)
                        val parsed = com.example.util.SuapParser.parseAcademicCalendar(doc)
                        if (parsed.isNotEmpty()) {
                            allEvents.addAll(parsed)
                        }
                    }
                }

                // 3. Integrate evaluations from /edu/calendario_avaliacao/
                try {
                    val evalResult = repo.executeRequest(okhttp3.Request.Builder().url("https://suap.ifal.edu.br/edu/calendario_avaliacao/"))
                    evalResult.onSuccess { evalHtml ->
                        val docEval = org.jsoup.Jsoup.parse(evalHtml)
                        val evals = com.example.util.SuapParser.parseEvaluations(docEval)
                        saveEvaluationsOffline(evals)
                        if (evals.isNotEmpty()) {
                            val currentYear = java.time.LocalDate.now().year
                            for (ev in evals) {
                                val parsedDate = parseCalendarDateStr(ev.date, currentYear)
                                if (parsedDate != null) {
                                    allEvents.add(
                                        AcademicEvent(
                                            dateInfo = ev.date,
                                            description = "${ev.subjectName}: ${ev.title.ifBlank { ev.type }}",
                                            color = "#FF80AB", // Pink for exams
                                            startDate = parsedDate,
                                            endDate = parsedDate
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    com.example.util.AppLog.e("SuapViewModel", "Error fetching evaluations for calendar", e)
                }

                // 4. Also incorporate any existing evaluations in memory
                val currentYear = java.time.LocalDate.now().year
                for (ev in _evaluations.value) {
                    val parsedDate = parseCalendarDateStr(ev.date, currentYear)
                    if (parsedDate != null && allEvents.none { it.description.contains(ev.subjectName) && it.startDate == parsedDate }) {
                        allEvents.add(
                            AcademicEvent(
                                dateInfo = ev.date,
                                description = "${ev.subjectName}: ${ev.title.ifBlank { ev.type }}",
                                color = "#FF80AB",
                                startDate = parsedDate,
                                endDate = parsedDate
                            )
                        )
                    }
                }

                val distinctEvents = allEvents.distinctBy { (it.startDate?.toString() ?: it.dateInfo) + it.description }

                if (distinctEvents.isNotEmpty()) {
                    _academicEvents.value = distinctEvents
                    saveAcademicCalendarToRoom(distinctEvents)
                    saveAcademicCalendarToPrefs(distinctEvents)
                } else {
                    _calendarError.value = "O SUAP não retornou eventos do calendário."
                    val offline = loadAcademicCalendarFromPrefs()
                    if (offline.isNotEmpty()) _academicEvents.value = offline
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                _calendarError.value = "Não foi possível atualizar o calendário. Verifique sua conexão."
                val offline = loadAcademicCalendarFromPrefs()
                if (offline.isNotEmpty()) _academicEvents.value = offline
            } finally {
                _isLoadingCalendar.value = false
            }
        }
    }

    fun fetchAvisos() {
        if (!_isLoadingAvisos.compareAndSet(false, true)) return
        _avisosError.value = null
        dataScope.launch(Dispatchers.IO) {
            try {
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                val response = repo.executeRequest(okhttp3.Request.Builder()
                    .url("https://suap.ifal.edu.br/admin/edu/mensagementrada/?tab=tab_todas"))
                when (response) {
                    is com.example.network.NetworkResult.Success -> {
                        when (val result = com.example.util.SuapParser.parseAvisos(Jsoup.parse(response.data))) {
                            is com.example.util.ParserResult.Success -> {
                                val notices = result.data.take(20)
                                _avisos.value = notices
                                saveAvisosToPrefs(notices)
                            }
                            is com.example.util.ParserResult.Error -> {
                                _avisosError.value = "Não foi possível ler os avisos do SUAP."
                            }
                        }
                    }
                    is com.example.network.NetworkResult.Error -> _avisosError.value = response.message
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
                _avisosError.value = "Não foi possível atualizar os avisos. Verifique sua conexão."
            } finally {
                _isLoadingAvisos.value = false
            }
        }
    }

    private val _materials = MutableStateFlow<List<com.example.data.Material>>(emptyList())
    val materials: StateFlow<List<com.example.data.Material>> = _materials
    
    private val _isLoadingMaterials = MutableStateFlow(false)
    val isLoadingMaterials: StateFlow<Boolean> = _isLoadingMaterials
    private val _materialsError = MutableStateFlow<String?>(null)
    val materialsError = _materialsError.asStateFlow()
    private val materialsRequestVersion = java.util.concurrent.atomic.AtomicLong()
    
    private var fetchMaterialsJob: kotlinx.coroutines.Job? = null

    private val _selectedSubjectName = MutableStateFlow("")
    val selectedSubjectName: StateFlow<String> = _selectedSubjectName.asStateFlow()

    private val _selectedSubjectUrl = MutableStateFlow("")
    val selectedSubjectUrl: StateFlow<String> = _selectedSubjectUrl.asStateFlow()

    private fun matchSubjectUrl(targetName: String, urlMap: Map<String, String>): String? {
        if (urlMap.isEmpty() || targetName.isBlank()) return null

        // 1. Exact match with key
        urlMap.keys.find { it.equals(targetName, ignoreCase = true) }?.let { return urlMap[it] }

        fun normalize(name: String) = name
            .replace(SUBJECT_CODE_PATTERN, "")
            .replace(SUBJECT_TAG_PATTERN, "")
            .replace(SUBJECT_LEVEL_PATTERN, "")
            .replace(SUBJECT_PUNCTUATION_PATTERN, " ")
            .replace(SUBJECT_WHITESPACE_PATTERN, " ")
            .trim()

        val cleanTarget = normalize(targetName)

        if (cleanTarget.isBlank()) return null
        val entries = urlMap.map { (name, url) -> Triple(normalize(name), name, url) }

        // 2. Exact match with cleaned key
        entries.firstOrNull { it.first.equals(cleanTarget, ignoreCase = true) }?.let { return it.third }

        // 3. Exact word list match
        val targetWords = cleanTarget.lowercase(Locale.ROOT).split(" ").filter { it.length > 2 }
        entries.firstOrNull {
            val keyWords = it.first.lowercase(Locale.ROOT).split(" ").filter { word -> word.length > 2 }
            keyWords == targetWords && keyWords.isNotEmpty()
        }?.let { return it.third }

        // 4. Overlap scoring
        var bestUrl: String? = null
        var bestScore = -1.0
        val targetWordSet = cleanTarget.lowercase(Locale.ROOT).split(" ").filter { it.isNotBlank() }.toSet()
        for ((keyClean, _, url) in entries) {
            val keyWords = keyClean.lowercase(Locale.ROOT).split(" ").filter { it.isNotBlank() }.toSet()
            val intersection = keyWords.intersect(targetWordSet).size
            val union = keyWords.union(targetWordSet).size
            val score = if (union > 0) intersection.toDouble() / union.toDouble() else 0.0
            if (score > bestScore && score > 0.3) {
                bestScore = score
                bestUrl = url
            }
        }

        return bestUrl
    }

    fun selectSubject(name: String, url: String) {
        _selectedSubjectName.value = name
        _selectedSubjectUrl.value = url
        fetchMaterials(url)
    }

    fun fetchMaterials(subjectUrl: String) {
        val targetName = _selectedSubjectName.value
        com.example.util.AppLog.d("SUAP_MATERIAIS", "Fetching materials for subject: '$targetName'")

        val requestVersion = materialsRequestVersion.incrementAndGet()
        fetchMaterialsJob?.cancel()
        _materialsError.value = null
        val cachedMaterials = com.example.data.MaterialOfflineCache.load(applicationContext, subjectUrl)
        if (cachedMaterials.isNotEmpty()) _materials.value = cachedMaterials
        _isLoadingMaterials.value = true

        fetchMaterialsJob = dataScope.launch(Dispatchers.IO) {
            try {
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                suspend fun loadDocument(url: String): org.jsoup.nodes.Document {
                    return when (val result = repo.executeRequest(okhttp3.Request.Builder().url(url))) {
                        is com.example.network.NetworkResult.Success -> Jsoup.parse(result.data, url)
                        is com.example.network.NetworkResult.Error -> throw java.io.IOException(result.message)
                    }
                }
                var finalUrl = subjectUrl.trim()

                // Fetch list of disciplines if finalUrl does not point to a specific /edu/disciplina/
                if (!finalUrl.contains("/edu/disciplina/")) {
                    try {
                        val listDoc = loadDocument("https://suap.ifal.edu.br/edu/disciplinas/")
                        val links = listDoc.select("h3.title a")
                        val urlMap = mutableMapOf<String, String>()
                        for (link in links) {
                            val href = link.attr("href").trim()
                            val title = link.text().trim()
                            if (href.isNotEmpty() && title.isNotEmpty()) {
                                urlMap[title] = href
                            }
                        }
                        
                        val matchedUrl = matchSubjectUrl(targetName, urlMap)
                        if (matchedUrl != null) {
                            finalUrl = matchedUrl
                            _selectedSubjectUrl.value = finalUrl
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        com.example.util.AppLog.printStackTrace(e)
                    }
                }

                if (finalUrl.isBlank()) throw java.io.IOException("Disciplina não localizada no SUAP.")
                val pageUrl = "https://suap.ifal.edu.br/".toHttpUrl().resolve(finalUrl)
                    ?: throw java.io.IOException("Endereço da disciplina inválido.")
                val urlToFetch = pageUrl.newBuilder().setQueryParameter("tab", "materiais").build().toString()
                val doc = loadDocument(urlToFetch)
                kotlinx.coroutines.currentCoroutineContext().ensureActive()

                parseAndSyncProfessorsFromHtml(doc)
                    
                var rows = doc.select("div[data-tab=materiais] table tbody tr")
                if (rows.isEmpty()) {
                    rows = doc.select("div#materiais-de-aula table tbody tr")
                }
                if (rows.isEmpty()) {
                    rows = doc.select("div#materiais table tbody tr")
                }
                if (rows.isEmpty()) {
                    rows = doc.select("div.tab-pane table tbody tr")
                }
                if (rows.isEmpty()) {
                    rows = doc.select("table.listagem tbody tr, table tbody tr")
                }
                
                com.example.util.AppLog.d("SUAP_MATERIAIS", "Found ${rows.size} rows")
                val parsedMaterials = mutableListOf<com.example.data.Material>()
                
                for (row in rows) {
                    val cols = row.select("td")
                    if (cols.isNotEmpty()) {
                        val allLinks = row.select("a[href]")
                        val validLink = allLinks.find { link ->
                            val href = link.attr("href")
                            href.contains("/registrar_acesso") || href.contains("/material/") || href.contains("/media/") || href.contains("/download/")
                        } ?: allLinks.getOrNull(1) ?: allLinks.firstOrNull()

                        if (validLink != null) {
                            val url = validLink.attr("href").trim()
                            var title = validLink.text().trim()
                            if (title.isEmpty() || title.equals("Visualizar", ignoreCase = true)) {
                                if (cols.size >= 2) {
                                    title = cols[1].text().trim()
                                }
                            }
                            if (title.isEmpty() || title.equals("Visualizar", ignoreCase = true)) {
                                title = "Material de Aula"
                            }
                            val date = if (cols.size >= 3) cols[2].text().trim() else ""

                            if (url.isNotEmpty() && parsedMaterials.none { it.url == url }) {
                                parsedMaterials.add(com.example.data.Material(title, url, date))
                            }
                        }
                    }
                }

                if (parsedMaterials.isEmpty()) {
                    val allLinks = doc.select("a[href*=/registrar_acesso_aluno_material_aula/], a[href*=/media/], a[href*=/material/], a[href*=/download/]")
                    for (link in allLinks) {
                        var title = link.text().trim()
                        val url = link.attr("href").trim()
                        val parentTd = link.parent()
                        if (title.isEmpty() || title.equals("Visualizar", ignoreCase = true)) {
                            title = parentTd?.parent()?.select("td")?.getOrNull(1)?.text()?.trim() ?: "Material de Aula"
                        }
                        if (title.isEmpty() || title.equals("Visualizar", ignoreCase = true)) {
                            title = "Material de Aula"
                        }
                        if (url.isNotEmpty() && parsedMaterials.none { it.url == url }) {
                            parsedMaterials.add(com.example.data.Material(title, url, ""))
                        }
                    }
                }

                com.example.util.AppLog.d("SUAP_MATERIAIS", "Parsed ${parsedMaterials.size} materials")
                if (materialsRequestVersion.get() == requestVersion) {
                    _materials.value = parsedMaterials
                    if (parsedMaterials.isNotEmpty()) com.example.data.MaterialOfflineCache.save(applicationContext, subjectUrl, parsedMaterials)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.AppLog.e("SUAP_MATERIAIS", "Error fetching materials", e)
                if (materialsRequestVersion.get() == requestVersion) {
                    _materialsError.value = "Não foi possível carregar os materiais. Verifique sua conexão e tente novamente."
                }
            } finally {
                if (materialsRequestVersion.get() == requestVersion) _isLoadingMaterials.value = false
            }
        }
    }
    
    fun clearMaterials() {
        materialsRequestVersion.incrementAndGet()
        fetchMaterialsJob?.cancel()
        _materials.value = emptyList()
        _materialsError.value = null
        _isLoadingMaterials.value = false
    }

    fun syncDisciplinasUrls() {
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    val doc = org.jsoup.Jsoup.connect("https://suap.ifal.edu.br/edu/disciplinas/")
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(15000)
                        .get()
                    parseAndSyncProfessorsFromHtml(doc)
                    
                    val links = doc.select("h3.title a")
                    val urlMap = mutableMapOf<String, String>()
                    for (link in links) {
                        val url = link.attr("href").trim()
                        val name = link.text().trim()
                        if (url.isNotEmpty() && name.isNotEmpty()) {
                            urlMap[name] = url
                        }
                    }
                    
                    val currentSubjects = repository.allSubjects.first()
                    if (currentSubjects.isNotEmpty()) {
                        val updatedSubjects = currentSubjects.map { subject ->
                            val matchedUrl = matchSubjectUrl(subject.name, urlMap)
                            if (matchedUrl != null) {
                                subject.copy(url = matchedUrl)
                            } else {
                                subject
                            }
                        }
                        val changedSubjects = updatedSubjects.filterIndexed { index, subject ->
                            subject != currentSubjects[index]
                        }
                        if (changedSubjects.isNotEmpty()) repository.updateSubjectUrls(changedSubjects)
                    }
                }
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
            }
        }
    }

    fun loadScheduleOffline() {
        val schedJson = sharedPreferences.getString("offline_schedule_json", "") ?: ""
        val diarioJson = sharedPreferences.getString("offline_diarios_json", "") ?: ""

        val scheduleList = mutableListOf<ScheduleItem>()
        val diarioList = mutableListOf<DiarioItem>()

        if (schedJson.isNotEmpty()) {
            try {
                val arr = JSONArray(schedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    scheduleList.add(
                        ScheduleItem(
                            dayOfWeek = obj.optString("dayOfWeek"),
                            timeSlot = obj.optString("timeSlot"),
                            shift = obj.optString("shift"),
                            subjectCode = obj.optString("subjectCode"),
                            subjectName = obj.optString("subjectName"),
                            teacher = obj.optString("teacher")
                        )
                    )
                }
            } catch (e: Exception) { com.example.util.AppLog.printStackTrace(e) }
        }

        val demoSchedule = getDemoScheduleAndDiarios().first
        sharedPreferences.edit()
            .putBoolean("offline_schedule_is_demo", scheduleList.isNotEmpty() && scheduleList == demoSchedule)
            .apply()

        if (diarioJson.isNotEmpty()) {
            try {
                val arr = JSONArray(diarioJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    diarioList.add(
                        DiarioItem(
                            code = obj.optString("code"),
                            component = obj.optString("component"),
                            teacher = obj.optString("teacher"),
                            room = obj.optString("room"),
                            horarioCode = obj.optString("horarioCode")
                        )
                    )
                }
            } catch (e: Exception) { com.example.util.AppLog.printStackTrace(e) }
        }

        val isGeneratedSchedule = scheduleList.isNotEmpty() && scheduleList == demoSchedule
        _scheduleItems.value = if (isGeneratedSchedule) emptyList() else scheduleList
        _diarioItems.value = if (isGeneratedSchedule) emptyList() else diarioList
    }

    fun saveScheduleOffline(schedule: List<ScheduleItem>, diarios: List<DiarioItem>, isDemo: Boolean = false) {
        val scheduleArray = JSONArray()
        for (s in schedule) {
            val obj = JSONObject()
            obj.put("dayOfWeek", s.dayOfWeek)
            obj.put("timeSlot", s.timeSlot)
            obj.put("shift", s.shift)
            obj.put("subjectCode", s.subjectCode)
            obj.put("subjectName", s.subjectName)
            obj.put("teacher", s.teacher)
            scheduleArray.put(obj)
        }

        val diarioArray = JSONArray()
        for (d in diarios) {
            val obj = JSONObject()
            obj.put("code", d.code)
            obj.put("component", d.component)
            obj.put("teacher", d.teacher)
            obj.put("room", d.room)
            obj.put("horarioCode", d.horarioCode)
            diarioArray.put(obj)
        }

        sharedPreferences.edit()
            .putString("offline_schedule_json", scheduleArray.toString())
            .putString("offline_diarios_json", diarioArray.toString())
            .putBoolean("offline_schedule_is_demo", isDemo)
            .apply()

        _scheduleItems.value = schedule
        _diarioItems.value = diarios
        if (sharedPreferences.getBoolean("class_notification_enabled", false)) {
            com.example.receiver.ClassReminderReceiver.scheduleNextClassAlarm(applicationContext)
        }
    }

    fun loadProfessorContactsOffline() {
        val savedJson = sharedPreferences.getString("offline_professors_json", "") ?: ""
        if (savedJson.isNotEmpty()) {
            try {
                val list = mutableListOf<ProfessorContact>()
                val arr = JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val pName = obj.optString("name")
                    var pEmail = obj.optString("email")
                    var pEstimated = obj.optBoolean("emailEstimated", false)
                    // Mark historical full-name guesses as estimates even if an older version saved the flag as false.
                    if (pEmail.isBlank() ||
                        !com.example.util.ProfessorEmail.isValid(pEmail) ||
                        pEmail.equals(com.example.util.ProfessorEmail.legacyGuess(pName), ignoreCase = true)
                    ) {
                        pEmail = com.example.util.ProfessorEmail.estimate(pName)
                        pEstimated = true
                    }
                    list.add(
                        ProfessorContact(
                            name = pName,
                            email = pEmail,
                            discipline = obj.optString("discipline"),
                            photoUrl = obj.optString("photoUrl"),
                            matricula = obj.optString("matricula"),
                            emailEstimated = pEstimated,
                            profileUrl = obj.optString("profileUrl")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _professorContacts.value = list
                    return
                }
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
            }
        }
    }

    fun loadEvaluationsOffline() {
        val savedJson = sharedPreferences.getString("offline_evaluations_json", "") ?: ""
        if (savedJson.isNotEmpty()) {
            try {
                val list = mutableListOf<EvaluationItem>()
                val arr = JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id")
                    // Do not load old mock demo items
                    if (id.startsWith("eval_demo_")) continue
                    list.add(
                        EvaluationItem(
                            id = id,
                            subjectName = obj.optString("subjectName"),
                            title = obj.optString("title"),
                            date = obj.optString("date"),
                            timeOrDetail = obj.optString("timeOrDetail"),
                            type = obj.optString("type", "Avaliação")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _evaluations.value = list
                    return
                }
            } catch (e: Exception) {
                com.example.util.AppLog.printStackTrace(e)
            }
        }
        _evaluations.value = emptyList()
    }

    fun saveEvaluationsOffline(evals: List<EvaluationItem>) {
        val array = JSONArray()
        for (item in evals) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("subjectName", item.subjectName)
            obj.put("title", item.title)
            obj.put("date", item.date)
            obj.put("timeOrDetail", item.timeOrDetail)
            obj.put("type", item.type)
            array.put(obj)
        }
        sharedPreferences.edit().putString("offline_evaluations_json", array.toString()).apply()
        _evaluations.value = evals
    }

    fun fetchEvaluations() {
        dataScope.launch(Dispatchers.IO) {
            try {
                val url = "https://suap.ifal.edu.br/edu/calendario_avaliacao/"
                val repo = com.example.network.SuapAuthRepository.getInstance(applicationContext)
                val result = repo.executeRequest(okhttp3.Request.Builder().url(url))
                result.onSuccess { html ->
                    val doc = Jsoup.parse(html)
                    val parsed = com.example.util.SuapParser.parseEvaluations(doc)
                    saveEvaluationsOffline(parsed)
                    // Sync parsed real evaluations into academic calendar events
                    if (parsed.isNotEmpty()) {
                        val currentYear = java.time.LocalDate.now().year
                        val examEvents = parsed.mapNotNull { ev ->
                            val dt = parseCalendarDateStr(ev.date, currentYear)
                            if (dt != null) {
                                AcademicEvent(
                                    dateInfo = ev.date,
                                    description = "${ev.subjectName}: ${ev.title.ifBlank { ev.type }}",
                                    color = "#FF4081",
                                    startDate = dt,
                                    endDate = dt
                                )
                            } else null
                        }
                        if (examEvents.isNotEmpty()) {
                            val combined = (_academicEvents.value + examEvents).distinctBy { (it.startDate?.toString() ?: it.dateInfo) + it.description }
                            _academicEvents.value = combined
                            saveAcademicCalendarToRoom(combined)
                            saveAcademicCalendarToPrefs(combined)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                com.example.util.AppLog.e("SUAP_EVALUATIONS", "Error fetching evaluations", e)
            }
        }
    }

    fun parseAndSyncProfessorsFromHtml(document: org.jsoup.nodes.Document, diarioList: List<DiarioItem> = emptyList()) {
        val currentList = _professorContacts.value
        val contactMap = currentList.associateBy { com.example.util.ProfessorEmail.nameKey(it.name) }.toMutableMap()

        try {
            // 0. Look for .farol elements in SUAP HTML (minhas disciplinas list)
            val farols = document.select(".farol")
            for (farol in farols) {
                val disc = farol.select("h3.title a").text().trim()
                val persons = farol.select(".person")
                for (person in persons) {
                    val imgNode = person.select("img").first()
                    var imgUrl = imgNode?.attr("src")?.trim() ?: ""
                    val altText = imgNode?.attr("alt")?.trim() ?: ""
                    val shortName = person.select("dd").text().trim()
                    val fullName = if (altText.startsWith("Foto de ", ignoreCase = true)) altText.substring(8).trim() else shortName
                    
                    if (imgUrl.isNotEmpty() && !imgUrl.startsWith("http")) {
                        if (!imgUrl.startsWith("/")) imgUrl = "/$imgUrl"
                        if (imgUrl != "/static/comum/img/default.jpg") {
                            imgUrl = "https://suap.ifal.edu.br$imgUrl"
                        } else {
                            imgUrl = ""
                        }
                    } else if (imgUrl.contains("/static/comum/img/default.jpg")) {
                        imgUrl = ""
                    }

                    if (fullName.isNotEmpty()) {
                        val key = com.example.util.ProfessorEmail.nameKey(fullName)
                        val existing = contactMap[key]
                        val (resolvedEmail, estimated) = com.example.util.ProfessorEmail.resolve(
                            com.example.util.ProfessorEmail.find(person), existing, fullName
                        )
                        contactMap[key] = ProfessorContact(
                            name = fullName,
                            email = resolvedEmail,
                            discipline = if (disc.isNotEmpty()) disc else existing?.discipline ?: "",
                            photoUrl = if (imgUrl.isNotEmpty()) imgUrl else existing?.photoUrl ?: "",
                            matricula = existing?.matricula ?: "",
                            emailEstimated = estimated,
                            profileUrl = com.example.util.ProfessorEmail.findProfileLink(person) ?: existing?.profileUrl ?: ""
                        )
                    }
                }
            }

            // 1. Look for .card elements in SUAP HTML (discipline & teacher cards)
            val cards = document.select(".card, .card-container .card")
            for (card in cards) {
                var name = card.select("dt:contains(Professor) + dd, dt:contains(Professores) + dd").text().trim()
                if (name.isEmpty()) {
                    name = card.select("h4, .nome, h3").text().trim()
                }
                var imgUrl = card.select(".photo img, img").attr("src").trim()
                if (imgUrl.isNotEmpty() && !imgUrl.startsWith("http")) {
                    if (!imgUrl.startsWith("/")) {
                        imgUrl = "/" + imgUrl
                    }
                    if (imgUrl != "/static/comum/img/default.jpg") {
                        imgUrl = "https://suap.ifal.edu.br" + imgUrl
                    } else {
                        imgUrl = ""
                    }
                } else if (imgUrl.contains("/static/comum/img/default.jpg")) {
                    imgUrl = ""
                }
                // Um único e-mail no card só pode ser atribuído com segurança se houver um só professor
                val cardEmail = com.example.util.ProfessorEmail.find(card)
                val matricula = card.select("dt:contains(Matrícula) + dd, dt:contains(Matricula) + dd").text().trim()

                if (name.isNotEmpty()) {
                    val names = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (tName in names) {
                        val key = com.example.util.ProfessorEmail.nameKey(tName)
                        val existing = contactMap[key]
                        val (resolvedEmail, estimated) = com.example.util.ProfessorEmail.resolve(
                            if (names.size == 1) cardEmail else null, existing, tName
                        )
                        contactMap[key] = ProfessorContact(
                            name = tName,
                            email = resolvedEmail,
                            discipline = existing?.discipline ?: "",
                            photoUrl = if (imgUrl.isNotEmpty()) imgUrl else existing?.photoUrl ?: "",
                            matricula = if (matricula.isNotEmpty()) matricula else existing?.matricula ?: "",
                            emailEstimated = estimated,
                            profileUrl = existing?.profileUrl ?: ""
                        )
                    }
                }
            }

            // 2. Extract teachers from diarioList
            for (diario in diarioList) {
                if (diario.teacher.isNotEmpty()) {
                    val teachers = diario.teacher.split(",").map { it.trim() }
                    for (tName in teachers) {
                        if (tName.isBlank()) continue
                        val key = com.example.util.ProfessorEmail.nameKey(tName)
                        val existing = contactMap[key]
                        val disc = diario.component.substringAfter(" - ").substringBefore(" - ").trim().ifEmpty { diario.component }
                        if (existing == null) {
                            contactMap[key] = ProfessorContact(
                                name = tName,
                                email = com.example.util.ProfessorEmail.estimate(tName),
                                discipline = disc,
                                photoUrl = "",
                                matricula = "",
                                emailEstimated = true
                            )
                        } else if (existing.discipline.isBlank()) {
                            contactMap[key] = existing.copy(discipline = disc)
                        }
                    }
                }
            }

            val updated = contactMap.values.toList()
            if (updated.isNotEmpty()) {
                saveProfessorContactsOffline(updated)
                enrichProfessorEmails()
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }

    /** Busca no perfil do SUAP o e-mail real dos professores que ainda têm e-mail estimado. */
    private fun enrichProfessorEmails() {
        val targets = _professorContacts.value.filter { it.emailEstimated && it.profileUrl.isNotBlank() }
        if (targets.isEmpty()) return
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
            if (cookie.isNullOrEmpty()) return@launch
            val now = System.currentTimeMillis()
            val oneDay = 24L * 60 * 60 * 1000
            var changed = false
            for (t in targets) {
                if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(t.profileUrl)) continue
                val triedKey = "prof_email_try_${t.profileUrl.hashCode()}"
                if (now - sharedPreferences.getLong(triedKey, 0L) < oneDay) continue
                try {
                    val doc = org.jsoup.Jsoup.connect(t.profileUrl)
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(10000)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; K)")
                        .get()
                    sharedPreferences.edit().putLong(triedKey, now).apply()
                    val found = com.example.util.ProfessorEmail.fromProfile(doc, t.name)
                    com.example.util.AppLog.i(message = "E-mail professor '${t.name}' (${t.profileUrl}): ${found ?: "não encontrado no perfil"}")
                    if (found != null) {
                        // Re-read the current state after the request: the user may have
                        // corrected this address while the profile was loading.
                        val latest = _professorContacts.value
                        val idx = latest.indexOfFirst {
                            com.example.util.ProfessorEmail.nameKey(it.name) == com.example.util.ProfessorEmail.nameKey(t.name)
                        }
                        if (idx != -1 && latest[idx].emailEstimated && latest[idx].profileUrl == t.profileUrl) {
                            val merged = latest.toMutableList()
                            merged[idx] = merged[idx].copy(email = found, emailEstimated = false)
                            saveProfessorContactsOffline(merged)
                            changed = true
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    com.example.util.AppLog.e(message = "Erro ao buscar e-mail de '${t.name}': ${e.message}", throwable = e)
                }
            }
            if (changed) com.example.util.AppLog.i(message = "Contatos de professores atualizados com e-mails confirmados.")
        }
    }

    /** Permite ao usuário corrigir manualmente o e-mail de um professor. */
    fun setProfessorEmail(name: String, newEmail: String): Boolean {
        val clean = newEmail.trim().lowercase()
        if (clean.isNotEmpty() && !com.example.util.ProfessorEmail.isValid(clean)) return false
        val list = _professorContacts.value.toMutableList()
        val idx = list.indexOfFirst { it.name == name }
        if (idx == -1) return false
        list[idx] = if (clean.isEmpty()) {
            list[idx].copy(email = com.example.util.ProfessorEmail.estimate(name), emailEstimated = true)
        } else {
            list[idx].copy(email = clean, emailEstimated = false)
        }
        saveProfessorContactsOffline(list)
        return true
    }

    fun saveProfessorContactsOffline(contacts: List<ProfessorContact>) {
        val arr = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("email", c.email)
            obj.put("discipline", c.discipline)
            obj.put("photoUrl", c.photoUrl)
            obj.put("matricula", c.matricula)
            obj.put("emailEstimated", c.emailEstimated)
            obj.put("profileUrl", c.profileUrl)
            arr.put(obj)
        }
        sharedPreferences.edit()
            .putString("offline_professors_json", arr.toString())
            .apply()
        _professorContacts.value = contacts
    }

    private fun parseScheduleFromHtml(document: org.jsoup.nodes.Document): Pair<List<ScheduleItem>, List<DiarioItem>> {
        val scheduleList = mutableListOf<ScheduleItem>()
        val diarioList = mutableListOf<DiarioItem>()

        try {
            val tables = document.select(".calendario table, #horarios table")
            for (table in tables) {
                var headers = table.select("thead tr th, tr:first-child th").map { it.text().trim() }
                if (headers.isEmpty() || headers.size < 2 || !headers.any { it.contains("Seg", ignoreCase = true) || it.contains("2", ignoreCase = true) }) {
                    headers = listOf("Horários", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
                }
                val shift = headers.firstOrNull() ?: "Aulas"
                val days = headers.drop(1).filter { it.isNotEmpty() }

                val tbodyRows = table.select("tbody tr")
                val rows = if (tbodyRows.isNotEmpty()) tbodyRows else table.select("tr")

                for (row in rows) {
                    val cols = row.select("td, th")
                    if (cols.isEmpty()) continue
                    
                    val rowText = row.text().lowercase()
                    if (rowText.contains("segunda") || rowText.contains("terça") || 
                        rowText.contains("quarta") || rowText.contains("quinta") || 
                        rowText.contains("sexta") || rowText.contains("matutino") || 
                        rowText.contains("vespertino") || rowText.contains("noturno")) continue

                    val timeSlot = cols.first()?.text()?.trim() ?: ""
                    if (timeSlot.isEmpty() || timeSlot.length < 3 || !timeSlot.contains("-")) continue
                    
                    // Check for single interval cell spanning all days
                    if (cols.size == 2 && cols[1].text().contains("Intervalo", ignoreCase = true)) {
                        for (dayName in days) {
                            scheduleList.add(
                                ScheduleItem(
                                    dayOfWeek = dayName,
                                    timeSlot = timeSlot,
                                    shift = shift,
                                    subjectCode = "",
                                    subjectName = "Intervalo",
                                    teacher = ""
                                )
                            )
                        }
                        continue
                    }

                    for (i in 1 until cols.size) {
                        val dayIndex = i - 1
                        if (dayIndex >= days.size) break
                        val dayName = days[dayIndex]
                        val cell = cols[i]
                        
                        // Check if this specific cell is an interval
                        if (cell.text().contains("Intervalo", ignoreCase = true) && cell.select("li, dl").isEmpty()) {
                            scheduleList.add(
                                ScheduleItem(
                                    dayOfWeek = dayName,
                                    timeSlot = timeSlot,
                                    shift = shift,
                                    subjectCode = "",
                                    subjectName = "Intervalo",
                                    teacher = ""
                                )
                            )
                            continue
                        }

                        val items = cell.select("li.calendario-item, div.calendario-item")
                        if (items.isNotEmpty()) {
                            for (item in items) {
                                val code = item.select("small").text().trim().replace("(", "").replace(")", "")
                                val titleAttr = item.attr("title").trim()
                                val name = if (titleAttr.isNotEmpty()) titleAttr else item.select("strong, b, span.disciplina").firstOrNull()?.text()?.trim() ?: item.text().trim()
                                
                                if (name.isEmpty() || name.length < 2) continue

                                val dds = item.select("dd").map { it.text().trim() }
                                val teacher = if (dds.size > 2) dds.drop(2).filter { it.isNotEmpty() }.joinToString(", ")
                                               else if (dds.size == 2 && !dds[1].contains(code)) dds[1]
                                               else ""

                                scheduleList.add(
                                    ScheduleItem(
                                        dayOfWeek = dayName,
                                        timeSlot = timeSlot,
                                        shift = shift,
                                        subjectCode = code,
                                        subjectName = name,
                                        teacher = teacher
                                    )
                                )
                            }
                        } else {
                            val dlItems = cell.select("dl")
                            if (dlItems.isNotEmpty()) {
                                for (dl in dlItems) {
                                    val code = dl.select("small").text().trim().replace("(", "").replace(")", "")
                                    val name = dl.select("strong, b").firstOrNull()?.text()?.trim() ?: dl.text().trim()
                                    if (name.isEmpty() || name.length < 2) continue
                                    val dds = dl.select("dd").map { it.text().trim() }
                                    val teacher = if (dds.size > 2) dds.drop(2).filter { it.isNotEmpty() }.joinToString(", ") else ""
                                    scheduleList.add(
                                        ScheduleItem(
                                            dayOfWeek = dayName,
                                            timeSlot = timeSlot,
                                            shift = shift,
                                            subjectCode = code,
                                            subjectName = name,
                                            teacher = teacher
                                        )
                                    )
                                }
                            } else if (cell.text().trim().length > 3) {
                                val text = cell.text().trim()
                                if (!text.equals("Intervalo", ignoreCase = true)) {
                                    scheduleList.add(
                                        ScheduleItem(
                                            dayOfWeek = dayName,
                                            timeSlot = timeSlot,
                                            shift = shift,
                                            subjectCode = "",
                                            subjectName = text,
                                            teacher = ""
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val diarioRows = document.select("#diarios table tbody tr")
            for (row in diarioRows) {
                val cols = row.select("td")
                if (cols.size >= 4) {
                    val code = cols[0].text().trim()
                    val compText = cols[1].select("dd").firstOrNull()?.text()?.trim() ?: cols[1].text().trim()
                    val teacherText = cols[1].select("dd").getOrNull(1)?.text()?.trim() ?: ""
                    val roomText = cols[2].text().trim()
                    val horarioCode = cols[3].text().trim()

                    diarioList.add(
                        DiarioItem(
                            code = code,
                            component = compText,
                            teacher = teacherText,
                            room = roomText,
                            horarioCode = horarioCode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }

        return Pair(scheduleList, diarioList)
    }

    private fun getDemoScheduleAndDiarios(): Pair<List<ScheduleItem>, List<DiarioItem>> {
        val sched = listOf(
            ScheduleItem("Segunda-feira", "07:00 - 08:00", "Matutino", "MAT01", "Matemática", "Prof A"),
            ScheduleItem("Segunda-feira", "08:00 - 09:00", "Matutino", "MAT01", "Matemática", "Prof A"),
            ScheduleItem("Segunda-feira", "09:00 - 09:15", "Matutino", "", "Intervalo", ""),
            ScheduleItem("Segunda-feira", "09:15 - 10:15", "Matutino", "FIS01", "Física", "Prof B"),
            ScheduleItem("Terça-feira", "07:00 - 08:00", "Matutino", "FIS01", "Física", "Prof B"),
            ScheduleItem("Terça-feira", "09:00 - 09:15", "Matutino", "", "Intervalo", "")
        )
        val diarios = emptyList<DiarioItem>()
        return Pair(sched, diarios)
    }

    private val detailedSubjects = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val _subjectDetails = MutableStateFlow<Map<String, List<EtapaDetail>>>(emptyMap())
    val subjectDetails: StateFlow<Map<String, List<EtapaDetail>>> = _subjectDetails.asStateFlow()

    init {
        // Start loaders only after every state holder has been initialized.
        dataScope.launch(Dispatchers.IO) {
            loadScheduleOffline()
            loadProfessorContactsOffline()
            loadEvaluationsOffline()
        }
        loadAcademicCalendarOffline()
    }

    fun fetchSubjectDetails(subject: Subject) {
        val subjectKey = subject.name
        if (subjectKey in detailedSubjects) {
            return
        }
        dataScope.launch(Dispatchers.IO) {
            val urlToFetch = if (subject.detailUrl.isNotEmpty()) {
                if (subject.detailUrl.startsWith("http")) subject.detailUrl else "https://suap.ifal.edu.br${subject.detailUrl}"
            } else if (subject.url.isNotEmpty()) {
                if (subject.url.startsWith("http")) subject.url else "https://suap.ifal.edu.br${subject.url}"
            } else ""

            if (com.example.util.UrlSecurityUtils.isAllowedSuapUrl(urlToFetch)) {
                try {
                    val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                    val conn = org.jsoup.Jsoup.connect(urlToFetch)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .followRedirects(false)
                        .timeout(10000)
                    if (!cookie.isNullOrEmpty()) {
                        conn.header("Cookie", cookie)
                    }
                    val doc = conn.get()
                    val parsedEtapas = parseEtapaDetailsFromDocument(doc)
                    if (parsedEtapas.isNotEmpty()) {
                        _subjectDetails.update { it + (subjectKey to parsedEtapas) }
                        detailedSubjects.add(subjectKey)
                        return@launch
                    }
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
            }

            // Only summarize known bulletin grades; individual assessment details are unavailable.
            val summaryEtapas = generateSummaryEtapas(subject)
            _subjectDetails.update { it + (subjectKey to summaryEtapas) }
        }
    }

    private fun parseEtapaDetailsFromDocument(doc: org.jsoup.nodes.Document): List<EtapaDetail> {
        val result = mutableListOf<EtapaDetail>()
        val h4Elements = doc.select("h4")
        for (h4 in h4Elements) {
            val title = h4.text().trim()
            if (!title.contains("Etapa", ignoreCase = true)) continue

            var table: org.jsoup.nodes.Element? = null
            var sib = h4.nextElementSibling()
            while (sib != null && table == null && sib.tagName() != "h4") {
                if (sib.tagName() == "table") {
                    table = sib
                } else {
                    table = sib.selectFirst("table")
                }
                sib = sib.nextElementSibling()
            }

            val items = mutableListOf<GradeDetailRow>()
            if (table != null) {
                val rows = table.select("tbody tr")
                for (tr in rows) {
                    val cols = tr.select("td")
                    if (cols.size >= 5) {
                        items.add(
                            GradeDetailRow(
                                sigla = cols[0].text().trim(),
                                tipo = cols[1].text().trim(),
                                descricao = cols[2].text().trim(),
                                peso = cols[3].text().trim(),
                                nota = cols[4].text().trim()
                            )
                        )
                    }
                }
            }
            if (items.isNotEmpty()) {
                result.add(EtapaDetail(title = title, items = items))
            }
        }
        return result
    }

    private fun generateSummaryEtapas(subject: Subject): List<EtapaDetail> {
        val grades = listOf(subject.grade1, subject.grade2, subject.grade3, subject.grade4)
        val recoveries = listOf(subject.rec1, subject.rec2, "", "")
        val summaries = grades.mapIndexed { index, grade ->
            val rows = mutableListOf(
                GradeDetailRow("MÉDIA", "Boletim", "Média da etapa no boletim", "-", grade.ifBlank { "-" })
            )
            if (recoveries[index].isNotBlank()) {
                rows.add(GradeDetailRow("REC", "Recuperação", "Nota no boletim", "-", recoveries[index]))
            }
            EtapaDetail("Etapa ${index + 1} — resumo do boletim", rows)
        }.toMutableList()
        if (subject.recFinal.isNotBlank()) {
            summaries.add(EtapaDetail("Etapa final — resumo do boletim", listOf(
                GradeDetailRow("FINAL", "Recuperação", "Nota no boletim", "-", subject.recFinal)
            )))
        }
        return summaries
    }

    suspend fun getMessageDetail(urlPath: String): com.example.data.MessageDetail {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val cached = loadSavedMessageDetail(urlPath)
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    val fullUrl = if (urlPath.startsWith("http")) urlPath else "https://suap.ifal.edu.br$urlPath"
                    if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(fullUrl)) {
                        return@withContext cached ?: com.example.data.MessageDetail("Endereço da mensagem fora do SUAP oficial bloqueado.")
                    }
                    val doc = org.jsoup.Jsoup.connect(fullUrl)
                        .header("Cookie", cookie)
                        .followRedirects(false)
                        .timeout(15000)
                        .get()
                    
                    var textContent = ""
                    val contentMain = doc.select(".richtext").first()
                    if (contentMain != null) {
                        contentMain.select("br").append("\\n")
                        contentMain.select("p").prepend("\\n\\n")
                        textContent = contentMain.text().replace("\\n", "\n").trim()
                    } else {
                        textContent = doc.body().text()
                    }

                    val attachments = mutableListOf<com.example.data.Attachment>()
                    val dds = doc.select("aside.right dl.details-container dt:contains(Anexo:) + dd")
                    for (dd in dds) {
                        val aTag = dd.select("a").first()
                        if (aTag != null) {
                            var aUrl = aTag.attr("href")
                            if (!aUrl.startsWith("http")) aUrl = "https://suap.ifal.edu.br" + aUrl
                            attachments.add(com.example.data.Attachment(aTag.text(), aUrl))
                        }
                    }

                    val detail = com.example.data.MessageDetail(textContent, attachments)
                    if (textContent.isNotBlank()) {
                        saveMessageDetailToPrefs(urlPath, detail)
                    }
                    return@withContext detail
                }
                return@withContext cached ?: com.example.data.MessageDetail("Cookie não encontrado e mensagem não salva offline.")
            } catch (e: Exception) {
                return@withContext cached ?: com.example.data.MessageDetail("Erro ao carregar mensagem (Modo Offline): ${e.message}")
            }
        }
    }
    fun submitJustificativa(
        dataInicio: String,
        quantidadeDias: Int,
        motivo: String,
        justificativa: String,
        anexoUri: android.net.Uri,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        dataScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie.isNullOrEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError("Sessão não encontrada. Faça login novamente.") }
                    return@launch
                }

                // 1. Obter a página para pegar o csrfmiddlewaretoken
                val client = com.example.network.SuapClient.okHttpClient
                val getRequest = okhttp3.Request.Builder()
                    .url("https://suap.ifal.edu.br/edu/cadastrar_justificativa_falta/")
                    .build()

                val html = client.newCall(getRequest).execute().use { response ->
                    response.body?.string().orEmpty()
                }

                val csrfTokenRegex = "name=\"csrfmiddlewaretoken\" value=\"(.*?)\"".toRegex()
                val matchResult = csrfTokenRegex.find(html)
                val csrfToken = matchResult?.groupValues?.get(1)

                if (csrfToken == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError("Falha ao obter token de segurança (CSRF).") }
                    return@launch
                }

                // 2. Ler nome/tamanho; o conteúdo segue em fluxo para evitar carregar anexos inteiros na RAM.
                var fileName = "anexo.pdf"
                var fileLength = -1L
                context.contentResolver.query(anexoUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) fileLength = cursor.getLong(sizeIndex)
                    }
                }
                
                val mimeType = context.contentResolver.getType(anexoUri) ?: "application/octet-stream"
                val mediaType = mimeType.toMediaTypeOrNull()
                val fileBody = object : okhttp3.RequestBody() {
                    override fun contentType() = mediaType
                    override fun contentLength() = fileLength
                    override fun writeTo(sink: okio.BufferedSink) {
                        val input = context.contentResolver.openInputStream(anexoUri)
                            ?: throw java.io.IOException("Falha ao abrir o arquivo anexado.")
                        input.use {
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = it.read(buffer)
                                if (read < 0) break
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }

                // 3. Montar o form-data
                val requestBody = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("csrfmiddlewaretoken", csrfToken)
                    .addFormDataPart("data_inicio", dataInicio)
                    .addFormDataPart("quantidade_dias", quantidadeDias.toString())
                    .addFormDataPart("motivo_ausencia_escolar", motivo)
                    .addFormDataPart("justificativa", justificativa)
                    .addFormDataPart("cadastrarjustificativafalta_form", "Salvar")
                    .addFormDataPart("anexo", fileName, fileBody)
                    .build()

                val postRequest = okhttp3.Request.Builder()
                    .url("https://suap.ifal.edu.br/edu/cadastrar_justificativa_falta/")
                    .post(requestBody)
                    .addHeader("Referer", "https://suap.ifal.edu.br/edu/cadastrar_justificativa_falta/")
                    .addHeader("Origin", "https://suap.ifal.edu.br")
                    .build()

                var postResponseCode = 0
                var postFinalPath = ""
                var postResponseSuccessful = false
                val postResponseBody = client.newCall(postRequest).execute().use { postResponse ->
                    postResponseCode = postResponse.code
                    postFinalPath = postResponse.request.url.encodedPath
                    postResponseSuccessful = postResponse.isSuccessful
                    postResponse.body?.string().orEmpty()
                }
                // Se continuamos na página do formulário, o SUAP não aceitou o envio
                val stillOnForm = postFinalPath.contains("cadastrar_justificativa_falta") &&
                    !postResponseBody.contains("alert-success")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (postResponseSuccessful || postResponseCode == 302) {
                        // SUAP usually redirects on success
                        if (postResponseBody.contains("alert-error") || postResponseBody.contains("errorlist") || stillOnForm) {
                            onError("Erro retornado pelo SUAP ao tentar salvar a justificativa. Verifique os dados.")
                        } else {
                            onSuccess()
                        }
                    } else {
                        onError("Falha na requisição. Código: $postResponseCode")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Erro ao enviar: ${e.message}")
                }
            }
        }
    }
}
