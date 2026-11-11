package com.example.util

import com.example.data.AulaHistory
import com.example.data.AulaLink
import com.example.data.Aviso
import com.example.data.EvaluationItem
import com.example.data.Material
import com.example.data.Subject
import com.example.ui.AcademicEvent
import com.example.ui.DiarioItem
import com.example.ui.ScheduleItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object SuapParser {

    fun findDisciplinaUrl(html: String): ParserResult<String> {
        if (html.isBlank()) {
            return ParserResult.Error("HTML fornecido está vazio")
        }
        return try {
            val document = Jsoup.parse(html)
            val link = document.select("a[href*=/edu/disciplina/]").firstOrNull()
            val href = link?.attr("href")
            if (!href.isNullOrBlank()) {
                ParserResult.Success(href)
            } else {
                ParserResult.Error("Nenhum link de disciplina (/edu/disciplina/) encontrado")
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar URL da disciplina: ${e.message}", e)
        }
    }

    fun parseAulas(html: String): ParserResult<List<AulaHistory>> {
        if (html.isBlank()) {
            return ParserResult.Error("HTML de aulas está vazio")
        }
        val aulas = mutableListOf<AulaHistory>()
        return try {
            val document = Jsoup.parse(html)

            val rows = document.select("div[data-tab=aulas] table tbody tr").ifEmpty {
                document.select("div[data-tab=aulas] table tr").ifEmpty {
                    document.select("table:has(th:contains(Aulas)) tbody tr").ifEmpty {
                        document.select("table:has(th:contains(Conteúdo)) tbody tr")
                    }
                }
            }

            if (rows.isEmpty()) {
                return ParserResult.Error("Tabela de aulas/conteúdos não encontrada no HTML")
            }

            val thead = document.select("div[data-tab=aulas] table thead th").ifEmpty {
                document.select("table:has(th:contains(Aulas)) thead th")
            }

            var dataIdx = 0
            var etapaIdx = 1
            var numAulasIdx = 2
            var numFaltasIdx = 3
            var conteudoIdx = 4
            var profIdx = 5

            if (thead.isNotEmpty()) {
                thead.forEachIndexed { index, th ->
                    val text = th.text().lowercase()
                    when {
                        text.contains("data") -> dataIdx = index
                        text.contains("etapa") -> etapaIdx = index
                        text.contains("aula") && !text.contains("conte") -> numAulasIdx = index
                        text.contains("falta") -> numFaltasIdx = index
                        text.contains("conte") -> conteudoIdx = index
                        text.contains("prof") -> profIdx = index
                    }
                }
            }

            for (row in rows) {
                val cols = row.select("td")
                if (cols.size >= 4) {
                    val data = cols.getOrNull(dataIdx)?.text().orEmpty().trim()
                    val etapa = if (etapaIdx in cols.indices) cols[etapaIdx].text().trim() else ""
                    val numAulas = cols.getOrNull(numAulasIdx)?.text().orEmpty().trim()
                    val numFaltas = cols.getOrNull(numFaltasIdx)?.text().orEmpty().trim()
                    val conteudoCol = cols.getOrNull(conteudoIdx)
                    val conteudo = conteudoCol?.text().orEmpty().trim()
                    val professor = if (profIdx in cols.indices) cols[profIdx].text().trim() else ""

                    val parsedLinks = mutableListOf<AulaLink>()
                    conteudoCol?.select("a[href]")?.forEach { linkElem ->
                        val href = linkElem.attr("href").trim()
                        val text = linkElem.text().trim().ifEmpty { href }
                        if (href.isNotBlank()) {
                            parsedLinks.add(AulaLink(title = text, url = href))
                        }
                    }

                    val attachLink = conteudoCol?.select("a[href]")?.firstOrNull()
                    val attachmentUrl = attachLink?.attr("href").orEmpty().trim()
                    val attachmentTitle = attachLink?.text().orEmpty().trim()

                    if (data.isNotEmpty() || conteudo.isNotEmpty()) {
                        aulas.add(
                            AulaHistory(
                                data = data,
                                etapa = etapa,
                                numAulas = numAulas,
                                numFaltas = numFaltas,
                                conteudo = conteudo,
                                professor = professor,
                                attachmentUrl = attachmentUrl,
                                attachmentTitle = attachmentTitle,
                                links = parsedLinks
                            )
                        )
                    }
                }
            }

            if (aulas.isEmpty()) {
                ParserResult.Error("Nenhuma aula pôde ser extraída das linhas da tabela")
            } else {
                ParserResult.Success(aulas)
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar aulas: ${e.message}", e)
        }
    }

    fun parseMaterials(html: String): ParserResult<List<Material>> {
        if (html.isBlank()) {
            return ParserResult.Error("HTML de materiais está vazio")
        }
        val materials = mutableListOf<Material>()
        return try {
            val doc = Jsoup.parse(html)
            val fullDateRegex = Regex("""(?i)\b\d{1,2}\s+de\s+[a-zá-úçãéíóú]+\s+de\s+\d{2,4}\b|\b\d{1,2}\s+de\s+[a-zá-úçãéíóú]+\b|\b\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?\b|\b\d{4}-\d{2}-\d{2}\b""")

            val allRows = doc.select("table tbody tr, table tr")

            for (row in allRows) {
                if (row.select("th").isNotEmpty() && row.select("td").isEmpty()) continue
                val cols = row.select("td")
                if (cols.isEmpty()) continue

                val links = row.select("a[href]")
                if (links.isEmpty()) continue

                var foundDate = ""
                for (col in cols) {
                    val colText = col.text().trim()
                    val match = fullDateRegex.find(colText)
                    if (match != null) {
                        foundDate = match.value
                        break
                    }
                }

                if (foundDate.isEmpty() && cols.size >= 3) {
                    val col3 = cols[2].text().trim()
                    if (col3.length in 4..30 && !col3.equals("Visualizar", ignoreCase = true) && !col3.equals("Download", ignoreCase = true)) {
                        foundDate = col3
                    }
                }

                var rowDescription = ""
                for (col in cols) {
                    val txt = col.text().trim()
                    if (txt.isNotBlank() &&
                        !txt.equals("Visualizar", ignoreCase = true) &&
                        !txt.equals("Download", ignoreCase = true) &&
                        !txt.equals("Baixar", ignoreCase = true) &&
                        !txt.equals("Arquivo", ignoreCase = true) &&
                        !fullDateRegex.matches(txt) &&
                        !txt.matches(Regex("""^\d+$""")) &&
                        txt.length > rowDescription.length
                    ) {
                        rowDescription = txt
                    }
                }

                for (link in links) {
                    var href = link.attr("href").trim()
                    if (href.isBlank() || href.startsWith("#") || href.startsWith("javascript:")) continue

                    if (href.startsWith("/")) {
                        href = "https://suap.ifal.edu.br$href"
                    }

                    val linkText = link.text().trim()
                    val linkTitleAttr = link.attr("title").trim()

                    val title = when {
                        linkText.isNotBlank() &&
                        !linkText.equals("Visualizar", ignoreCase = true) &&
                        !linkText.equals("Download", ignoreCase = true) &&
                        !linkText.equals("Baixar", ignoreCase = true) &&
                        !linkText.equals("Arquivo", ignoreCase = true) -> linkText
                        rowDescription.isNotBlank() -> rowDescription
                        linkTitleAttr.isNotBlank() -> linkTitleAttr
                        else -> "Material da Disciplina"
                    }

                    var date = foundDate
                    if (date.isEmpty()) {
                        val matchTitle = fullDateRegex.find(linkTitleAttr)
                        if (matchTitle != null) {
                            date = matchTitle.value
                        } else {
                            val matchHref = fullDateRegex.find(href)
                            if (matchHref != null) date = matchHref.value
                        }
                    }

                    val isMaterialLink = isLikelyMaterialUrl(href) || cols.size >= 2
                    if (isMaterialLink && materials.none { it.url == href }) {
                        materials.add(Material(title = title, url = href, date = date))
                    }
                }
            }

            // Fallback: check any file links in document
            val allLinks = doc.select(
                "a[href*=/registrar_acesso_aluno_material_aula/], " +
                "a[href*=/media/edu/material/], " +
                "a[href*=/media/materiais/], " +
                "a[href*=/material_aula/], " +
                "a[href*=/material/], " +
                "a[href*=/download/], " +
                "a[href$=.pdf], a[href$=.docx], a[href$=.pptx], a[href$=.zip], a[href$=.rar]"
            )
            for (link in allLinks) {
                var href = link.attr("href").trim()
                if (href.isBlank()) continue
                if (href.startsWith("/")) {
                    href = "https://suap.ifal.edu.br$href"
                }

                if (materials.none { it.url == href }) {
                    val linkText = link.text().trim()
                    val title = if (linkText.isNotBlank() &&
                        !linkText.equals("Visualizar", true) &&
                        !linkText.equals("Download", true) &&
                        !linkText.equals("Baixar", true)
                    ) {
                        linkText
                    } else {
                        link.attr("title").trim().ifEmpty { "Material em PDF / Arquivo" }
                    }
                    val dateMatch = fullDateRegex.find(link.parent()?.text().orEmpty()) ?: fullDateRegex.find(href)
                    val date = dateMatch?.value.orEmpty()
                    materials.add(Material(title = title, url = href, date = date))
                }
            }

            ParserResult.Success(materials)
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar materiais: ${e.message}", e)
        }
    }

    private fun isLikelyMaterialUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("/material") ||
                lower.contains("/media/") ||
                lower.contains("/download") ||
                lower.contains("/arquivo") ||
                lower.contains("/documento") ||
                lower.contains("/registrar_acesso") ||
                lower.endsWith(".pdf") ||
                lower.endsWith(".zip") ||
                lower.endsWith(".docx") ||
                lower.endsWith(".pptx")
    }

    fun parseAvisos(doc: Document): ParserResult<List<Aviso>> {
        return try {
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
            if (rows.isEmpty()) {
                return if (senderIdx >= 0 && subjectIdx >= 0) {
                    ParserResult.Success(emptyList())
                } else {
                    ParserResult.Error("Tabela de avisos/mensagens vazia ou não encontrada")
                }
            }

            val parsedAvisos = mutableListOf<Aviso>()

            for (row in rows) {
                val cols = row.select("td, th")
                val sender = if (senderIdx >= 0 && cols.size > senderIdx) cols[senderIdx].text().trim() else ""
                val subject = if (subjectIdx >= 0 && cols.size > subjectIdx) cols[subjectIdx].text().trim() else ""
                val date = if (dateIdx >= 0 && cols.size > dateIdx) cols[dateIdx].text().trim() else ""
                val isRead = if (readIdx >= 0 && cols.size > readIdx) {
                    val t = cols[readIdx].text().lowercase()
                    t.contains("sim") || t.contains("lida")
                } else false

                val detailLink = row.select("a[href*=/admin/edu/mensagem/]").first()?.attr("href") ?: ""

                if (sender.isNotEmpty() && subject.isNotEmpty()) {
                    parsedAvisos.add(Aviso(sender, subject, date, isRead, detailLink))
                }
            }

            if (parsedAvisos.isEmpty()) {
                if (senderIdx >= 0 && subjectIdx >= 0) {
                    ParserResult.Success(emptyList())
                } else {
                    ParserResult.Error("Nenhum aviso com remetente e assunto pôde ser identificado")
                }
            } else {
                ParserResult.Success(parsedAvisos)
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar avisos: ${e.message}", e)
        }
    }

    fun parseSubjects(doc: Document): ParserResult<List<Subject>> {
        return try {
            val tables = doc.select("table").filter { table ->
                val headers = table.select("thead th, tr:first-child th").text().lowercase(java.util.Locale.ROOT)
                (headers.contains("disciplina") || headers.contains("componente")) &&
                    (headers.contains("falta") || headers.contains("situa"))
            }
            val rows = tables.flatMap { table ->
                table.select("tbody tr").filter { it.closest("table") == table }
            }
            if (rows.isEmpty()) {
                return ParserResult.Error("Tabela de boletim não encontrada ou vazia no HTML")
            }

            val subjects = mutableListOf<Subject>()

            for (row in rows) {
                val cols = row.select("td")
                if (cols.size < 8) continue

                var name = cols.getOrNull(1)?.text()?.trim().orEmpty()
                if (name.isEmpty()) continue
                if (name.startsWith("INT")) {
                    name = name.substringAfter(" - ").trim()
                }

                val url = cols.getOrNull(1)?.select("a")?.attr("href") ?: ""
                val absences = cols.getOrNull(4)?.text()?.trim() ?: "0"
                val status = cols.getOrNull(6)?.text()?.trim() ?: ""
                val grade1 = cols.getOrNull(7)?.text()?.trim() ?: ""
                val grade2 = cols.getOrNull(9)?.text()?.trim() ?: ""
                val grade3 = if (cols.size > 11) cols[11].text().trim() else ""
                val grade4 = if (cols.size > 13) cols[13].text().trim() else ""
                val finalGrade = if (cols.size > 15) cols[15].text().trim() else ""

                subjects.add(
                    Subject(
                        id = Subject.generateStableId(name, url),
                        name = name,
                        url = url,
                        grade1 = grade1,
                        grade2 = grade2,
                        rec1 = "",
                        grade3 = grade3,
                        grade4 = grade4,
                        rec2 = "",
                        recFinal = "",
                        finalGrade = finalGrade,
                        absences = absences,
                        status = status,
                        detailUrl = row.selectFirst("a.popup[href]")?.attr("href").orEmpty()
                    )
                )
            }

            if (subjects.isEmpty()) {
                ParserResult.Error("Nenhuma disciplina válida pôde ser extraída da tabela do boletim")
            } else {
                ParserResult.Success(subjects)
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar disciplinas do boletim: ${e.message}", e)
        }
    }

    fun parseSchedule(document: Document): ParserResult<Pair<List<ScheduleItem>, List<DiarioItem>>> {
        val scheduleList = mutableListOf<ScheduleItem>()
        val diarioList = mutableListOf<DiarioItem>()

        return try {
            val tables = document.select(".calendario table, #horarios table, table:has(th:contains(Segunda))")
            if (tables.isEmpty() && document.select("#diarios table").isEmpty()) {
                return ParserResult.Error("Tabela de horários e diários não encontrada no documento")
            }

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
                        rowText.contains("vespertino") || rowText.contains("noturno")
                    ) continue

                    val timeSlot = cols.first()?.text()?.trim() ?: ""
                    if (timeSlot.isEmpty() || timeSlot.length < 3 || !timeSlot.contains("-")) continue

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

            if (scheduleList.isEmpty() && diarioList.isEmpty()) {
                ParserResult.Error("Nenhum horário ou diário encontrado na grade")
            } else {
                ParserResult.Success(Pair(scheduleList, diarioList))
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            ParserResult.Error("Falha ao analisar horários: ${e.message}", e)
        }
    }

    fun parseEvaluations(document: Document): List<EvaluationItem> {
        val list = mutableListOf<EvaluationItem>()
        try {
            val dateRegex = Regex("""(\d{1,2}/\d{1,2}(?:/\d{2,4})?)""")

            // 1. Scan tables
            val tables = document.select("table")
            for (table in tables) {
                val headers = table.select("thead th, tr:first-child th").map { it.text().trim().lowercase() }
                val dateCol = headers.indexOfFirst { it.contains("data") || it.contains("dia") || it.contains("quando") || it.contains("período") || it.contains("periodo") }
                val subjectCol = headers.indexOfFirst { it.contains("disciplina") || it.contains("componente") || it.contains("diário") || it.contains("diario") || it.contains("turma") || it.contains("matéria") }
                val titleCol = headers.indexOfFirst { it.contains("avaliação") || it.contains("avaliacao") || it.contains("descrição") || it.contains("descricao") || it.contains("título") || it.contains("titulo") || it.contains("sigla") || it.contains("tipo") }
                val detailCol = headers.indexOfFirst { it.contains("horário") || it.contains("horario") || it.contains("hora") || it.contains("peso") || it.contains("etapa") || it.contains("local") }

                val rows = table.select("tbody tr, tr:not(:first-child)")
                for (row in rows) {
                    val cols = row.select("td")
                    if (cols.isEmpty()) continue

                    if (dateCol >= 0 && dateCol < cols.size) {
                        val rawDate = cols[dateCol].text().trim()
                        val dateMatch = dateRegex.find(rawDate)?.value ?: rawDate
                        val subject = if (subjectCol >= 0 && subjectCol < cols.size) cols[subjectCol].text().trim() else ""
                        val title = if (titleCol >= 0 && titleCol < cols.size) cols[titleCol].text().trim() else ""
                        val detail = if (detailCol >= 0 && detailCol < cols.size) cols[detailCol].text().trim() else ""

                        if (dateMatch.isNotBlank() && (subject.isNotBlank() || title.isNotBlank())) {
                            list.add(
                                EvaluationItem(
                                    id = "eval_${list.size}_${dateMatch.replace("/", "_")}",
                                    subjectName = subject.ifBlank { "Avaliação" },
                                    title = title.ifBlank { "Avaliação agendada" },
                                    date = dateMatch,
                                    timeOrDetail = detail
                                )
                            )
                        }
                    } else {
                        // Scan cells for date regex
                        var foundDate = ""
                        val cellTexts = cols.map { it.text().trim() }
                        for (text in cellTexts) {
                            val match = dateRegex.find(text)
                            if (match != null) {
                                foundDate = match.value
                                break
                            }
                        }
                        if (foundDate.isNotBlank() && cellTexts.size >= 2) {
                            val otherTexts = cellTexts.filter { !it.contains(foundDate) && it.isNotBlank() }
                            val sub = otherTexts.firstOrNull() ?: "Avaliação"
                            val desc = otherTexts.getOrNull(1) ?: "Avaliação SUAP"
                            list.add(
                                EvaluationItem(
                                    id = "eval_${list.size}_${foundDate.replace("/", "_")}",
                                    subjectName = sub,
                                    title = desc,
                                    date = foundDate,
                                    timeOrDetail = otherTexts.getOrNull(2) ?: ""
                                )
                            )
                        }
                    }
                }
            }

            // 2. Scan calendar items / evaluation boxes
            val calItems = document.select(".calendario-item, .item-avaliacao, li:has(a[href*=/edu/]), .evento-avaliacao")
            for (item in calItems) {
                val text = item.text().trim()
                val dateMatch = dateRegex.find(text)?.value
                if (dateMatch != null) {
                    val remaining = text.replace(dateMatch, "").trim(' ', '-', '•', ':')
                    val parts = remaining.split("-", "–", "•", "|").map { it.trim() }.filter { it.isNotEmpty() }
                    val subject = parts.firstOrNull() ?: "Avaliação"
                    val title = parts.getOrNull(1) ?: remaining
                    list.add(
                        EvaluationItem(
                            id = "eval_item_${list.size}",
                            subjectName = subject,
                            title = title,
                            date = dateMatch,
                            timeOrDetail = parts.getOrNull(2) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e("SuapParser", "Error parsing evaluations", e)
        }
        return list
    }

    fun parseAcademicCalendar(document: Document, defaultYear: Int = java.time.LocalDate.now().year): List<AcademicEvent> {
        val events = mutableListOf<AcademicEvent>()
        try {
            val titleText = document.select("h1, h2, h3, caption, .titulo").text()
            val yearMatch = Regex("""20\d{2}""").find(titleText)
            val currentYear = yearMatch?.value?.toIntOrNull() ?: defaultYear

            // 1. Scan tables (Datas Comemorativas, Feriados, Sábados Letivos, Prazos)
            val tables = document.select("table")
            for (table in tables) {
                val headers = table.select("thead th, tr:first-child th").map { it.text().trim().lowercase() }
                
                val descIdx = headers.indexOfFirst { it.contains("descrição") || it.contains("descricao") || it.contains("evento") || it.contains("comemoração") || it.contains("atividade") }
                val startIdx = headers.indexOfFirst { it.contains("início") || it.contains("inicio") || it.contains("data") || it.contains("quando") }
                val endIdx = headers.indexOfFirst { it.contains("fim") || it.contains("término") || it.contains("termino") }
                val isSabadoTable = headers.any { it.contains("sábado") || it.contains("sabado") } && headers.any { it.contains("semana") }

                val rows = table.select("tbody tr, tr:not(:first-child)")
                for (row in rows) {
                    val cols = row.select("td")
                    if (cols.size < 2) continue

                    if (isSabadoTable) {
                        val dateStr = cols.getOrNull(1)?.text()?.trim() ?: cols[0].text().trim()
                        val diaSemana = cols.getOrNull(2)?.text()?.trim() ?: cols.getOrNull(1)?.text()?.trim() ?: ""
                        val parsedDate = parseDateFromStr(dateStr, currentYear)
                        if (parsedDate != null) {
                            events.add(
                                AcademicEvent(
                                    dateInfo = dateStr,
                                    description = if (diaSemana.isNotBlank()) "Sábado Letivo ($diaSemana)" else "Sábado Letivo",
                                    color = "#FFF9C4",
                                    startDate = parsedDate,
                                    endDate = parsedDate
                                )
                            )
                        }
                    } else if (descIdx >= 0 && startIdx >= 0) {
                        val desc = if (descIdx < cols.size) cols[descIdx].text().trim() else ""
                        val startStr = if (startIdx < cols.size) cols[startIdx].text().trim() else ""
                        val endStr = if (endIdx >= 0 && endIdx < cols.size) cols[endIdx].text().trim() else startStr
                        
                        val startDate = parseDateFromStr(startStr, currentYear)
                        val endDate = parseDateFromStr(endStr, currentYear) ?: startDate
                        
                        if (desc.isNotBlank() && startDate != null) {
                            val isFeriado = desc.contains("feriado", ignoreCase = true) || desc.contains("recesso", ignoreCase = true)
                            events.add(
                                AcademicEvent(
                                    dateInfo = if (startStr == endStr || endStr.isBlank()) startStr else "$startStr a $endStr",
                                    description = desc,
                                    color = if (isFeriado) "#FFCDD2" else "#B3E5FC",
                                    startDate = startDate,
                                    endDate = endDate
                                )
                            )
                        }
                    }
                }
            }

            // 2. Scan div.calendario and monthly grids
            val monthNames = listOf("janeiro", "fevereiro", "março", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
            val divs = document.select("div.calendario, .calendario-mes, .mes-calendario, div:has(table.calendario)")
            for (div in divs) {
                val caption = div.select("caption, h3, h4, .mes-titulo").text().trim().lowercase()
                val monthIndex = monthNames.indexOfFirst { caption.contains(it) } + 1
                
                val items = div.select("ul.legenda li, .legenda li, .legenda tr")
                for (li in items) {
                    li.select("span.visually-hidden").remove()
                    val cleanText = li.text().trim()
                    val parts = cleanText.split(":", limit = 2)
                    if (parts.size == 2) {
                        val dateInfo = parts[0].trim()
                        val desc = parts[1].trim()
                        
                        if (events.any { it.description.equals(desc, ignoreCase = true) }) continue
                        
                        var startDate: java.time.LocalDate? = null
                        var endDate: java.time.LocalDate? = null
                        
                        try {
                            val lowerDate = dateInfo.lowercase()
                            val rangeMatch = Regex("""(?:de\s*)?(\d{1,2})(?:/(\d{1,2}))?\s*(?:a|à|-)\s*(\d{1,2})(?:/(\d{1,2}))?""").find(lowerDate)
                            val singleMatch = Regex("""(?:dia\s*)?(\d{1,2})(?:/(\d{1,2}))?""").find(lowerDate)
                            
                            if (rangeMatch != null) {
                                val sDay = rangeMatch.groupValues[1].toInt()
                                val sMonth = rangeMatch.groupValues[2].toIntOrNull() ?: if (monthIndex in 1..12) monthIndex else 1
                                val eDay = rangeMatch.groupValues[3].toInt()
                                val eMonth = rangeMatch.groupValues[4].toIntOrNull() ?: sMonth
                                startDate = java.time.LocalDate.of(currentYear, sMonth, sDay)
                                endDate = java.time.LocalDate.of(currentYear, eMonth, eDay)
                            } else if (singleMatch != null) {
                                val sDay = singleMatch.groupValues[1].toInt()
                                val sMonth = singleMatch.groupValues[2].toIntOrNull() ?: if (monthIndex in 1..12) monthIndex else 1
                                startDate = java.time.LocalDate.of(currentYear, sMonth, sDay)
                                endDate = startDate
                            }
                        } catch (e: Exception) {
                            AppLog.e("SuapParser", "Error parsing calendar legend date: $dateInfo", e)
                        }
                        
                        var color = "#E0E0E0"
                        if (li.hasClass("extra")) color = "#D1C4E9"
                        else if (li.hasClass("extra2")) color = "#C8E6C9"
                        else if (li.hasClass("extra3")) color = "#FFF9C4"
                        else if (li.hasClass("danger")) color = "#FFCDD2"
                        else if (li.hasClass("info")) color = "#B3E5FC"
                        else if (li.hasClass("extra7")) color = "#FFCC80"
                        else if (li.hasClass("extra4")) color = "#FFCDD2"
                        else if (li.hasClass("extra5")) color = "#FFF9C4"
                        
                        events.add(
                            AcademicEvent(
                                dateInfo = dateInfo,
                                description = desc,
                                color = color,
                                startDate = startDate,
                                endDate = endDate
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.e("SuapParser", "Error parsing academic calendar", e)
        }
        return events
    }

    private fun parseDateFromStr(str: String, currentYear: Int): java.time.LocalDate? {
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
}
