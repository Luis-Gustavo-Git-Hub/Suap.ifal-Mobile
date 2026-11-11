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
    data class Error(val message: String, val exception: Throwable? = null) : NetworkResult<Nothing>()

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

    companion object {
        const val BASE_URL = "https://suap.ifal.edu.br"
        const val LOGIN_URL = "https://suap.ifal.edu.br/accounts/login/"

        @Volatile
        private var INSTANCE: SuapAuthRepository? = null

        fun getInstance(context: Context): SuapAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SuapAuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun login(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        loginMutex.withLock { performLogin() }
    }

    // Called with loginMutex held and on Dispatchers.IO.
    private fun performLogin(): NetworkResult<Boolean> {
        return try {
            val (username, password) = credentialsProvider()
            if (username.isBlank() || password.isBlank()) {
                diagnostic("Login SUAP interrompido: credenciais ausentes.")
                return NetworkResult.Error("Credenciais do SUAP não configuradas.")
            }
            val loginPage = fetchPage(Request.Builder().url(LOGIN_URL).get().build())
            diagnostic("Login SUAP GET: status=${loginPage.code}, rota=${loginPage.safeRoute()}, csrf=${hasCsrf(loginPage.html)}.")
            if (!loginPage.isSuccessful) {
                return NetworkResult.Error("Falha ao carregar página de login do SUAP. Código: ${loginPage.code}")
            }
            val csrfToken = Jsoup.parse(loginPage.html).select("input[name=csrfmiddlewaretoken]").`val`()
            if (csrfToken.isBlank()) {
                diagnostic("Login SUAP falhou: token CSRF ausente na página.")
                return NetworkResult.Error("Token de segurança (CSRF) não encontrado na página do SUAP.")
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
                return NetworkResult.Error("Falha ao entrar no SUAP. Código: ${page.code}")
            }
            if (page.html.contains("alert-error") || page.html.contains("errorlist") ||
                page.html.contains("Usuário e/ou senha inválidos")) {
                return NetworkResult.Error("Usuário ou senha incorretos no SUAP.")
            }
            if (page.requiresLogin() || page.html.isBlank()) {
                diagnostic("Login SUAP não confirmado: resposta permaneceu na tela de login ou veio vazia.")
                return NetworkResult.Error("O SUAP não confirmou o login. Verifique suas credenciais ou conclua a verificação na tela de login.")
            }

            diagnostic("Login SUAP confirmado pelo fluxo HTTP.")
            AppLog.d("SuapAuthRepository", "Sessão autenticada com sucesso no SUAP")
            NetworkResult.Success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            diagnostic("Exceção no login SUAP: ${e.javaClass.simpleName}.")
            AppLog.e("SuapAuthRepository", "Erro durante login no SUAP", e)
            NetworkResult.Error("Erro de conexão com o SUAP: ${e.message}", e)
        }
    }

    /** Re-authenticates at most once and validates both the original and retried response. */
    suspend fun executeRequest(requestBuilder: Request.Builder): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = requestBuilder.build()
            if (!request.url.isHttps || request.url.host != "suap.ifal.edu.br") {
                return@withContext NetworkResult.Error("Endereço bloqueado: as requisições autenticadas só podem acessar o SUAP oficial.")
            }
            val page = fetchPage(request)
            if (page.requiresLogin()) {
                diagnostic("Sessão SUAP recusada: status=${page.code}, rotaSolicitada=${safeRoute(request.url)}, rotaFinal=${page.safeRoute()}.")
                return@withContext NetworkResult.Error("Sessão do SUAP expirada. Entre novamente no app para continuar.")
            }
            when {
                !page.isSuccessful -> NetworkResult.Error("O SUAP não concluiu a solicitação. Código: ${page.code}")
                page.html.isBlank() -> NetworkResult.Error("O SUAP retornou uma resposta vazia. Tente novamente.")
                else -> NetworkResult.Success(page.html)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLog.e("SuapAuthRepository", "Erro na execução da requisição SUAP", e)
            NetworkResult.Error("Falha na comunicação com o SUAP: ${e.message}", e)
        }
    }

    private fun fetchPage(request: Request): Page = client.newCall(request).execute().use { response ->
        Page(response.body?.string().orEmpty(), response.request.url, response.code, response.isSuccessful)
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
