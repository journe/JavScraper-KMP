package javscraper.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JavdbLoginControllerTest {
    class FakeClient(
        private val cookie: String? = "session-cookie",
        private val failure: Exception? = null,
        private val waitForever: Boolean = false
    ) : JavdbBrowserLoginClient {
        var requestedBaseUrl: String? = null
        var observedPages = 0

        override suspend fun waitForSessionCookie(
            baseUrl: String,
            onPage: suspend (JavdbLoginPage) -> Unit
        ): String {
            requestedBaseUrl = baseUrl
            onPage(
                JavdbLoginPage(
                    url = "$baseUrl/users/new",
                    hasProfileLink = false,
                    hasLogoutLink = false
                )
            )
            if (waitForever) {
                delay(Long.MAX_VALUE)
                throw CancellationException()
            }
            failure?.let { throw it }
            return cookie ?: error("JavDB login cookie was not found")
        }
    }

    @Test
    fun `successful login saves cookie and reports success`() = runControllerTest(
        FakeClient()
    ) { controller, client ->
        val saved = mutableListOf<String>()
        controller.start("https://javdb580.com") { saved.add(it) }
        advanceUntilIdle()

        assertEquals("https://javdb580.com", client.requestedBaseUrl)
        assertEquals(listOf("session-cookie"), saved)
        assertEquals(JavdbLoginStatus.SUCCESS, controller.state.value.status)
        assertTrue(!controller.state.value.running)
    }

    @Test
    fun `client failure reports sanitized error`() = runControllerTest(
        FakeClient(failure = IllegalStateException("browser failed"))
    ) { controller, _ ->
        val saved = mutableListOf<String>()
        controller.start("https://javdb580.com") { saved.add(it) }
        advanceUntilIdle()

        assertEquals(emptyList(), saved)
        assertEquals(JavdbLoginStatus.ERROR, controller.state.value.status)
        assertEquals("browser failed", controller.state.value.error)
    }

    @Test
    fun `cancel returns controller to idle`() = runControllerTest(
        FakeClient(waitForever = true)
    ) { controller, _ ->
        controller.start("https://javdb580.com") {}
        runCurrent()
        assertEquals(JavdbLoginStatus.WAITING_LOGIN, controller.state.value.status)

        controller.cancel()
        advanceUntilIdle()

        assertEquals(JavdbLoginStatus.IDLE, controller.state.value.status)
        assertTrue(!controller.state.value.running)
    }
}
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun runControllerTest(
    client: JavdbLoginControllerTest.FakeClient,
    block: TestScope.(JavdbLoginController, JavdbLoginControllerTest.FakeClient) -> Unit
) = TestScope().runTest {
    val controller = JavdbLoginController(this, client, StandardTestDispatcher(testScheduler))
    try {
        block(controller, client)
    } finally {
        controller.cancel()
        advanceUntilIdle()
    }
}
