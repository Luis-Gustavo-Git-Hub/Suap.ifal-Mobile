package com.example.util

import com.example.data.Aviso
import com.example.data.Subject
import org.jsoup.nodes.Document

object SuapParser {
    fun parseAvisos(doc: Document): List<Aviso> {
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
            val isRead = if (readIdx >= 0 && cols.size > readIdx) {
                val t = cols[readIdx].text().lowercase()
                t.contains("sim") || t.contains("lida")
            } else false

            val detailLink = row.select("a[href*=/admin/edu/mensagem/]").first()?.attr("href") ?: ""
            
            if (sender.isNotEmpty() && subject.isNotEmpty()) {
                parsedAvisos.add(Aviso(sender, subject, date, isRead, detailLink))
            }
        }
        return parsedAvisos
    }

    fun parseSubjects(doc: Document): List<Subject> {
        val rows = doc.select("table.bordered tbody tr, table.table-bordered tbody tr")
        val subjects = mutableListOf<Subject>()
        
        for (row in rows) {
            val cols = row.select("td")
            if (cols.size < 15) continue
            
            var name = cols[1].text().trim()
            if (name.isEmpty()) continue
            if (name.startsWith("INT")) {
                name = name.substringAfter(" - ").trim()
            }
            
            val url = cols[1].select("a").attr("href") ?: ""
            val absences = cols[4].text().trim()
            val status = cols[6].text().trim()
            val grade1 = cols[7].text().trim()
            val grade2 = cols[9].text().trim()
            val grade3 = if (cols.size > 11) cols[11].text().trim() else ""
            val grade4 = if (cols.size > 13) cols[13].text().trim() else ""
            val finalGrade = if (cols.size > 15) cols[15].text().trim() else ""

            subjects.add(Subject(
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
                status = status
            ))
        }
        return subjects
    }
}
