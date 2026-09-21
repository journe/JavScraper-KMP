package javscraper.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class JavdbLoginPage(
    val url: String,
    val hasProfileLink: Boolean = false,
    val hasLogoutLink: Boolean = false
) {
    val loggedIn: Boolean
        get() = hasProfileLink || hasLogoutLink
}

@Serializable
private data class CdpCookiesPayload(val cookies: List<CdpCookiePayload> = emptyList())

@Serializable
private data class CdpCookiePayload(
    val name: String,
    val value: String,
    val domain: String = ""
)

private val json = Json { ignoreUnknownKeys = true }

fun parseJavdbLoginPage(payload: String): JavdbLoginPage =
    json.decodeFromString(JavdbLoginPage.serializer(), payload)

fun extractJavdbSessionCookie(payload: String, host: String): String? {
    val cookies = json.decodeFromString(CdpCookiesPayload.serializer(), payload).cookies
    val normalizedHost = host.lowercase().removePrefix(".")
    return cookies
        .firstOrNull { cookie ->
            cookie.name == "_jdb_session" && cookieDomainMatches(normalizedHost, cookie.domain)
        }
        ?.value
        ?.takeIf { it.isNotBlank() }
}

private fun cookieDomainMatches(host: String, cookieDomain: String): Boolean {
    val normalizedDomain = cookieDomain.lowercase().removePrefix(".")
    return normalizedDomain.isNotBlank() && (
            host == normalizedDomain || host.endsWith(".$normalizedDomain")
            )
}