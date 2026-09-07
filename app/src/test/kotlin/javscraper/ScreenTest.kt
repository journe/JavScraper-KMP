package javscraper

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenTest {

    @Test
    fun `navigation includes network preview screen`() {
        assertEquals(Screen.NETWORK_PREVIEW, Screen.valueOf("NETWORK_PREVIEW"))
    }
}