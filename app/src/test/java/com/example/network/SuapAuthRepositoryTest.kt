package com.example.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class SuapAuthRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val protectedUrl = "https://suap.ifal.edu.br/edu/aluno/123/?tab=boletim"
    private val loginHtml = "<input name='csrfmiddlewaretoken' value='token'><input id='id_username'><input id='id_password'>"

    private fun repository(
        credentials: Pair<String, String> = "123" to "senha",
        handler: (Request) -> Response
    ): SuapAuthRepository {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { handler(it.request()) }).build()
        return SuapAuthRepository(context, client) { credentials }
    }

    private fun response(request: Request, code: Int = 200, html: String, finalUrl: String? = null): Response =
        Response.Builder()
            .request(if (finalUrl == null) request else request.newBuilder().url(finalUrl).build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Fixture")
            .body(html.toResponseBody("text/html".toMediaType()))
            .build()

    @Test
    fun `blank credentials fail before making a request`() = runBlocking {
        val repository = repository("" to "") { error("No HTTP request expected") }
        assertTrue(repository.login() is NetworkResult.Error)
    }

    @Test
    fun `HTTP errors and blank responses never become successful data`() = runBlocking {
        for ((code, html) in listOf(403 to "Forbidden", 404 to "Not found", 500 to "Failure", 200 to "")) {
            val repository = repository { response(it, code, html) }
            assertTrue("HTTP $code must fail", repository.executeRequest(Request.Builder().url(protectedUrl)) is NetworkResult.Error)
        }
    }

    @Test
    fun `external URLs are rejected before making a request`() = runBlocking {
        val repository = repository { error("No HTTP request expected") }
        assertTrue(repository.executeRequest(Request.Builder().url("https://example.org/")) is NetworkResult.Error)
    }

    @Test
    fun `login rejects server failure and an uncompleted login form`() = runBlocking {
        for (postCode in listOf(200, 403, 500)) {
            val repository = repository { request ->
                response(request, if (request.method == "GET") 200 else postCode, loginHtml)
            }
            assertTrue("Login with POST $postCode must fail", repository.login() is NetworkResult.Error)
        }
    }

    @Test
    fun `expired session is reported without attempting a background login`() = runBlocking {
            val calls = AtomicInteger()
            val repository = repository { request ->
                calls.incrementAndGet()
                response(request, html = loginHtml, finalUrl = SuapAuthRepository.LOGIN_URL)
            }
            val result = repository.executeRequest(Request.Builder().url(protectedUrl))
            assertTrue(result is NetworkResult.Error)
            assertTrue((result as NetworkResult.Error).message.contains("Sessão do SUAP expirada"))
            assertEquals("A saved password must never be replayed during silent sync", 1, calls.get())
    }

    @Test
    fun `concurrent expired requests do not submit credentials`() = runBlocking {
        val requestCount = AtomicInteger()
        val repository = repository { request ->
            requestCount.incrementAndGet()
            response(request, html = loginHtml, finalUrl = SuapAuthRepository.LOGIN_URL)
        }
        val results = List(2) { async { repository.executeRequest(Request.Builder().url(protectedUrl)) } }.awaitAll()
        assertTrue(results.all { it is NetworkResult.Error })
        assertEquals(2, requestCount.get())
    }
}
