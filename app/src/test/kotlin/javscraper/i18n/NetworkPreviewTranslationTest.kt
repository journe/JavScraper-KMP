package javscraper.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkPreviewTranslationTest {

    @Test
    fun `all network preview strings are localized`() {
        val english = TranslationEn()
        val chinese = TranslationZh()

        assertTrue(english.navNetworkPreview.isNotBlank())
        assertTrue(chinese.navNetworkPreview.isNotBlank())
        assertTrue(english.networkPreviewTitle.isNotBlank())
        assertTrue(chinese.networkPreviewTitle.isNotBlank())
        assertTrue(english.networkPreviewEmptyHint.isNotBlank())
        assertTrue(chinese.networkPreviewEmptyHint.isNotBlank())
        assertTrue(english.networkPreviewClear.isNotBlank())
        assertTrue(chinese.networkPreviewClear.isNotBlank())
        assertEquals("3 network results", english.networkPreviewCount(3))
        assertEquals("3 条网络结果", chinese.networkPreviewCount(3))
    }
}