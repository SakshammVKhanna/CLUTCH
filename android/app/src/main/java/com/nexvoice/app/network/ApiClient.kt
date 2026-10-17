package com.nexvoice.app.network

import android.util.Log
import com.nexvoice.app.auth.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ApiClient.kt
 *
 * Creates and holds the single Retrofit instance for the whole app.
 * Using `object` makes this a Singleton — only one instance ever exists.
 *
 * OkHttpClient is the underlying HTTP engine.
 * We add a LoggingInterceptor so you can see full request/response logs
 * in Android Studio's Logcat — very useful for debugging.
 *
 * GsonConverterFactory automatically converts JSON ↔ Kotlin data classes.
 */
object ApiClient {

    private const val TAG = "ApiClient"

    // The Retrofit instance — created lazily (only when first accessed)
    val api: ApiService by lazy {

        // Logging — shows HTTP requests/responses in Logcat
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            // BODY = log full request + response including JSON body
            // Change to Level.NONE in production to avoid leaking sensitive data
        }

        // ── Session expiry interceptor ────────────────────────────────────────
        // Har API call yahin se guzarti hai. Agar 401 aaya (token expire/invalid)
        // to SessionManager ko batao — NavGraph phir token clear karke user ko
        // seedha Login screen pe bhej dega. Ek jagah fix, poori app cover.
        //
        // Dhyaan: /api/auth/* ko chhod dete hain, kyunki unka 401 "session expire"
        // nahi hota:
        //   /login, /signup → galat password / duplicate email
        //   /me             → yehi wo "session probe" hai jo Splash khud call karta hai;
        //                     uska 401 SplashViewModel already handle karta hai.
        //                     Yahan se bhi bhejte to Splash + NavGraph dono ek saath
        //                     Auth pe navigate karte (double navigation).
        val sessionExpiry = okhttp3.Interceptor { chain ->
            val request  = chain.request()
            val response = chain.proceed(request)

            val path = request.url.encodedPath
            val isAuthCall = path.endsWith("/api/auth/login") ||
                             path.endsWith("/api/auth/signup") ||
                             path.endsWith("/api/auth/me")

            if (response.code == 401 && !isAuthCall) {
                Log.w(TAG, "401 on $path → session expired, sending user to Login")
                SessionManager.notifyExpired()
            }
            response
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(sessionExpiry)
            .build()

        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)    // From NetworkConfig.kt
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
