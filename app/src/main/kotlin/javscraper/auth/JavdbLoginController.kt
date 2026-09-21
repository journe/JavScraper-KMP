package javscraper.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.URI

interface JavdbBrowserLoginClient {
    suspend fun waitForSessionCookie(
        baseUrl: String,
        onPage: suspend (JavdbLoginPage) -> Unit
    ): String
}

class JavdbLoginController(
    private val scope: CoroutineScope,
    private val client: JavdbBrowserLoginClient = JavdbBrowserCdpLoginClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutableState = MutableStateFlow(JavdbLoginState())
    val state: StateFlow<JavdbLoginState> = mutableState
    private var job: Job? = null

    fun start(baseUrl: String, onCookie: (String) -> Unit) {
        if (mutableState.value.running) return
        mutableState.value = JavdbLoginState(JavdbLoginStatus.WAITING_BROWSER)
        job = scope.launch(dispatcher) {
            try {
                val cookie = client.waitForSessionCookie(baseUrl) { page ->
                    mutableState.value = JavdbLoginState(
                        JavdbLoginStatus.WAITING_LOGIN,
                        page.url
                    )
                }
                mutableState.value = JavdbLoginState(JavdbLoginStatus.SAVING)
                onCookie(cookie)
                mutableState.value = JavdbLoginState(JavdbLoginStatus.SUCCESS)
            } catch (error: CancellationException) {
                mutableState.value = JavdbLoginState()
            } catch (error: Exception) {
                mutableState.value = JavdbLoginState(
                    JavdbLoginStatus.ERROR,
                    error = error.message ?: "JavDB login failed"
                )
            }
        }
    }

    fun cancel() {
        job?.cancel(CancellationException("JavDB login cancelled"))
    }
}

class JavdbBrowserCdpLoginClient : JavdbBrowserLoginClient {
    override suspend fun waitForSessionCookie(
        baseUrl: String,
        onPage: suspend (JavdbLoginPage) -> Unit
    ): String {
        val loginUrl = resolveJavdbLoginUrl(baseUrl)
        val baseUri = URI(baseUrl.trim().removeSuffix("/"))
        val cookieUrl = URI(
            baseUri.scheme,
            baseUri.userInfo,
            baseUri.host,
            baseUri.port,
            "/",
            null,
            null
        ).toString()
        val session = JavdbBrowserLauncher.launch(loginUrl = loginUrl)
        try {
            val endpoint = session.waitForEndpoint()
            val cdpClient = JavdbCdpClient.connect(endpoint, loginUrl)
            try {
                while (true) {
                    val page = parseJavdbLoginPage(cdpClient.evaluate(LOGIN_PAGE_EXPRESSION))
                    onPage(page)
                    if (page.loggedIn) {
                        val cookie = extractJavdbSessionCookie(
                            cdpClient.cookies(cookieUrl),
                            baseUri.host
                        )
                        if (cookie.isNullOrBlank()) {
                            error("JavDB login cookie was not found")
                        }
                        return cookie
                    }
                    delay(1_000)
                }
            } finally {
                cdpClient.close()
            }
        } finally {
            session.close()
        }
    }

    private companion object {
        val LOGIN_PAGE_EXPRESSION = """
            JSON.stringify({
              url: location.href,
              hasProfileLink: Boolean(document.querySelector('a[href="/users/profile"]')),
              hasLogoutLink: Boolean(document.querySelector('a[href*="logout"]'))
            })
        """.trimIndent()
    }
}
