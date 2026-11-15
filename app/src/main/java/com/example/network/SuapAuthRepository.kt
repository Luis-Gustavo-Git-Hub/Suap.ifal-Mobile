package com.example.network

import android.content.Context
import com.example.util.AppLog
import com.example.util.CrashLogger
import com.example.util.CredentialStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Throwable? = null, val error: SuapError = exception?.let(SuapError::fromException) ?: SuapError.Unknown) : NetworkResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Throwable?) -> Unit): NetworkResult<T> {
        if (this is Error) action(message, exception)
        return this
    }
}

class SuapAuthRepository internal constructor(
    private val context: Context,
    private val client: OkHttpClient = SuapClient.okHttpClient,
    private val credentialsProvider: () -> Pair<String, String> = {
        val store = CredentialStore.getInstance(context.applicationContext)
        store.getSuapUsername() to store.getSuapPassword()
    }
) {
    private val loginMutex = Mutex()
    @Volatile var lastError: SuapError? = null
    internal var coordinator: com.example.sync.SyncCoordinator? = null

    companion object {
        private val requestPermits = java.util.concurrent.Semaphore(2, true)
        const val BASE_URL = "https://suap.ifal.edu.br"
        const val LOGIN_URL = "https://suap.ifal.edu.br/accounts/login/"

        @Volatile
        private var INSTANCE: SuapAuthRepository? = null

        fun getInstance(context: Context): SuapAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SuapAuthRepository(context.applicationContext).also { it.coordinator = com.example.sync.SyncCoordinator.shared; INSTANCE = it }
            }
        }
    }

    suspend fun login(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        loginMutex.withLock { SyncMetrics.logins.incrementAndGet(); performLogin() }
    }

    // Called with loginMutex held and on Dispatchers.IO.
    private fun performLogin(): NetworkResult<Boolean> {
        return try {
            val (username, password) = credentialsProvider()
            if (username.isBlank() || password.isBlank()) {
                diagnostic("Login SUAP interrompido: credenciais ausentes.")
                return NetworkResult.Error(SuapError.NoCredentials.message, error = SuapError.NoCredentials)
            }
            val loginPage = fetchPage(Request.Builder().url(LOGIN_URL).get().build())
            diagnostic("Login SUAP GET: status=${loginPage.code}, rota=${loginPage.safeRoute()}, csrf=${hasCsrf(loginPage.html)}.")
            if (!loginPage.isSuccessful) {
                return NetworkResult.Error("Falha ao carregar página de login do SUAP. Código: ${loginPage.code}", error = if (loginPage.code >= 500) SuapError.SuapUnavailable else SuapError.Unknown)
            }
            val csrfToken = Jsoup.parse(loginPage.html).select("input[name=csrfmiddlewaretoken]").`val`()
            if (csrfToken.isBlank()) {
                diagnostic("Login SUAP falhou: token CSRF ausente na página.")
                return NetworkResult.Error(SuapError.VerificationRequired.message, error = SuapError.VerificationRequired)
            }

            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("csrfmiddlewaretoken", csrfToken)
                .add("this_is_the_login_form", "1")
                .add("next", "/")
                .build()
            val page = fetchPage(
                Request.Builder().url(LOGIN_URL).post(formBody).header("Referer", LOGIN_URL).build()
            )
            diagnostic("Login SUAP POST: status=${page.code}, rota=${page.safeRoute()}, formularioLogin=${page.requiresLogin()}.")
            if (!page.isSuccessful) {
                return NetworkResult.Error("Falha ao entrar no SUAP. Código: ${page.code}", error = if (page.code >= 500) SuapError.SuapUnavailable else SuapError.Unknown)
            }
            if (page.requiresLogin() && (page.html.contains("captcha", true) || page.html.contains("verificação", true))) {
                return NetworkResult.Error(SuapError.VerificationRequired.message, error = SuapError.VerificationRequired)
            }
            if (page.html.contains("alert-error") || page.html.contains("errorlist") ||
                page.html.contains("Usuário e/ou senha inválidos")) {
                return NetworkResult.Error(SuapError.InvalidCredentials.message, error = SuapError.InvalidCredentials)
            }
            if (page.requiresLogin() || page.html.isBlank()) {
                diagnostic("Login SUAP não confirmado: resposta permaneceu na tela de login ou veio vazia.")
                return NetworkResult.Error(SuapError.VerificationRequired.message, error = SuapError.VerificationRequired)
            }

            diagnostic("Login SUAP confirmado pelo fluxo HTTP.")
            AppLog.d("SuapAuthRepository", "Sessão autenticada com sucesso no SUAP")
            NetworkResult.Success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            diagnostic("Exceção no login SUAP: ${e.javaClass.simpleName}.")
            AppLog.e("SuapAuthRepository", "Erro durante login no SUAP", e)
            NetworkResult.Error(SuapError.fromException(e).message, e, SuapError.fromException(e))
        }
    }

    /** Detects rejection; the foreground coordinator owns reauthentication, never this repository. */
    suspend fun executeRequest(requestBuilder: Request.Builder, coordinate: Boolean = true): NetworkResult<String> = withContext(Dispatchers.IO) {
        val owner = coordinator.takeIf { coordinate }
        try {
            val request = requestBuilder.build()
            if (!request.url.isHttps || request.url.host != "suap.ifal.edu.br") {
                return@withContext NetworkResult.Error("Endereço bloqueado: só o SUAP oficial é permitido.")
            }
            if (owner?.isSyncing == true && !owner.awaitSession()) {
                val error = owner.currentError ?: SuapError.Timeout
                return@withContext NetworkResult.Error(error.message, error = error)
            }
            val sessionVersion = owner?.sessionVersion
            var page = fetchPage(request)
            if (page.requiresLogin()) {
                diagnostic("Sessão SUAP recusada: status=${page.code}, rotaSolicitada=${safeRoute(request.url)}, rotaFinal=${page.safeRoute()}.")
                if (owner != null) {
                    owner.onSessionRejected(sessionVersion ?: owner.sessionVersion)
                    if (owner.awaitSession()) page = fetchPage(request) // original request retried once
                }
                if (page.requiresLogin()) {
                    val error = owner?.currentError ?: SuapError.SessionExpired(true)
                    if (owner?.currentError == null) owner?.fail(error)
                    return@withContext NetworkResult.Error(error.message, error = error)
                }
            }
            when {
                page.code >= 500 -> NetworkResult.Error(SuapError.SuapUnavailable.message, error = SuapError.SuapUnavailable)
                !page.isSuccessful -> NetworkResult.Error("O SUAP não concluiu a solicitação. Código: ${page.code}")
                page.html.isBlank() -> NetworkResult.Error(SuapError.ParseError.message, error = SuapError.ParseError)
                else -> NetworkResult.Success(page.html)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val error = SuapError.fromException(e)
            NetworkResult.Error(error.message, e, error)
        }
    }.also { if (coordinate && it is NetworkResult.Error) lastError = it.error }

    suspend fun executeRaw(request: Request): okhttp3.Response = withContext(Dispatchers.IO) {
        if (!request.url.isHttps || request.url.host != "suap.ifal.edu.br") throw SuapRequestException(SuapError.Unknown)
        val owner = coordinator
        if (owner?.isSyncing == true && !owner.awaitSession()) throw SuapRequestException(owner.currentError ?: SuapError.Timeout)
        val version = owner?.sessionVersion
        SyncMetrics.requests.incrementAndGet()
        var response = client.newCall(request).execute()
        if (response.code == 401 || response.request.url.encodedPath.startsWith("/accounts/login")) {
            response.close()
            owner?.onSessionRejected(version ?: owner.sessionVersion)
            if (owner?.awaitSession() != true) throw SuapRequestException(SuapError.SessionExpired(true))
            SyncMetrics.requests.incrementAndGet()
            response = client.newCall(request).execute()
            if (response.code == 401 || response.request.url.encodedPath.startsWith("/accounts/login")) {
                response.close()
                owner.fail(SuapError.SessionExpired(true))
                throw SuapRequestException(SuapError.SessionExpired(true))
            }
        }
        response
    }

    suspend fun readDocument(url: String): org.jsoup.nodes.Document = when (val result = executeRequest(Request.Builder().url(url))) {
        is NetworkResult.Success -> org.jsoup.Jsoup.parse(result.data, url)
        is NetworkResult.Error -> throw SuapRequestException(result.error)
    }

    fun cookieHeaderFor(url: String): String {
        if (!com.example.util.UrlSecurityUtils.isAllowedSuapUrl(url)) return ""
        return android.webkit.CookieManager.getInstance().getCookie(url).orEmpty()
    }

    private fun fetchPage(request: Request): Page {
        requestPermits.acquire()
        try {
            SyncMetrics.requests.incrementAndGet()
            return client.newCall(request).execute().use { response ->
                Page(response.body?.string().orEmpty(), response.request.url, response.code, response.isSuccessful)
            }
        } finally { requestPermits.release() }
    }

    private fun diagnostic(message: String) {
        CrashLogger.log(context.applicationContext, "INFO", "SUAP_AUTH: $message")
    }

    private fun hasCsrf(html: String): Boolean =
        Jsoup.parse(html).selectFirst("input[name=csrfmiddlewaretoken]") != null

    private fun safeRoute(url: HttpUrl): String = when {
        url.host != "suap.ifal.edu.br" -> "host-nao-oficial"
        url.encodedPath.startsWith("/accounts/login") -> "/accounts/login"
        url.encodedPath.matches(Regex("/edu/aluno/\\d+/?")) -> "/edu/aluno/{matricula}"
        else -> "/outra-rota"
    }

    private data class Page(val html: String, val url: HttpUrl, val code: Int, val isSuccessful: Boolean) {
        fun safeRoute(): String = when {
            url.host != "suap.ifal.edu.br" -> "host-nao-oficial"
            url.encodedPath.startsWith("/accounts/login") -> "/accounts/login"
            url.encodedPath.matches(Regex("/edu/aluno/\\d+/?")) -> "/edu/aluno/{matricula}"
            else -> "/outra-rota"
        }

        fun requiresLogin(): Boolean = code == 401 || url.encodedPath.startsWith("/accounts/login") ||
            LOGIN_FORM_FIELD.containsMatchIn(html) ||
            (LOGIN_USERNAME_FIELD.containsMatchIn(html) && LOGIN_PASSWORD_FIELD.containsMatchIn(html))
    }
}

private val LOGIN_FORM_FIELD = Regex("""\bname\s*=\s*(['"])this_is_the_login_form\1""", RegexOption.IGNORE_CASE)
private val LOGIN_USERNAME_FIELD = Regex("""\bid\s*=\s*(['"])id_username\1""", RegexOption.IGNORE_CASE)
private val LOGIN_PASSWORD_FIELD = Regex("""\bid\s*=\s*(['"])id_password\1""", RegexOption.IGNORE_CASE)

/** Counters only: no identifiers, cookie values, URLs or HTML. */
object SyncMetrics {
    val requests = java.util.concurrent.atomic.AtomicInteger()
    val logins = java.util.concurrent.atomic.AtomicInteger()
}
