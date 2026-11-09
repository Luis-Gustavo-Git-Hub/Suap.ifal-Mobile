package com.example.data

import android.content.Context
import com.example.network.SuapClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class AlmocoRepository private constructor(
    private val context: Context,
    private val client: OkHttpClient = SuapClient.okHttpClient
) {
    suspend fun getCafeteriaStatus(): String = withContext(Dispatchers.IO) {
        "Status do Restaurante Universitário ok"
    }

    companion object {
        @Volatile
        private var INSTANCE: AlmocoRepository? = null

        fun getInstance(context: Context): AlmocoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlmocoRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
