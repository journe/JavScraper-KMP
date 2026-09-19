package javscraper.scrape.update

import javscraper.models.ScannedFile
import javscraper.models.VideoUpdateField
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateModeExistingMetadataReaderTest {
    @Test
    fun `reads old video and distinguishes missing fields from empty elements`() {
        val folder = createTempDirectory("field-update-reader")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")
        folder.resolve("ABC-001.nfo").writeText(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <num>ABC-001</num>
                  <title>Old title</title>
                  <plot></plot>
                  <genre>Old genre</genre>
                  <tag>Old tag</tag>
                  <actor><name>Alice</name></actor>
                  <javdb_extra><want_count>12</want_count></javdb_extra>
                </movie>
            """.trimIndent()
        )

        val result = UpdateModeExistingMetadataReader().read(
            listOf(ScannedFile(source.toString(), "ABC-001.mp4", "ABC-001"))
        )

        val ready = assertIs<ExistingMetadataResult.Ready>(result)
        assertEquals("ABC-001", ready.metadata.video.number)
        assertTrue(VideoUpdateField.NUMBER in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.SUMMARY in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.TAGS in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.ACTRESSES in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.WANT_COUNT in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.MAKER !in ready.metadata.presentFields)
        assertTrue(VideoUpdateField.RATING_COUNT !in ready.metadata.presentFields)
        assertTrue(source.toFile().exists())
        assertTrue(folder.resolve("ABC-001.nfo").toFile().exists())
    }

    @Test
    fun `fails when source files are not in one folder`() {
        val first = createTempDirectory("field-update-first")
        val second = createTempDirectory("field-update-second")

        val result = UpdateModeExistingMetadataReader().read(
            listOf(
                ScannedFile(first.resolve("ABC-001.mp4").toString(), "ABC-001.mp4", "ABC-001"),
                ScannedFile(second.resolve("ABC-001-2.mp4").toString(), "ABC-001-2.mp4", "ABC-001")
            )
        )

        val failed = assertIs<ExistingMetadataResult.Failed>(result)
        assertTrue(failed.errors.single().contains("one folder"))
    }

    @Test
    fun `fails when nfo is missing`() {
        val folder = createTempDirectory("field-update-no-nfo")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")

        val result = UpdateModeExistingMetadataReader().read(
            listOf(ScannedFile(source.toString(), "ABC-001.mp4", "ABC-001"))
        )

        val failed = assertIs<ExistingMetadataResult.Failed>(result)
        assertTrue(failed.errors.single().contains("NFO"))
    }

    @Test
    fun `fails when nfo xml is invalid`() {
        val folder = createTempDirectory("field-update-invalid-xml")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")
        folder.resolve("ABC-001.nfo").writeText("<movie><title>Broken</title>")

        val result = UpdateModeExistingMetadataReader().read(
            listOf(ScannedFile(source.toString(), "ABC-001.mp4", "ABC-001"))
        )

        val failed = assertIs<ExistingMetadataResult.Failed>(result)
        assertTrue(failed.errors.single().contains("read"))
    }
}