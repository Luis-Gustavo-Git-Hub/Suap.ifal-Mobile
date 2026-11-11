package com.example.data

import android.content.Context
import com.example.network.NetworkResult
import com.example.network.SuapAuthRepository
import com.example.util.SuapParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class ScheduleRepository private constructor(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context),
    private val suapAuthRepository: SuapAuthRepository = SuapAuthRepository.getInstance(context)
) {
    val aulaHistoryDao = db.aulaHistoryDao()

    suspend fun getAulasForSubject(url: String): List<AulaHistoryEntity> {
        return aulaHistoryDao.getAulasForSubject(url)
    }

    suspend fun saveAulasForSubject(url: String, aulas: List<AulaHistoryEntity>) {
        if (aulas.isNotEmpty()) {
            aulaHistoryDao.deleteAulasForSubject(url)
            aulaHistoryDao.insertAulas(aulas)
        }
    }

    suspend fun fetchScheduleFromSuap(matricula: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        val url = "https://suap.ifal.edu.br/edu/aluno/$matricula/?tab=locais_aula_aluno"
        suapAuthRepository.executeRequest(Request.Builder().url(url))
    }

    companion object {
        @Volatile
        private var INSTANCE: ScheduleRepository? = null

        fun getInstance(context: Context): ScheduleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
