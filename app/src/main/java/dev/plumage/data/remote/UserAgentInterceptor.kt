package dev.plumage.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * e926 rejects clients that present a browser User-Agent and expects a contact
 * handle in the string so they can reach the operator instead of blanket-banning
 * the app. The username is read fresh on every request so a change in Settings
 * takes effect without rebuilding the OkHttp client.
 */
class UserAgentInterceptor(
    private val usernameProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val username = usernameProvider().trim().ifBlank { DEFAULT_CONTACT }
        val request = chain.request().newBuilder()
            .header("User-Agent", "Plumage/1.0 ($username)")
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val DEFAULT_CONTACT = "unconfigured"
    }
}
