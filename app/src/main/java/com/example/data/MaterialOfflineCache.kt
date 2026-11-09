package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small bounded metadata cache; file contents remain under DownloadManager control. */
object MaterialOfflineCache {
    private const val PREFIX = "materials_cache_"

    private fun key(subjectUrl: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(subjectUrl.trim().toByteArray(Charsets.UTF_8))
        return PREFIX + digest.joinToString("") { "%02x".format(it) }
    }

    fun load(context: Context, subjectUrl: String): List<Material> {
        return try {
            val prefs = context.applicationContext.getSharedPreferences("suap_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString(key(subjectUrl), null) ?: return emptyList()
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Material(item.optString("title"), item.optString("url"), item.optString("date"))
            }.filter { it.url.isNotBlank() }
        } catch (e: Exception) {
            com.example.util.AppLog.w("MaterialOfflineCache", "Could not read material cache", e)
            emptyList()
        }
    }

    fun save(context: Context, subjectUrl: String, materials: List<Material>) {
        if (materials.isEmpty()) return
        try {
            val prefs = context.applicationContext.getSharedPreferences("suap_prefs", Context.MODE_PRIVATE)
            val array = JSONArray()
            materials.take(100).forEach { material ->
                array.put(JSONObject().put("title", material.title).put("url", material.url).put("date", material.date))
            }
            val cacheKey = key(subjectUrl)
            prefs.edit().putString(cacheKey, array.toString()).apply()
            val keys = prefs.all.keys.filter { it.startsWith(PREFIX) }
            if (keys.size > 40) {
                prefs.edit().apply { keys.take(keys.size - 40).forEach { cacheKey -> remove(cacheKey) } }.apply()
            }
        } catch (e: Exception) {
            com.example.util.AppLog.w("MaterialOfflineCache", "Could not save material cache", e)
        }
    }
}
