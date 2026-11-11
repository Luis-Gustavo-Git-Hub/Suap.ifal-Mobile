package com.example.data

import android.content.Context
import com.example.network.NetworkResult
import com.example.network.SuapAuthRepository
import com.example.util.SuapParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class AvisosRepository private constructor(
    private val context: Context,
    private val suapAuthRepository: SuapAuthRepository = SuapAuthRepository.getInstance(context)
) {
    suspend fun fetchAvisos(): NetworkResult<List<Aviso>> = withContext(Dispatchers.IO) {
        val url = "https://suap.ifal.edu.br/admin/edu/mensagementrada/?tab=tab_todas"
        val request = Request.Builder().url(url)
        when (val result = suapAuthRepository.executeRequest(request)) {
            is NetworkResult.Success -> {
                val parseResult = SuapParser.parseAvisos(Jsoup.parse(result.data))
                when (parseResult) {
                    is com.example.util.ParserResult.Success -> NetworkResult.Success(parseResult.data)
                    is com.example.util.ParserResult.Error -> NetworkResult.Error(parseResult.message)
                }
            }
            is NetworkResult.Error -> result
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AvisosRepository? = null

        fun getInstance(context: Context): AvisosRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AvisosRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
