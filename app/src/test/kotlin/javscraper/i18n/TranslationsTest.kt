package javscraper.i18n

import java.lang.reflect.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationsTest {
    @Test
    fun `common labels reuse shared translation keys`() {
        val english = TranslationEn()
        val chinese = TranslationZh()
        assertEquals("Scraping", english.commonScraping)
        assertEquals("刮削", chinese.commonScraping)
        assertEquals("Scrape Results", english.commonScrapeResults)
        assertEquals("刮削结果", chinese.commonScrapeResults)
        assertEquals("Reset to Defaults", english.commonResetToDefaults)
        assertEquals("恢复默认", chinese.commonResetToDefaults)
    }

    @Test
    fun `part videos label is localized`() {
        assertEquals("Part Videos", TranslationEn().commonPartVideos)
        assertEquals("分段视频", TranslationZh().commonPartVideos)
    }

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

    @Test
    fun `video metadata edit texts are localized`() {
        assertEquals("Edit metadata", TranslationEn().galleryDetailEdit)
        assertEquals("编辑元数据", TranslationZh().galleryDetailEdit)
        assertEquals("Edit Video Metadata", TranslationEn().galleryEditTitle)
        assertEquals("编辑视频元数据", TranslationZh().galleryEditTitle)
        assertEquals("Save", TranslationEn().commonSave)
        assertEquals("保存", TranslationZh().commonSave)
        assertEquals("Saving...", TranslationEn().commonSaving)
        assertEquals("保存中...", TranslationZh().commonSaving)
        assertEquals("No matching NFO file was found", TranslationEn().metadataEditNfoMissing)
        assertEquals("未找到匹配的 NFO 文件", TranslationZh().metadataEditNfoMissing)
        assertEquals(
            "Failed to save metadata: disk error",
            TranslationEn().metadataEditSaveFailed("disk error")
        )
        assertEquals("保存元数据失败：disk error", TranslationZh().metadataEditSaveFailed("disk error"))
        assertEquals("Check number, duration, and rating", TranslationEn().metadataEditInvalidFields)
        assertEquals("请检查番号、时长和评分", TranslationZh().metadataEditInvalidFields)
        assertEquals("Metadata saved", TranslationEn().statusMetadataSaved)
        assertEquals("元数据已保存", TranslationZh().statusMetadataSaved)
    }

    @Test
    fun `poster watermark texts are localized`() {
        assertEquals("Add watermark", TranslationEn().cropWatermarkEnabled)
        assertEquals("添加水印", TranslationZh().cropWatermarkEnabled)
        assertEquals("Censored", TranslationEn().cropWatermarkCensored)
        assertEquals("有码", TranslationZh().cropWatermarkCensored)
        assertEquals("Watermark size: 5", TranslationEn().cropWatermarkSize(5))
        assertEquals("水印大小：5", TranslationZh().cropWatermarkSize(5))
        assertEquals("无", TranslationZh().cropWatermarkNone)
    }

    @Test
    fun `poster crop entry is localized`() {
        assertEquals("Crop Poster", TranslationEn().galleryDetailCropPoster)
        assertEquals("裁剪封面", TranslationZh().galleryDetailCropPoster)
    }

    private fun stringFields(translation: TranslationEn): Map<String, String> =
        translation.javaClass.declaredFields
            .filter { field: Field -> field.type == String::class.java }
            .associate { field: Field ->
                field.isAccessible = true
                field.name to field.get(translation) as String
            }
}
