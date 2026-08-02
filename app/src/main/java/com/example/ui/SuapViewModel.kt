package com.example.ui


import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.ui.theme.AppTheme
import androidx.lifecycle.viewModelScope
import com.example.data.Aviso
import com.example.data.EtapaDetail
import com.example.data.GradeDetailRow
import com.example.data.ProfessorContact
import com.example.data.Subject
import com.example.data.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

data class ChartBar(val subjectName: String, val studentGrade: Double, val classGrade: Double)

data class ScheduleItem(
    val dayOfWeek: String,
    val timeSlot: String,
    val shift: String,
    val subjectCode: String,
    val subjectName: String,
    val teacher: String
)

data class DiarioItem(
    val code: String,
    val component: String,
    val teacher: String,
    val room: String,
    val horarioCode: String
)

class SuapViewModel(private val repository: SubjectRepository, private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems

    private val _diarioItems = MutableStateFlow<List<DiarioItem>>(emptyList())
    val diarioItems: StateFlow<List<DiarioItem>> = _diarioItems

    private val _professorContacts = MutableStateFlow<List<ProfessorContact>>(emptyList())
    val professorContacts: StateFlow<List<ProfessorContact>> = _professorContacts

    init {
        loadScheduleOffline()
        loadProfessorContactsOffline()
    }


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

    init {
        viewModelScope.launch {
            val currentSubjects = repository.allSubjects.first()
            if (currentSubjects.any { it.name == "Matemática" && it.grade1 == "80" } || currentSubjects.any { it.name == "Física" && it.grade1 == "70" }) {
                repository.clearAll()
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
                
                sharedPreferences.edit()
                    .putString("saved_student_name", "Visitante")
                    .putString("saved_full_name", "-")
                    .putString("saved_matricula", "-")
                    .putString("saved_ingresso", "-")
                    .putString("saved_curso", "-")
                    .putString("saved_global_frequency", "-")
                    .putString("saved_global_absences", "-")
                    .putString("saved_total_classes", "-")
                    .putString("saved_passed_classes", "-")
                    .putString("saved_student_average", "-")
                    .putString("saved_class_average", "-")
                    .apply()
            } else {
                val name = sharedPreferences.getString("saved_student_name", "") ?: ""
                if (name == "Visitante" || name == "Aluno" || name == "") {
                    repository.clearAll()
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
                    
                    sharedPreferences.edit()
                        .putString("saved_student_name", "Visitante")
                        .putString("saved_full_name", "-")
                        .putString("saved_matricula", "-")
                        .putString("saved_ingresso", "-")
                        .putString("saved_curso", "-")
                        .putString("saved_global_frequency", "-")
                        .putString("saved_global_absences", "-")
                        .putString("saved_total_classes", "-")
                        .putString("saved_passed_classes", "-")
                        .putString("saved_student_average", "-")
                        .putString("saved_class_average", "-")
                        .apply()
                }
            }
        }
    }

    private val _appTheme = MutableStateFlow<AppTheme>(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme

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
            obj.put("isDark", it.isDark)
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
                result.add(
                    com.example.ui.theme.CustomThemeData(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primaryColor = com.example.ui.theme.ThemeColor.valueOf(obj.getString("primaryColor")),
                        iconColor = com.example.ui.theme.ThemeColor.valueOf(obj.getString("iconColor")),
                        isDark = obj.getBoolean("isDark")
                    )
                )
            }
        } catch(e: Exception) {}
        return result
    }

    fun setAppTheme(context: android.content.Context, theme: AppTheme) {
        _appTheme.value = theme
        sharedPreferences.edit().putString("saved_app_theme", theme.id).apply()
        
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
                AppTheme.CYBERPUNK to "Cyberpunk"
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

    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _chartBars = MutableStateFlow<List<ChartBar>>(emptyList())
    val chartBars: StateFlow<List<ChartBar>> = _chartBars.asStateFlow()


    fun parseHtmlAndSave(html: String) {
        viewModelScope.launch {
            try {
                val currentFavorites = repository.allSubjects.first().filter { it.isFavorite }.map { it.name }.toSet()
                val document = Jsoup.parse(html)
                
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
                if (footerFreq != null) {
                    _globalFrequency.value = footerFreq.text().substringBefore(" ").trim()
                }
                


                val rows = document.select("table.bordered tbody tr")
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

                val parsedSubjects = mutableListOf<Subject>()
                val avgMap = extractClassAverages(html)
                val etapaSelectedNode = document.select("a.tab.selected").firstOrNull() ?: document.select("li.active a").firstOrNull()
                var currentEtapa = 1
                if (etapaSelectedNode != null) {
                    val href = etapaSelectedNode.attr("href")
                    val m = Regex("etapa=([0-9]+)").find(href)
                    if (m != null) {
                        currentEtapa = m.groupValues[1].toIntOrNull() ?: 1
                    }
                }
                   
                for (row in rows) {
                    val cols = row.select("td")
                    if (cols.size >= 19) {
                        val link = cols[1].selectFirst("a")
                        val url = link?.attr("href") ?: ""
                        var name = cols[1].text().trim()
                        if (name.startsWith("INT")) {
                            name = name.substringAfter(" - ").trim()
                        }
                        


                        val absences = cols[4].text().trim()
                        val status = cols[6].text().trim()
                                        // User requested to use 'N' column for grades.
                                        val grade1 = if (cols.size > 7) cols[7].text().trim() else ""
                                        val grade2 = if (cols.size > 9) cols[9].text().trim() else ""
                                        val grade3 = if (cols.size > 11) cols[11].text().trim() else ""
                                        val grade4 = if (cols.size > 13) cols[13].text().trim() else ""
                                        
                                        // Recuperações are often in the detail page, or not present here.
                                        val rec1 = ""
                                        val rec2 = ""
                                        val recFinal = ""
                                        val finalGrade = if (cols.size > 15) cols[15].text().trim() else "" 
                                        
                                        val detailUrl = row.selectFirst("a.popup")?.attr("href") ?: if (cols.size > 19) cols[19].selectFirst("a")?.attr("href") ?: "" else ""
                                        
                                        val matchedKey = avgMap.keys.find { cat -> name.contains(cat, ignoreCase = true) }
                                        var cg1 = ""
                                        var cg2 = ""
                                        var cg3 = ""
                                        var cg4 = ""
                                        
                                        
                                        if (matchedKey != null) {
                                            val avg = avgMap[matchedKey]
                                            if (avg != null && avg > 0.0) {
                                                when (currentEtapa) {
                                                    1 -> cg1 = avg.toString()
                                                    2 -> cg2 = avg.toString()
                                                    3 -> cg3 = avg.toString()
                                                    4 -> cg4 = avg.toString()
                                                }
                                            }
                                        }

                                        parsedSubjects.add(
                                            Subject(
                                                name = name,
                                                url = url,
                                                grade1 = grade1,
                                                grade2 = grade2,
                                                rec1 = rec1,
                                                grade3 = grade3,
                                                grade4 = grade4,
                                                classGrade1 = cg1,
                                                classGrade2 = cg2,
                                                classGrade3 = cg3,
                                                classGrade4 = cg4,
                                                rec2 = rec2,
                                                recFinal = recFinal,
                                                finalGrade = finalGrade,
                                                absences = absences,
                                                status = status,
                                                isFavorite = currentFavorites.contains(name),
                                                detailUrl = detailUrl
                                            )
                                        )
                    }
                }

                repository.clearAll()
                if (parsedSubjects.isNotEmpty()) {
                    repository.insertAll(parsedSubjects)
                    syncDisciplinasUrls()
                }

                // Parse schedule tables from HTML
                extractChartBars(html)

                val (parsedSchedule, parsedDiarios) = parseScheduleFromHtml(document)
                if (parsedSchedule.isNotEmpty() || parsedDiarios.isNotEmpty()) {
                    saveScheduleOffline(parsedSchedule, parsedDiarios)
                }
                parseAndSyncProfessorsFromHtml(document, parsedDiarios)
                
                fetchScheduleFromSuap(_matricula.value)
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        try {
            val doc = org.jsoup.Jsoup.parse(html)
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
            e.printStackTrace()
        }
    }

    private fun extractClassAverages(html: String): Map<String, Double> {
        val classAvgMap = mutableMapOf<String, Double>()
        try {
            val doc = org.jsoup.Jsoup.parse(html)
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
            e.printStackTrace()
        }
        return classAvgMap
    }

    fun fetchScheduleFromSuap(matriculaVal: String = _matricula.value) {
        if (matriculaVal.isEmpty() || matriculaVal == "-") return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
            if (cookie == null) return@launch

            try {
                val url = "https://suap.ifal.edu.br/edu/aluno/$matriculaVal/?tab=locais_aula_aluno"
                android.util.Log.d("SUAP_SCHEDULE", "Fetching schedule from $url")
                val doc = org.jsoup.Jsoup.connect(url)
                    .header("Cookie", cookie)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .get()

                val (parsedSchedule, parsedDiarios) = parseScheduleFromHtml(doc)
                if (parsedSchedule.isNotEmpty() || parsedDiarios.isNotEmpty()) {
                    saveScheduleOffline(parsedSchedule, parsedDiarios)
                }
            } catch (e: Exception) {
                android.util.Log.e("SUAP_SCHEDULE", "Error fetching schedule", e)
            }
        }
    }

    fun fetchClassAveragesInBackground() {
        val matriculaVal = _matricula.value
        if (matriculaVal == "-" || matriculaVal.isEmpty()) return
        fetchScheduleFromSuap(matriculaVal)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
            android.util.Log.d("SUAP_AVG", "Cookie is null? ${cookie == null}")
            if (cookie == null) return@launch

            try {
                val etapaAvgMaps = mutableMapOf<Int, Map<String, Double>>()
                for (etapa in 1..4) {
                    try {
                        val url = "https://suap.ifal.edu.br/edu/aluno/$matriculaVal/?tab=boletim&etapa=$etapa"
                        android.util.Log.d("SUAP_AVG", "Fetching $url")
                        val doc = org.jsoup.Jsoup.connect(url)
                            .header("Cookie", cookie)
                            .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                            .get()
                        val html = doc.outerHtml()
                        val avgMap = extractClassAverages(html)
                        etapaAvgMaps[etapa] = avgMap
                        android.util.Log.d("SUAP_AVG", "Etapa $etapa avgMap size: ${avgMap.size}, keys: ${avgMap.keys}")
                    } catch (e: Exception) {
                        android.util.Log.e("SUAP_AVG", "Error fetching etapa $etapa", e)
                    }
                }
                
                val currentSubjects = repository.getAllSubjectsSync()
                val updatedList = currentSubjects.map { subject ->
                    var subjToUpdate = subject
                    for (etapa in 1..4) {
                        val avgMap = etapaAvgMaps[etapa] ?: continue
                        val possibleKeys = avgMap.keys.filter { cat -> subject.name.contains(cat, ignoreCase = true) }
                        val matchedKey = possibleKeys.maxByOrNull { it.length }
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
                updatedList.forEach { repository.updateSubject(it) }
                android.util.Log.d("SUAP_AVG", "Updated DB subjects with class averages. Sample: ${updatedList.firstOrNull()?.name} C1: ${updatedList.firstOrNull()?.classGrade1}")
            } catch (e: Exception) {
                android.util.Log.e("SUAP_AVG", "Error", e)
            }
        }
    }

    fun downloadBoletim(context: android.content.Context) {
        val imprimirUrl = _boletimPdfUrl.value
        if (imprimirUrl.isEmpty()) {
            android.widget.Toast.makeText(context, "URL do boletim não encontrada. Tente sincronizar novamente.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        android.widget.Toast.makeText(context, "Iniciando download...", android.widget.Toast.LENGTH_SHORT).show()
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: ""
                val client = okhttp3.OkHttpClient()
                var request = okhttp3.Request.Builder()
                    .url(imprimirUrl)
                    .header("Cookie", cookie)
                    .build()
                
                var response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Erro ao acessar boletim", android.widget.Toast.LENGTH_SHORT).show()
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
                        .header("Cookie", cookie)
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
                                android.widget.Toast.makeText(context, "Boletim salvo em Downloads!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Erro: Não foi possível obter o PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Erro ao baixar boletim", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
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
        }
    }
    
    fun toggleFavorite(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubject(subject.copy(isFavorite = !subject.isFavorite))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
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
            
            sharedPreferences.edit()
                .remove("saved_student_name")
                .remove("saved_full_name")
                .remove("saved_matricula")
                .remove("saved_ingresso")
                .remove("saved_curso")
                .remove("saved_global_frequency")
                .remove("saved_global_absences")
                .apply()
                
            // clear cookie manager so webview login is reset
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        }
    }


    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())
    val avisos: StateFlow<List<Aviso>> = _avisos

    private val _isLoadingAvisos = MutableStateFlow(false)
    val isLoadingAvisos: StateFlow<Boolean> = _isLoadingAvisos

    fun fetchAvisos() {
        

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingAvisos.value = true
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    val doc = org.jsoup.Jsoup.connect("https://suap.ifal.edu.br/admin/edu/mensagementrada/?tab=tab_todas")
                        .header("Cookie", cookie)
                        .get()
                    
                    val thead = doc.select("table thead th")
                    var senderIdx = -1
                    var subjectIdx = -1
                    var dateIdx = -1
                    var readIdx = -1
                    
                    thead.forEachIndexed { index, th ->
                        val text = th.text().lowercase()
                        if (text.contains("remetente")) senderIdx = index
                        if (text.contains("assunto")) subjectIdx = index
                        if (text.contains("data")) dateIdx = index
                        if (text.contains("lida") || text.contains("situação")) readIdx = index
                    }
                    
                    val rows = doc.select("table tbody tr")
                    val parsedAvisos = mutableListOf<Aviso>()
                    
                    for (row in rows) {
                        val cols = row.select("td, th")
                        val sender = if (senderIdx >= 0 && cols.size > senderIdx) cols[senderIdx].text().trim() else ""
                        val subject = if (subjectIdx >= 0 && cols.size > subjectIdx) cols[subjectIdx].text().trim() else ""
                        val date = if (dateIdx >= 0 && cols.size > dateIdx) cols[dateIdx].text().trim() else ""
                        
                        var isRead = false
                        if (readIdx >= 0 && cols.size > readIdx) {
                            val readCol = cols[readIdx]
                            val img = readCol.select("img").firstOrNull()
                            if (img != null) {
                                isRead = img.attr("alt").equals("True", ignoreCase = true)
                            } else {
                                isRead = readCol.text().contains("Sim", ignoreCase = true)
                            }
                        } else {
                            if (row.hasClass("nao_lida") || row.select("strong").isNotEmpty()) {
                                isRead = false
                            } else {
                                isRead = false
                            }
                        }
                        
                        val detailUrlNode = row.select("a[href*=/edu/mensagem/]").firstOrNull()
                        val detailUrl = detailUrlNode?.attr("href") ?: ""

                        if (sender.isNotEmpty() || subject.isNotEmpty()) {
                            parsedAvisos.add(Aviso(sender, subject, date, isRead, detailUrl))
                        }
                    }
                    _avisos.value = parsedAvisos.take(10)
                    if (parsedAvisos.isNotEmpty()) {
                        sharedPreferences.edit().putString("last_aviso_subject", parsedAvisos[0].subject).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingAvisos.value = false
            }
        }
    }

    private val _materials = MutableStateFlow<List<com.example.data.Material>>(emptyList())
    val materials: StateFlow<List<com.example.data.Material>> = _materials
    
    private val _isLoadingMaterials = MutableStateFlow(false)
    val isLoadingMaterials: StateFlow<Boolean> = _isLoadingMaterials
    
    private var fetchMaterialsJob: kotlinx.coroutines.Job? = null

    private val _selectedSubjectName = MutableStateFlow("")
    val selectedSubjectName: StateFlow<String> = _selectedSubjectName.asStateFlow()

    private val _selectedSubjectUrl = MutableStateFlow("")
    val selectedSubjectUrl: StateFlow<String> = _selectedSubjectUrl.asStateFlow()

    private fun matchSubjectUrl(targetName: String, urlMap: Map<String, String>): String? {
        if (urlMap.isEmpty() || targetName.isBlank()) return null
        
        // 1. Exact match with key
        urlMap.keys.find { it.equals(targetName, ignoreCase = true) }?.let { return urlMap[it] }

        val cleanTarget = targetName
            .replace(Regex("(?i)INT\\.\\d+"), "")
            .replace(Regex("(?i)SATAGO\\w+"), "")
            .replace(Regex("(?i)Médio|\\[.*?\\]"), "")
            .replace(Regex("[^a-zA-ZÀ-ÿ0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanTarget.isBlank()) return null

        // 2. Exact match with cleaned key
        for ((key, url) in urlMap) {
            val keyClean = key
                .replace(Regex("(?i)INT\\.\\d+"), "")
                .replace(Regex("(?i)SATAGO\\w+"), "")
                .replace(Regex("(?i)Médio|\\[.*?\\]"), "")
                .replace(Regex("[^a-zA-ZÀ-ÿ0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (keyClean.equals(cleanTarget, ignoreCase = true)) {
                return url
            }
        }

        // 3. Exact word list match
        val targetWords = cleanTarget.lowercase().split(" ").filter { it.length > 2 }
        for ((key, url) in urlMap) {
            val keyClean = key
                .replace(Regex("(?i)INT\\.\\d+"), "")
                .replace(Regex("(?i)SATAGO\\w+"), "")
                .replace(Regex("(?i)Médio|\\[.*?\\]"), "")
                .replace(Regex("[^a-zA-ZÀ-ÿ0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val keyWords = keyClean.lowercase().split(" ").filter { it.length > 2 }
            if (keyWords == targetWords && keyWords.isNotEmpty()) {
                return url
            }
        }

        // 4. Overlap scoring
        var bestUrl: String? = null
        var bestScore = -1.0
        for ((key, url) in urlMap) {
            val keyClean = key
                .replace(Regex("(?i)INT\\.\\d+"), "")
                .replace(Regex("(?i)SATAGO\\w+"), "")
                .replace(Regex("(?i)Médio|\\[.*?\\]"), "")
                .replace(Regex("[^a-zA-ZÀ-ÿ0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val keyWords = keyClean.lowercase().split(" ").filter { it.isNotBlank() }.toSet()
            val tWords = cleanTarget.lowercase().split(" ").filter { it.isNotBlank() }.toSet()
            val intersection = keyWords.intersect(tWords).size
            val union = keyWords.union(tWords).size
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
        if (_studentName.value == "Aluno") {
            _isLoadingMaterials.value = true
            _materials.value = listOf(
                com.example.data.Material("Aula 01 - Introdução e Conceitos Básicos", "/edu/registrar_acesso_aluno_material_aula/123/1/", "18 de Junho de 2026"),
                com.example.data.Material("Aula 02 - Fundamentos e Aplicações", "/edu/registrar_acesso_aluno_material_aula/123/2/", "27 de Maio de 2026"),
                com.example.data.Material("Aula 03 - Atividade Prática 1", "/edu/registrar_acesso_aluno_material_aula/123/3/", "10 de Maio de 2026")
            )
            _isLoadingMaterials.value = false
            return
        }
        
        val targetName = _selectedSubjectName.value
        android.util.Log.d("SUAP_MATERIAIS", "Fetching materials for subject: '$targetName', url: '$subjectUrl'")

        fetchMaterialsJob?.cancel()
        _materials.value = emptyList()
        _isLoadingMaterials.value = true

        fetchMaterialsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    var finalUrl = subjectUrl.trim()

                    // Fetch list of disciplines if finalUrl does not point to a specific /edu/disciplina/
                    if (!finalUrl.contains("/edu/disciplina/")) {
                        try {
                            val listDoc = org.jsoup.Jsoup.connect("https://suap.ifal.edu.br/edu/disciplinas/")
                                .header("Cookie", cookie)
                                .get()
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val urlToFetch = if (finalUrl.startsWith("http")) {
                        if (finalUrl.contains("?")) "$finalUrl&tab=materiais" else "$finalUrl?tab=materiais"
                    } else {
                        val cleanUrl = if (finalUrl.startsWith("/")) finalUrl else "/$finalUrl"
                        if (cleanUrl.contains("?")) "https://suap.ifal.edu.br$cleanUrl&tab=materiais" else "https://suap.ifal.edu.br$cleanUrl?tab=materiais"
                    }

                    android.util.Log.d("SUAP_MATERIAIS", "urlToFetch: $urlToFetch")
                    val doc = org.jsoup.Jsoup.connect(urlToFetch)
                        .header("Cookie", cookie)
                        .get()
                        
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
                    
                    android.util.Log.d("SUAP_MATERIAIS", "Found ${rows.size} rows")
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

                    android.util.Log.d("SUAP_MATERIAIS", "Parsed ${parsedMaterials.size} materials")
                    _materials.value = parsedMaterials
                } else {
                    android.util.Log.d("SUAP_MATERIAIS", "Cookie is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("SUAP_MATERIAIS", "Error fetching materials", e)
                e.printStackTrace()
                _materials.value = emptyList()
            } finally {
                _isLoadingMaterials.value = false
            }
        }
    }
    
    fun clearMaterials() {
        _materials.value = emptyList()
    }
    fun syncDisciplinasUrls() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    val doc = org.jsoup.Jsoup.connect("https://suap.ifal.edu.br/edu/disciplinas/")
                        .header("Cookie", cookie)
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
                        updatedSubjects.forEach { repository.updateSubject(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            } catch (e: Exception) { e.printStackTrace() }
        }

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
            } catch (e: Exception) { e.printStackTrace() }
        }

        if (scheduleList.isEmpty() && diarioList.isEmpty()) {
            val demoData = getDemoScheduleAndDiarios()
            _scheduleItems.value = demoData.first
            _diarioItems.value = demoData.second
            saveScheduleOffline(demoData.first, demoData.second)
        } else {
            _scheduleItems.value = scheduleList
            _diarioItems.value = diarioList
        }
    }

    fun saveScheduleOffline(schedule: List<ScheduleItem>, diarios: List<DiarioItem>) {
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
            .apply()

        _scheduleItems.value = schedule
        _diarioItems.value = diarios
    }

    fun loadProfessorContactsOffline() {
        val savedJson = sharedPreferences.getString("offline_professors_json", "") ?: ""
        if (savedJson.isNotEmpty()) {
            try {
                val list = mutableListOf<ProfessorContact>()
                val arr = JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ProfessorContact(
                            name = obj.optString("name"),
                            email = obj.optString("email"),
                            discipline = obj.optString("discipline"),
                            photoUrl = obj.optString("photoUrl"),
                            matricula = obj.optString("matricula")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _professorContacts.value = list
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun parseAndSyncProfessorsFromHtml(document: org.jsoup.nodes.Document, diarioList: List<DiarioItem> = emptyList()) {
        val currentList = _professorContacts.value
        val contactMap = currentList.associateBy { it.name.lowercase().trim() }.toMutableMap()

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
                        val key = fullName.lowercase().trim()
                        val existing = contactMap[key]
                        contactMap[key] = ProfessorContact(
                            name = fullName,
                            email = existing?.email ?: "${shortName.lowercase().replace(" ", ".")}@ifal.edu.br",
                            discipline = if (disc.isNotEmpty()) disc else existing?.discipline ?: "",
                            photoUrl = if (imgUrl.isNotEmpty()) imgUrl else existing?.photoUrl ?: "",
                            matricula = existing?.matricula ?: ""
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
                var email = ""
                val dds = card.select("dd")
                for (dd in dds) {
                    val t = dd.text().trim()
                    if (t.contains("@")) {
                        email = t
                        break
                    }
                }
                val matricula = card.select("dt:contains(Matrícula) + dd, dt:contains(Matricula) + dd").text().trim()

                if (name.isNotEmpty()) {
                    val names = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (tName in names) {
                        val key = tName.lowercase().trim()
                        val existing = contactMap[key]
                        contactMap[key] = ProfessorContact(
                            name = tName,
                            email = if (email.isNotEmpty()) email else existing?.email ?: "${tName.lowercase().replace(" ", ".")}@ifal.edu.br",
                            discipline = existing?.discipline ?: "",
                            photoUrl = if (imgUrl.isNotEmpty()) imgUrl else existing?.photoUrl ?: "",
                            matricula = if (matricula.isNotEmpty()) matricula else existing?.matricula ?: ""
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
                        val key = tName.lowercase().trim()
                        val existing = contactMap[key]
                        val disc = diario.component.substringAfter(" - ").substringBefore(" - ").trim().ifEmpty { diario.component }
                        if (existing == null) {
                            contactMap[key] = ProfessorContact(
                                name = tName,
                                email = "${tName.lowercase().replace(" ", ".")}@ifal.edu.br",
                                discipline = disc,
                                photoUrl = "",
                                matricula = ""
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            e.printStackTrace()
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

    private val _subjectDetails = MutableStateFlow<Map<String, List<EtapaDetail>>>(emptyMap())
    val subjectDetails: StateFlow<Map<String, List<EtapaDetail>>> = _subjectDetails.asStateFlow()

    fun fetchSubjectDetails(subject: Subject) {
        val subjectKey = subject.name
        if (_subjectDetails.value[subjectKey]?.isNotEmpty() == true) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val urlToFetch = if (subject.detailUrl.isNotEmpty()) {
                if (subject.detailUrl.startsWith("http")) subject.detailUrl else "https://suap.ifal.edu.br${subject.detailUrl}"
            } else if (subject.url.isNotEmpty()) {
                if (subject.url.startsWith("http")) subject.url else "https://suap.ifal.edu.br${subject.url}"
            } else ""

            if (urlToFetch.isNotEmpty()) {
                try {
                    val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                    val conn = org.jsoup.Jsoup.connect(urlToFetch)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                    if (!cookie.isNullOrEmpty()) {
                        conn.header("Cookie", cookie)
                    }
                    val doc = conn.get()
                    val parsedEtapas = parseEtapaDetailsFromDocument(doc)
                    if (parsedEtapas.isNotEmpty()) {
                        _subjectDetails.value = _subjectDetails.value + (subjectKey to parsedEtapas)
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback or demo data
            val fallbackEtapas = generateFallbackEtapas(subject)
            _subjectDetails.value = _subjectDetails.value + (subjectKey to fallbackEtapas)
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

    private fun generateFallbackEtapas(subject: Subject): List<EtapaDetail> {
        fun parseVal(s: String): Double? = s.replace(",", ".").toDoubleOrNull()
        val g1 = parseVal(subject.grade1)
        val g2 = parseVal(subject.grade2)
        val g3 = parseVal(subject.grade3)
        val g4 = parseVal(subject.grade4)

        fun fmt(d: Double?): String = d?.let { String.format(java.util.Locale.US, "%.2f", it).replace(".", ",") } ?: "-"

        val etapa1Items = listOf(
            GradeDetailRow("A1", "Prova", "Prova mensal 1", "-", fmt(g1)),
            GradeDetailRow("A2", "Prova", "Prova bimestral 1", "-", fmt(g1)),
            GradeDetailRow("REC", "Recuperação", "Recuperação", "-", subject.rec1.ifEmpty { "-" })
        )
        val etapa2Items = listOf(
            GradeDetailRow("A1", "Prova", "Avaliação mensal 2", "-", fmt(g2)),
            GradeDetailRow("A2", "Prova", "Avaliação bimestral 2", "-", fmt(g2)),
            GradeDetailRow("REC", "Recuperação", "Recuperação", "-", subject.rec2.ifEmpty { "-" })
        )
        val etapa3Items = listOf(
            GradeDetailRow("A1", "Prova", "Prova mensal 3", "-", fmt(g3)),
            GradeDetailRow("A2", "Prova", "Prova bimestral 3", "-", fmt(g3)),
            GradeDetailRow("REC", "Recuperação", "Recuperação", "-", "-")
        )
        val etapa4Items = listOf(
            GradeDetailRow("A1", "Prova", "Prova mensal 4", "-", fmt(g4)),
            GradeDetailRow("A2", "Prova", "Prova bimestral 4", "-", fmt(g4)),
            GradeDetailRow("REC", "Recuperação", "Recuperação", "-", "-")
        )
        val etapaFinalItems = listOf(
            GradeDetailRow("A1", "Prova", "Prova Final", "-", subject.recFinal.ifEmpty { "-" })
        )

        return listOf(
            EtapaDetail("Etapa 1 - Média Aritmética", etapa1Items),
            EtapaDetail("Etapa 2 - Média Aritmética", etapa2Items),
            EtapaDetail("Etapa 3 - Média Aritmética", etapa3Items),
            EtapaDetail("Etapa 4 - Média Aritmética", etapa4Items),
            EtapaDetail("Etapa Final - Média Aritmética", etapaFinalItems)
        )
    }

    suspend fun getMessageDetail(urlPath: String): com.example.data.MessageDetail {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br")
                if (cookie != null) {
                    val fullUrl = if (urlPath.startsWith("http")) urlPath else "https://suap.ifal.edu.br$urlPath"
                    val doc = org.jsoup.Jsoup.connect(fullUrl).header("Cookie", cookie).get()
                    
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

                    return@withContext com.example.data.MessageDetail(textContent, attachments)
                }
                return@withContext com.example.data.MessageDetail("Cookie não encontrado.")
            } catch (e: Exception) {
                return@withContext com.example.data.MessageDetail("Erro ao carregar mensagem: ${e.message}")
            }
        }
    }}
