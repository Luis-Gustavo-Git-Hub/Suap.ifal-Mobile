package com.example.util

import com.example.data.ProfessorContact
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.Normalizer

/**
 * Regras de e-mail de professores.
 *
 * O SUAP nem sempre expõe o e-mail do professor nas páginas lidas. Antes o app
 * montava `nome.completo.com.todos.os.nomes@ifal.edu.br` e tratava como real, o que
 * gerava endereços errados. Agora:
 *  - e-mail encontrado no HTML (link mailto: ou texto com @) é sempre preferido;
 *  - se não houver, usamos uma estimativa `primeiro.ultimo@ifal.edu.br` marcada como estimada,
 *    e a estimativa nunca sobrescreve um e-mail real já salvo.
 */
object ProfessorEmail {
    private val EMAIL_REGEX = Regex("(?i)[A-Z0-9_%+-]+(?:\\.[A-Z0-9_%+-]+)*@(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\\.)+[A-Z]{2,63}")
    private val PARTICLES = setOf("da", "de", "di", "do", "das", "dos", "e")

    fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    private fun mailtoAddress(element: Element): String? = element.select("a[href]")
        .firstOrNull { it.attr("href").startsWith("mailto:", ignoreCase = true) }
        ?.attr("href")
        ?.substringAfter(':')
        ?.substringBefore('?')
        ?.trim()
        ?.takeIf { EMAIL_REGEX.matches(it) }
        ?.lowercase()

    /** Procura um e-mail real dentro do elemento (mailto: primeiro, depois texto dos <dd>). */
    fun find(element: Element): String? {
        mailtoAddress(element)?.let { return it }

        for (dt in element.select("dt")) {
            if (!dt.text().contains("mail", ignoreCase = true)) continue
            val dd = dt.nextElementSibling()?.takeIf { it.tagName() == "dd" } ?: continue
            EMAIL_REGEX.find(dd.text())?.value?.let { return it.lowercase() }
        }

        for (dd in element.select("dd")) {
            EMAIL_REGEX.find(dd.text())?.value?.let { return it.lowercase() }
        }
        return null
    }

    /** Formato antigo (errado) que o app gerava: todos os nomes separados por ponto. */
    fun legacyGuess(name: String): String =
        "${name.lowercase().replace(" ", ".")}@ifal.edu.br"

    /** Estimativa: primeiro.ultimo@ifal.edu.br, sem acentos nem partículas (de, da, dos...). */
    fun estimate(name: String): String {
        val tokens = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .split(Regex("\\s+"))
            .map { it.filter { c -> c.isLetter() } }
            .filter { it.isNotEmpty() && it !in PARTICLES }
        if (tokens.isEmpty()) return ""
        val local = if (tokens.size == 1) tokens.first() else "${tokens.first()}.${tokens.last()}"
        return "$local@ifal.edu.br"
    }

    /** Decide o e-mail final e se ele é uma estimativa. */
    fun resolve(found: String?, existing: ProfessorContact?, name: String): Pair<String, Boolean> = when {
        !found.isNullOrBlank() -> found to false
        existing != null && existing.email.isNotBlank() && !existing.emailEstimated &&
            isValid(existing.email) &&
            !existing.email.equals(legacyGuess(name), ignoreCase = true) -> existing.email to false
        else -> estimate(name) to true
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    /** Chave estável para juntar o mesmo professor apesar de acentos ou espaços inconsistentes. */
    fun nameKey(name: String): String = normalize(name)
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Link para o perfil do professor dentro de um bloco (foto/nome costumam ser links). */
    fun findProfileLink(element: Element): String? {
        val a = element.select("a[href]").firstOrNull {
            val h = it.attr("href")
            h.contains("/rh/servidor/") || h.contains("/edu/professor") || h.contains("/rh/")
        } ?: return null
        val h = a.attr("href").trim()
        if (h.isEmpty() || h.startsWith("#") || h.startsWith("javascript")) return null
        return if (h.startsWith("http")) h else "https://suap.ifal.edu.br" + (if (h.startsWith("/")) h else "/$h")
    }

    /**
     * Lê o e-mail na página de perfil do professor. Prioriza campos rotulados
     * ("E-mail Institucional", "E-mail"...) e só aceita mailto: solto se o endereço
     * contiver o primeiro nome do professor (evita pegar e-mail de suporte/rodapé).
     */
    fun fromProfile(doc: Document, name: String): String? {
        val labeled = mutableListOf<Pair<String, Element>>()
        for (dt in doc.select("dt")) {
            val dd = dt.nextElementSibling()?.takeIf { it.tagName() == "dd" } ?: continue
            labeled.add(dt.text().trim() to dd)
        }
        for (tr in doc.select("tr")) {
            val cells = tr.select("th, td")
            if (cells.size >= 2) labeled.add(cells[0].text().trim() to cells[1])
        }
        val mailLabeled = labeled.filter { it.first.contains("mail", ignoreCase = true) }
        val ordered = mailLabeled.sortedByDescending { it.first.contains("institucional", ignoreCase = true) }
        for ((_, el) in ordered) {
            mailtoAddress(el)?.let { return it }
            EMAIL_REGEX.find(el.text())?.value?.let { return it.lowercase() }
        }

        val firstName = normalize(name).split(Regex("\\s+")).firstOrNull { it.isNotBlank() && it !in PARTICLES }
        if (firstName != null) {
            for (a in doc.select("a[href]")) {
                val href = a.attr("href")
                if (!href.startsWith("mailto:", ignoreCase = true)) continue
                val addr = href.substringAfter(':').substringBefore('?').trim()
                if (EMAIL_REGEX.matches(addr) && normalize(addr).substringBefore("@").contains(firstName)) return addr.lowercase()
            }
        }
        return null
    }
}
