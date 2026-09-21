package javscraper.auth

import java.net.URI

enum class JavdbLoginStatus { IDLE, WAITING_BROWSER, WAITING_LOGIN, SAVING, SUCCESS, ERROR }

data class JavdbLoginState(
    val status: JavdbLoginStatus = JavdbLoginStatus.IDLE,
    val message: String = "",
    val error: String? = null
) {
    val running: Boolean
        get() = status in RUNNING_STATUSES

    private companion object {
        val RUNNING_STATUSES = setOf(
            JavdbLoginStatus.WAITING_BROWSER,
            JavdbLoginStatus.WAITING_LOGIN,
            JavdbLoginStatus.SAVING
        )
    }
}

fun resolveJavdbLoginUrl(baseUrl: String): String {
    val trimmed = baseUrl.trim().removeSuffix("/")
    require(trimmed.isNotBlank()) { "JavDB URL is required" }

    val uri = URI(trimmed)
    val scheme = uri.scheme?.lowercase().orEmpty()
    val host = uri.host?.lowercase().orEmpty()
    require(scheme == "http" || scheme == "https") { "JavDB URL must use http(s)" }
    require(host.isNotBlank()) { "JavDB URL host is required" }
    require(uri.rawQuery == null && uri.rawFragment == null) {
        "JavDB URL must not contain a query or fragment"
    }
    return "$trimmed/login"
}