package com.example.util

import android.content.Context
import com.example.ui.JustificativaDetalhes
import com.example.ui.JustificativaFaltaItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cache offline das justificativas de falta (lista e detalhes), para que as telas
 * de acompanhamento continuem funcionando sem internet ou com a sessão expirada.
 */
object JustificativaCache {
    private const val PREFS = "justificativa_cache"
    private const val KEY_LIST = "list"
    private fun detailKey(id: String) = "detail_$id"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveList(context: Context, items: List<JustificativaFaltaItem>) {
        try {
            val array = JSONArray()
            for (i in items) {
                array.put(JSONObject().apply {
                    put("dataInicio", i.dataInicio)
                    put("dataFim", i.dataFim)
                    put("motivo", i.motivo)
                    put("dataSolicitacao", i.dataSolicitacao)
                    put("situacao", i.situacao)
                    put("detalhesUrl", i.detalhesUrl ?: JSONObject.NULL)
                })
            }
            prefs(context).edit().putString(KEY_LIST, array.toString()).apply()
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
        }
    }

    /** Retorna null se nunca houve cache (lista vazia salva é um cache válido). */
    fun loadList(context: Context): List<JustificativaFaltaItem>? {
        return try {
            val json = prefs(context).getString(KEY_LIST, null) ?: return null
            val array = JSONArray(json)
            List(array.length()) { idx ->
                val o = array.getJSONObject(idx)
                JustificativaFaltaItem(
                    dataInicio = o.optString("dataInicio"),
                    dataFim = o.optString("dataFim"),
                    motivo = o.optString("motivo"),
                    dataSolicitacao = o.optString("dataSolicitacao"),
                    situacao = o.optString("situacao"),
                    detalhesUrl = if (o.isNull("detalhesUrl")) null else o.optString("detalhesUrl")
                )
            }
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            null
        }
    }

    fun saveDetalhes(context: Context, id: String, d: JustificativaDetalhes) {
        try {
            val faltas = JSONArray()
            for (row in d.detalhamentoFaltas) {
                val m = JSONObject()
                for ((k, v) in row) m.put(k, v)
                faltas.put(m)
            }
            val o = JSONObject().apply {
                put("aluno", d.aluno)
                put("dataInicio", d.dataInicio)
                put("dataFim", d.dataFim)
                put("motivo", d.motivo)
                put("infoComplementar", d.infoComplementar)
                put("anexoNome", d.anexoNome ?: JSONObject.NULL)
                put("anexoUrl", d.anexoUrl ?: JSONObject.NULL)
                put("dataSolicitacao", d.dataSolicitacao)
                put("situacao", d.situacao)
                put("podeSolicitarReposicao", d.podeSolicitarReposicao)
                put("detalhamentoMensagem", d.detalhamentoMensagem ?: JSONObject.NULL)
                put("detalhamentoFaltas", faltas)
            }
            prefs(context).edit().putString(detailKey(id), o.toString()).apply()
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
        }
    }

    fun loadDetalhes(context: Context, id: String): JustificativaDetalhes? {
        return try {
            val json = prefs(context).getString(detailKey(id), null) ?: return null
            val o = JSONObject(json)
            val faltasArr = o.optJSONArray("detalhamentoFaltas") ?: JSONArray()
            val faltas = List(faltasArr.length()) { i ->
                val m = faltasArr.getJSONObject(i)
                m.keys().asSequence().associateWith { k -> m.optString(k) }
            }
            JustificativaDetalhes(
                aluno = o.optString("aluno", "-"),
                dataInicio = o.optString("dataInicio", "-"),
                dataFim = o.optString("dataFim", "-"),
                motivo = o.optString("motivo", "-"),
                infoComplementar = o.optString("infoComplementar", "-"),
                anexoNome = if (o.isNull("anexoNome")) null else o.optString("anexoNome"),
                anexoUrl = if (o.isNull("anexoUrl")) null else o.optString("anexoUrl"),
                dataSolicitacao = o.optString("dataSolicitacao", "-"),
                situacao = o.optString("situacao", "Desconhecido"),
                podeSolicitarReposicao = o.optBoolean("podeSolicitarReposicao", false),
                detalhamentoMensagem = if (o.isNull("detalhamentoMensagem")) null else o.optString("detalhamentoMensagem"),
                detalhamentoFaltas = faltas
            )
        } catch (e: Exception) {
            AppLog.printStackTrace(e)
            null
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
