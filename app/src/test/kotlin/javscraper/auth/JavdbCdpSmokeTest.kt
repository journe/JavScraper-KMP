package javscraper.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs only when JAVDB_CDP_SMOKE=1 so normal tests never launch a browser. */
class JavdbCdpSmokeTest {
    @Test
    fun `edge reads anonymous javdb page and session cookie`() = runBlocking {
        if (System.getenv("JAVDB_CDP_SMOKE") != "1") return@runBlocking

        val baseUrl = "https://javdb580.com"
        val expectedHost = java.net.URI(baseUrl).host
        val loginUrl = resolveJavdbLoginUrl(baseUrl)
        val session = JavdbBrowserLauncher.launch(
            loginUrl = loginUrl,
            headless = true
        )
        try {
            val endpoint = session.waitForEndpoint()
            assertTrue(endpoint.isLocal)
            val client = JavdbCdpClient.connect(endpoint, loginUrl)
            try {
                var page: JavdbLoginPage? = null
                var attempts = 0
                while (page == null && attempts < 40) {
                    val candidate = runCatching {
                        parseJavdbLoginPage(
                            client.evaluate(
                                """JSON.stringify({
                                    url: location.href,
                                    hasProfileLink: Boolean(document.querySelector('a[href="/users/profile"]')),
                                    hasLogoutLink: Boolean(document.querySelector('a[href*="logout"]'))
                                })"""
                            )
                        )
                    }.getOrNull()
                    val candidateHost = candidate?.url?.let { url ->
                        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()
                    }
                    if (candidateHost == expectedHost) page = candidate
                    if (page == null) delay(500)
                    attempts++
                }
                assertNotNull(page, "JavDB page did not become readable")
                assertTrue(!page.loggedIn, "Temporary profile should not be logged in")

                println("devtoolsConnected=true")
                println("targetUrl=${page.url}")
                println("loggedIn=false")
                val cookiePayload = client.cookies(page.url)
                val cookie = extractJavdbSessionCookie(cookiePayload, expectedHost)
                assertNotNull(cookie, "JavDB session cookie was not found")
                println("sessionCookieFound=true")
                println("sessionCookieLength=${cookie.length}")
            } finally {
                client.close()
            }
        } finally {
            session.close()
        }
    }
}
