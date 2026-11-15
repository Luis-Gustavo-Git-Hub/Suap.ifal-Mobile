package com.example.worker

import com.example.network.NetworkResult
import com.example.network.SuapError
import okhttp3.Request

/** One recovery budget per worker execution, shared by all its module requests. */
internal class WorkerSessionRecovery(
    private val allowPasswordLogin: Boolean,
    private val request: suspend (Request.Builder) -> NetworkResult<String>,
    private val login: suspend () -> NetworkResult<Boolean>
) {
    private var attempted = false
    suspend fun execute(builder: Request.Builder): NetworkResult<String> {
        val response = request(builder)
        if (response !is NetworkResult.Error || response.error !is SuapError.SessionExpired ||
            !allowPasswordLogin || attempted) return response
        attempted = true
        return when (val result = login()) {
            is NetworkResult.Success -> request(builder)
            is NetworkResult.Error -> result
        }
    }
}
