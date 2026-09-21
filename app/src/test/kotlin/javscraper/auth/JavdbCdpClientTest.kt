package javscraper.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavdbCdpClientTest {
    @Test
    fun `runtime request evaluates expression and returns value`() {
        val payload = runtimeEvaluateRequest(7, "JSON.stringify({ok:true})")

        assertTrue(payload.contains("\"id\":7"), payload)
        assertTrue(payload.contains("\"method\":\"Runtime.evaluate\""), payload)
        assertTrue(payload.contains("\"returnByValue\":true"), payload)
        assertTrue(payload.contains("JSON.stringify({ok:true})"), payload)
    }

    @Test
    fun `cookie request limits cookies to target url`() {
        val payload = networkCookiesRequest(9, "https://javdb580.com/")

        assertTrue(payload.contains("\"id\":9"), payload)
        assertTrue(payload.contains("\"method\":\"Network.getCookies\""), payload)
        assertTrue(payload.contains("\"urls\":[\"https://javdb580.com/\"]"), payload)
    }

    @Test
    fun `evaluated value is unwrapped from cdp response`() {
        val value = cdpEvaluatedValue(
            """{"result":{"result":{"type":"string","value":"{\"loggedIn\":false}"}}}"""
        )

        assertEquals("""{"loggedIn":false}""", value)
    }
}
