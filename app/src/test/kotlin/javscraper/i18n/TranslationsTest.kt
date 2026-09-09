package javscraper.i18n

import java.lang.reflect.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationsTest {
    @Test
    fun `static texts duplicated in both locales use shared keys`() {
        val english = stringFields(TranslationEn())
        val chinese = stringFields(TranslationZh())
        val duplicatedPairs = english.keys.flatMap { first ->
            english.keys.filter { second ->
                second > first &&
                    english.getValue(first) == english.getValue(second) &&
                    chinese.getValue(first) == chinese.getValue(second)
            }.map { second -> "$first/$second" }
        }

        assertTrue(
            duplicatedPairs.isEmpty(),
            "Duplicated texts should use shared translation keys: $duplicatedPairs"
        )
    }

    @Test
    fun `webpage setting label is localized`() {
        assertEquals("Download webpage", TranslationEn().settingsDownloadWebPages)
        assertEquals("下载网页", TranslationZh().settingsDownloadWebPages)
    }

    @Test
    fun `preview image setting label is localized`() {
        assertEquals("Download preview images", TranslationEn().settingsDownloadPreviewImages)
        assertEquals("下载预览图", TranslationZh().settingsDownloadPreviewImages)
    }

    @Test
    fun `poster crop dialog texts are localized`() {
        assertEquals("Crop Poster - SONE-001", TranslationEn().cropTitle("SONE-001"))
        assertEquals("封面裁剪 - SONE-001", TranslationZh().cropTitle("SONE-001"))
        assertEquals("裁剪并保存", TranslationZh().cropConfirm)
        assertEquals("Source: 800 x 533", TranslationEn().cropSourceSize(800, 533))
        assertEquals("原图：800 × 533", TranslationZh().cropSourceSize(800, 533))
        assertEquals("Output: 355 x 533", TranslationEn().cropOutputSize(355, 533))
        assertEquals("输出：355 × 533", TranslationZh().cropOutputSize(355, 533))
        assertEquals(
            "Aspect ratio (H/W): 1.50",
            TranslationEn().cropAspectRatio(1.5f).replace(",", ".")
        )
        assertTrue(TranslationZh().cropAspectRatio(1.5f).endsWith("1.50"))
    }

    private fun stringFields(translation: TranslationEn): Map<String, String> =
        translation.javaClass.declaredFields
            .filter { field: Field -> field.type == String::class.java }
            .associate { field: Field ->
                field.isAccessible = true
                field.name to field.get(translation) as String
            }
}
