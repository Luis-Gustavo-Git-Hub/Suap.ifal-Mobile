package com.example.util

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class SuapParserTest {

    private fun loadFixture(filename: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(filename)
            ?: throw IllegalArgumentException("Fixture $filename not found in test resources")
        return InputStreamReader(inputStream).readText()
    }

    @Test
    fun `parseSubjects should parse boletim HTML fixture correctly`() {
        val html = loadFixture("boletim.html")
        val doc = Jsoup.parse(html)
        val result = SuapParser.parseSubjects(doc)

        assertTrue("Expected Success result but got $result", result is ParserResult.Success)
        val subjects = (result as ParserResult.Success).data
        assertEquals(3, subjects.size)

        // Subject 1: INT - MATEMÁTICA I -> MATEMÁTICA I
        val sub1 = subjects[0]
        assertEquals("MATEMÁTICA I", sub1.name)
        assertEquals("/edu/disciplina/101/", sub1.url)
        assertEquals("/edu/disciplina/101/", sub1.id)
        assertEquals("8.5", sub1.grade1)
        assertEquals("7.0", sub1.grade2)
        assertEquals("9.0", sub1.grade3)
        assertEquals("-", sub1.grade4)
        assertEquals("8.2", sub1.finalGrade)
        assertEquals("4", sub1.absences)
        assertEquals("Cursando", sub1.status)

        // Subject 2: FÍSICA GERAL
        val sub2 = subjects[1]
        assertEquals("FÍSICA GERAL", sub2.name)
        assertEquals("6.0", sub2.grade1)
        assertEquals("5.5", sub2.grade2)

        // Subject 3: HISTÓRIA DO BRASIL
        val sub3 = subjects[2]
        assertEquals("HISTÓRIA DO BRASIL", sub3.name)
        assertEquals("Aprovado", sub3.status)
        assertEquals("9.0", sub3.grade1)
        assertEquals("9.5", sub3.grade2)
    }

    @Test
    fun `parseSubjects ignores unrelated wide tables and preserves detail links`() {
        val doc = Jsoup.parse(loadFixture("boletim.html"))
        doc.body().prepend("<table><tbody><tr>" + "<td>Layout</td>".repeat(20) + "</tr></tbody></table>")
        doc.select("table.bordered tbody tr").first()!!.append("<td><a class='popup' href='/edu/notas/101/'>Detalhes</a></td>")

        val result = SuapParser.parseSubjects(doc)
        assertTrue(result is ParserResult.Success)
        val subjects = (result as ParserResult.Success).data
        assertEquals(3, subjects.size)
        assertEquals("/edu/notas/101/", subjects.first().detailUrl)
    }

    @Test
    fun `parseSubjects rejects unrelated tables instead of replacing offline grades`() {
        val html = "<table><tbody><tr>" + "<td>Outro conteúdo</td>".repeat(20) + "</tr></tbody></table>"
        assertTrue(SuapParser.parseSubjects(Jsoup.parse(html)) is ParserResult.Error)
    }

    @Test
    fun `parseSubjects on empty or invalid table should return Error`() {
        val resultEmpty = SuapParser.parseSubjects(Jsoup.parse("<html><body><div>Sem tabelas</div></body></html>"))
        assertTrue(resultEmpty is ParserResult.Error)

        val resultBlank = SuapParser.parseSubjects(Jsoup.parse(""))
        assertTrue(resultBlank is ParserResult.Error)
    }

    @Test
    fun `parseAvisos should parse avisos HTML fixture correctly`() {
        val html = loadFixture("avisos.html")
        val doc = Jsoup.parse(html)
        val result = SuapParser.parseAvisos(doc)

        assertTrue(result is ParserResult.Success)
        val avisos = (result as ParserResult.Success).data
        assertEquals(3, avisos.size)

        val aviso1 = avisos[0]
        assertEquals("Coordenação Acadêmica", aviso1.sender)
        assertEquals("Horário de Aulas - 2026.1", aviso1.subject)
        assertEquals("01/03/2026", aviso1.date)
        assertTrue(aviso1.isRead)
        assertEquals("/admin/edu/mensagem/5001/", aviso1.detailUrl)

        val aviso2 = avisos[1]
        assertEquals("Departamento de Ensino", aviso2.sender)
        assertEquals("Início das Inscrições para Monitoria", aviso2.subject)
        assertEquals(false, aviso2.isRead)
    }

    @Test
    fun `parseAvisos on invalid table should return Error`() {
        val result = SuapParser.parseAvisos(Jsoup.parse("<html><body><p>Nenhuma mensagem</p></body></html>"))
        assertTrue(result is ParserResult.Error)
    }

    @Test
    fun `parseAvisos treats a valid empty inbox as an empty success`() {
        val result = SuapParser.parseAvisos(
            Jsoup.parse("""
                <table>
                    <thead><tr><th>Remetente</th><th>Assunto</th><th>Data</th></tr></thead>
                    <tbody></tbody>
                </table>
            """)
        )

        assertTrue(result is ParserResult.Success)
        assertTrue((result as ParserResult.Success).data.isEmpty())
    }

    @Test
    fun `parseAulas and parseMaterials should parse aulas HTML fixture correctly`() {
        val html = loadFixture("aulas.html")

        val aulasResult = SuapParser.parseAulas(html)
        assertTrue(aulasResult is ParserResult.Success)
        val aulas = (aulasResult as ParserResult.Success).data
        assertEquals(3, aulas.size)

        assertEquals("10/02/2026", aulas[0].data)
        assertEquals("1º Bimestre", aulas[0].etapa)
        assertEquals("2", aulas[0].numAulas)
        assertEquals("0", aulas[0].numFaltas)
        assertEquals("Prof. Silva", aulas[0].professor)
        assertEquals("/media/edu/material/plano_ensino.pdf", aulas[0].attachmentUrl)

        val materialsResult = SuapParser.parseMaterials(html)
        assertTrue(materialsResult is ParserResult.Success)
        val materials = (materialsResult as ParserResult.Success).data
        assertTrue(materials.size >= 2)
        assertTrue(materials.any { it.title.contains("Lista de Exercícios 1") })
        assertTrue(materials.any { it.url.contains("gabarito1.pdf") })
    }

    @Test
    fun `parseAulas on empty HTML should return Error`() {
        val result = SuapParser.parseAulas("")
        assertTrue(result is ParserResult.Error)
    }

    @Test
    fun `parseSchedule should parse horarios HTML fixture correctly`() {
        val html = loadFixture("horarios.html")
        val doc = Jsoup.parse(html)
        val result = SuapParser.parseSchedule(doc)

        assertTrue(result is ParserResult.Success)
        val (scheduleList, diarioList) = (result as ParserResult.Success).data

        assertTrue("Expected schedule items to be parsed", scheduleList.isNotEmpty())
        assertEquals(2, diarioList.size)

        val mathDiario = diarioList.find { it.code == "MAT001" }
        assertNotNull(mathDiario)
        assertEquals("Matemática I", mathDiario?.component)
        assertEquals("Prof. Silva", mathDiario?.teacher)
        assertEquals("Sala 12", mathDiario?.room)
        assertEquals("2M12", mathDiario?.horarioCode)

        val intervalItem = scheduleList.find { it.subjectName == "Intervalo" }
        assertNotNull(intervalItem)
        assertEquals("09:10 - 09:25", intervalItem?.timeSlot)
    }

    @Test
    fun `parseSchedule on invalid document should return Error`() {
        val result = SuapParser.parseSchedule(Jsoup.parse("<html><body>Vazio</body></html>"))
        assertTrue(result is ParserResult.Error)
    }
}
