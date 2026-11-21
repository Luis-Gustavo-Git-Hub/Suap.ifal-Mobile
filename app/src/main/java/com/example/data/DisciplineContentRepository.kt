package com.example.data

import android.content.Context
import android.net.Uri
import com.example.util.SuapParser
import kotlinx.coroutines.CancellationException
import okhttp3.Request

data class DisciplineSyncResult(val aulas: List<AulaHistory>, val materials: List<Material>, val failed: Boolean, val offline: Boolean, val error: com.example.network.SuapError?)

/** Detail retrieval uses the same foreground session owner as the main sync. */
class DisciplineContentRepository(private val context: Context) {
    fun buildUrlWithTab(rawUrl: String, tabName: String): String {
        val base = if (rawUrl.startsWith("http")) rawUrl else "https://suap.ifal.edu.br${if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"}"
        val uri = Uri.parse(base)
        val builder = uri.buildUpon()
        builder.clearQuery()
        uri.queryParameterNames.forEach { param ->
            if (param != "tab") {
                uri.getQueryParameters(param).forEach { value ->
                    builder.appendQueryParameter(param, value)
                }
            }
        }
        builder.appendQueryParameter("tab", tabName)
        return builder.build().toString()
    }

    suspend fun load(url: String): DisciplineSyncResult {
                val suapAuthRepository = com.example.network.SuapAuthRepository.getInstance(context)

                // Extract discipline numeric ID if present (e.g. 2236)
                val idMatch = Regex("""/(\d+)/?""").find(url)?.groupValues?.get(1)

                val allParsedAulas = mutableListOf<AulaHistory>()
                val allParsedMaterials = mutableListOf<Material>()
                var aulasRequestFailed = false

                // Primary URL to fetch aulas
                val finalAulasUrl = buildUrlWithTab(url, "aulas")
                val aulasReq = Request.Builder().url(finalAulasUrl)
                val aulasResult = suapAuthRepository.executeRequest(aulasReq)

                aulasResult.onSuccess { html ->
                    if (html.isNotEmpty()) {
                        allParsedAulas.addAll(SuapParser.parseAulas(html).getOrDefault(emptyList()))
                        allParsedMaterials.addAll(SuapParser.parseMaterials(html).getOrDefault(emptyList()))

                        // If empty aulas, check if there's a link to the real disciplina
                        val disciplinaHref = SuapParser.findDisciplinaUrl(html).getOrNull()
                        if (allParsedAulas.isEmpty() && !disciplinaHref.isNullOrEmpty()) {
                            val redirectUrl = buildUrlWithTab(disciplinaHref, "aulas")
                            val redirectResult = suapAuthRepository.executeRequest(Request.Builder().url(redirectUrl))
                            redirectResult.onSuccess { redirectHtml ->
                                if (redirectHtml.isNotEmpty()) {
                                    allParsedAulas.addAll(SuapParser.parseAulas(redirectHtml).getOrDefault(emptyList()))
                                    allParsedMaterials.addAll(SuapParser.parseMaterials(redirectHtml).getOrDefault(emptyList()))
                                }
                            }.onError { message, _ ->
                                aulasRequestFailed = true
                                com.example.util.AppLog.w("AulasScreen", "Erro ao buscar aulas na disciplina vinculada: $message")
                            }
                        }
                    }
                }.onError { msg, _ ->
                    aulasRequestFailed = true
                    com.example.util.AppLog.e("AulasScreen", "Erro ao carregar aulas: $msg")
                }

                // Explicitly query both possible materiais tab URLs: /edu/disciplina/$id/?tab=materiais and /edu/aluno_disciplina/$id/?tab=materiais
                val urlsToTryMaterials = mutableListOf<String>()
                if (!idMatch.isNullOrBlank()) {
                    urlsToTryMaterials.add("https://suap.ifal.edu.br/edu/disciplina/$idMatch/?tab=materiais")
                    urlsToTryMaterials.add("https://suap.ifal.edu.br/edu/aluno_disciplina/$idMatch/?tab=materiais")
                }
                urlsToTryMaterials.add(buildUrlWithTab(url, "materiais"))

                for (matUrl in urlsToTryMaterials.distinct()) {
                    try {
                        val matResult = suapAuthRepository.executeRequest(Request.Builder().url(matUrl))
                        matResult.onSuccess { matHtml ->
                            if (matHtml.isNotEmpty()) {
                                allParsedMaterials.addAll(SuapParser.parseMaterials(matHtml).getOrDefault(emptyList()))
                                if (allParsedAulas.isEmpty()) {
                                    allParsedAulas.addAll(SuapParser.parseAulas(matHtml).getOrDefault(emptyList()))
                                }
                            }
                        }.onError { message, _ ->
                            aulasRequestFailed = true
                            com.example.util.AppLog.w("AulasScreen", "Erro ao buscar dados offline da disciplina: $message")
                        }
                        if (allParsedMaterials.isNotEmpty()) break
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        com.example.util.AppLog.printStackTrace(e)
                    }
                }

                val finalAulas = allParsedAulas.distinctBy { "${it.data}_${it.conteudo}" }
                val finalMaterials = allParsedMaterials.distinctBy { it.url }
                if (finalMaterials.isNotEmpty()) MaterialOfflineCache.save(context, url, finalMaterials)
                val offlineAulas = if (finalAulas.isEmpty() && aulasRequestFailed) {
                    try {
                        AppDatabase.getDatabase(context).aulaHistoryDao().getAulasForSubject(url).map { it.toDomain() }
                    } catch (e: Exception) {
                        com.example.util.AppLog.w("AulasScreen", "Could not load offline class history", e)
                        emptyList()
                    }
                } else emptyList()

                // Save successfully loaded aulas to Room Database for Offline availability
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val dao = db.aulaHistoryDao()
                    if (finalAulas.isNotEmpty()) {
                        dao.deleteAulasForSubject(url)
                        dao.insertAulas(finalAulas.map { it.toEntity(url) })
                    }
                } catch (dbEx: Exception) {
                    com.example.util.AppLog.printStackTrace(dbEx)
                }

        return DisciplineSyncResult(finalAulas.ifEmpty { offlineAulas },
            finalMaterials.ifEmpty { MaterialOfflineCache.load(context, url) }, aulasRequestFailed, offlineAulas.isNotEmpty(), suapAuthRepository.lastError)
    }
}
