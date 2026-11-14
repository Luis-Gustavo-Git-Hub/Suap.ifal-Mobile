package com.example.util

import com.example.data.ProfessorContact
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorEmailTest {
    @Test
    fun `find prefers a mailto address and normalizes case`() {
        val person = Jsoup.parse("""
            <div class="person">
                <a href="MAILTO:Prof.Example@IFAL.edu.br?subject=Contato">E-mail</a>
                <dd>wrong@example.com</dd>
            </div>
        """).selectFirst(".person")!!

        assertEquals("prof.example@ifal.edu.br", ProfessorEmail.find(person))
    }

    @Test
    fun `fromProfile reads labeled institutional email`() {
        val profile = Jsoup.parse("""
            <dl>
                <dt>E-mail institucional</dt>
                <dd><a href="mailto:ana@ifal.edu.br">ana@ifal.edu.br</a></dd>
            </dl>
            <a href="mailto:suporte@ifal.edu.br">Suporte</a>
        """)

        assertEquals("ana@ifal.edu.br", ProfessorEmail.fromProfile(profile, "Ana Silva"))
    }

    @Test
    fun `resolve replaces legacy generated addresses with a marked estimate`() {
        val name = "Ana Maria da Silva"
        val oldContact = ProfessorContact(
            name = name,
            email = ProfessorEmail.legacyGuess(name),
            discipline = "",
            emailEstimated = false
        )

        assertEquals(
            ProfessorEmail.estimate(name) to true,
            ProfessorEmail.resolve(found = null, existing = oldContact, name = name)
        )
    }

    @Test
    fun `resolve prefers a real address and preserves a saved real address`() {
        val existing = ProfessorContact(name = "Ana Silva", email = "ana@ifal.edu.br", discipline = "")

        assertEquals(
            "ana.silva@ifal.edu.br" to false,
            ProfessorEmail.resolve("ana.silva@ifal.edu.br", existing, "Ana Silva")
        )
        assertEquals(
            "ana@ifal.edu.br" to false,
            ProfessorEmail.resolve(null, existing, "Ana Silva")
        )
    }

    @Test
    fun `validates manually entered addresses`() {
        assertTrue(ProfessorEmail.isValid("prof@ifal.edu.br"))
        assertFalse(ProfessorEmail.isValid("not-an-email"))
        val invalidSaved = ProfessorContact(name = "Prof Exemplo", email = "not-an-email", discipline = "")
        assertEquals(
            ProfessorEmail.estimate("Prof Exemplo") to true,
            ProfessorEmail.resolve(null, invalidSaved, "Prof Exemplo")
        )
    }
}
