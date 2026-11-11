package com.example.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

object SuapClient {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * WebKit CookieManager adapter so that OkHttp calls and WebViews share 
     * the exact same authentication cookies seamlessly.
     */
    val webKitCookieJar = object : CookieJar {
        private val cookieManager = CookieManager.getInstance()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (url.scheme != "https" || url.host != SUAP_HOST) return
            val urlString = url.toString()
            for (cookie in cookies) {
                cookieManager.setCookie(urlString, cookie.toString())
            }
            cookieManager.flush()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (url.scheme != "https" || url.host != SUAP_HOST) return emptyList()
            val urlString = url.toString()
            val rawCookies = cookieManager.getCookie(urlString) ?: return emptyList()
            return rawCookies.split(";").mapNotNull { part ->
                Cookie.parse(url, part.trim())
            }
        }
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("User-Agent", USER_AGENT)
        val request = requestBuilder.build()
        chain.proceed(request)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            // Never forward authenticated requests across a TLS downgrade.
            .followSslRedirects(false)
            .cookieJar(webKitCookieJar)
            .addInterceptor(userAgentInterceptor)
            .addNetworkInterceptor { chain ->
                val url = chain.request().url
                if (url.scheme != "https" || url.host != SUAP_HOST) {
                    throw IOException("Requisição autenticada fora do host oficial do SUAP bloqueada.")
                }
                chain.proceed(chain.request())
            }
            .build()
    }

    private const val SUAP_HOST = "suap.ifal.edu.br"
}
