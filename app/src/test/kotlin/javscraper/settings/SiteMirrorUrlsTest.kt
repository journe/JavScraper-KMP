package javscraper.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SiteMirrorUrlsTest {

    @Test
    fun `normalizes mirror urls by trimming whitespace and trailing slashes`() {
        assertEquals(
            "https://www.dmmsee.casa",
            normalizeSiteMirrorUrl("  https://www.dmmsee.casa/  ")
        )
        assertEquals("", normalizeSiteMirrorUrl("   "))
    }

    @Test
    fun `accepts only http and https urls with a host`() {
        assertTrue(isValidSiteMirrorUrl("https://www.dmmsee.casa"))
        assertTrue(isValidSiteMirrorUrl("http://localhost:8080"))
        assertFalse(isValidSiteMirrorUrl("ftp://www.dmmsee.casa"))
        assertFalse(isValidSiteMirrorUrl("https://"))
        assertFalse(isValidSiteMirrorUrl("www.dmmsee.casa"))
    }
}