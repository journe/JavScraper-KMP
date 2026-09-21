package javscraper.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavdbLoginModelsTest {
    @Test
    fun `login url uses configured mirror root`() {
        assertEquals(
            "https://javdb580.com/login",
            resolveJavdbLoginUrl("https://javdb580.com/")
        )
    }

    @Test
    fun `login url rejects non web input`() {
        assertFailsWith<IllegalArgumentException> { resolveJavdbLoginUrl("") }
        assertFailsWith<IllegalArgumentException> { resolveJavdbLoginUrl("ftp://javdb580.com") }
        assertFailsWith<IllegalArgumentException> { resolveJavdbLoginUrl("https://javdb580.com?x=1") }
    }

    @Test
    fun `default state is idle and not running`() {
        val state = JavdbLoginState()

        assertEquals(JavdbLoginStatus.IDLE, state.status)
        assertFalse(state.running)
        assertTrue(state.error == null)
    }
}