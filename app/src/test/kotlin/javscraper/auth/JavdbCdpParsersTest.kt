package javscraper.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JavdbCdpParsersTest {
    @Test
    fun `anonymous page is not logged in`() {
        val page = parseJavdbLoginPage(
            """{"url":"https://javdb580.com/users/new","hasProfileLink":false,"hasLogoutLink":false}"""
        )

        assertEquals("https://javdb580.com/users/new", page.url)
        assertFalse(page.loggedIn)
    }

    @Test
    fun `profile or logout link means logged in`() {
        assertTrue(
            parseJavdbLoginPage(
                """{"url":"https://javdb580.com/","hasProfileLink":true,"hasLogoutLink":false}"""
            ).loggedIn
        )
        assertTrue(
            parseJavdbLoginPage(
                """{"url":"https://javdb580.com/","hasProfileLink":false,"hasLogoutLink":true}"""
            ).loggedIn
        )
    }

    @Test
    fun `extracts only javdb session cookie for target host`() {
        val payload = """
            {"cookies":[
              {"name":"theme","value":"auto","domain":".javdb580.com"},
              {"name":"_jdb_session","value":"anonymous-session","domain":".javdb580.com"},
              {"name":"_jdb_session","value":"other-site","domain":".example.com"}
            ]}
        """.trimIndent()

        assertEquals("anonymous-session", extractJavdbSessionCookie(payload, "javdb580.com"))
    }

    @Test
    fun `missing cookie returns null without throwing`() {
        assertNull(extractJavdbSessionCookie("""{"cookies":[]}""", "javdb580.com"))
    }
}