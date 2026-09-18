package javscraper.io.metadata

import javscraper.io.NfoWriter
import javscraper.models.Video
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VideoMetadataEditorTest {

    @Test
    fun `update writes edited metadata to matching nfo and reads it back`() {
        val directory = Files.createTempDirectory("javscraper-metadata-edit")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText(
            NfoWriter.generate(
                Video(
                    number = "ABC-001",
                    title = "Old title",
                    actresses = listOf("Old actor"),
                    tags = listOf("Old tag")
                )
            )
        )
        val edited = Video(
            number = "ABC-001",
            title = "New title",
            actresses = listOf("New actor"),
            tags = listOf("New tag"),
            path = videoPath.toString()
        )

        val result = VideoMetadataEditor.update(edited, lockData = true)

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals("New title", success.video.title)
        assertEquals(listOf("New actor"), success.video.actresses)
        assertEquals(videoPath.toString(), success.video.path)
        val savedNfo = Files.readString(nfoPath)
        assertTrue(savedNfo.contains("<title>New title</title>"))
        assertTrue(savedNfo.contains("<name>New actor</name>"))
        assertTrue(savedNfo.contains("<genre>New tag</genre>"))
    }

    @Test
    fun `update reports missing nfo without creating files`() {
        val directory = Files.createTempDirectory("javscraper-metadata-missing")
        val videoPath = directory.resolve("ABC-001.mp4")

        val result = VideoMetadataEditor.update(
            Video(number = "ABC-001", path = videoPath.toString()),
            lockData = false
        )

        assertIs<VideoMetadataEditResult.NfoMissing>(result)
        assertEquals(0, Files.list(directory).use { it.count() })
    }

    @Test
    fun `update adds fields that were absent in minimal nfo`() {
        val directory = Files.createTempDirectory("javscraper-metadata-add-fields")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText("<movie><title>Old</title><num>ABC-001</num></movie>")
        val edited = Video(
            number = "ABC-001",
            title = "A much longer edited title",
            actresses = listOf("New actor"),
            date = "2026-09-18",
            summary = "New summary",
            maker = "New maker",
            label = "New label",
            series = "New series",
            director = "New director",
            duration = 125,
            rating = 9.5,
            tags = listOf("New tag"),
            source = "JavBus",
            detailUrl = "https://example.com/detail",
            coverUrl = "https://example.com/cover.jpg",
            posterUrl = "https://example.com/poster.jpg",
            sampleImages = listOf("https://example.com/sample.jpg"),
            path = videoPath.toString()
        )

        val result = VideoMetadataEditor.update(edited, lockData = false)

        val savedVideo = assertIs<VideoMetadataEditResult.Success>(result).video
        assertEquals("A much longer edited title", savedVideo.title)
        assertEquals("New actor", savedVideo.actresses.single())
        assertEquals("2026-09-18", savedVideo.date)
        assertEquals("New summary", savedVideo.summary)
        assertEquals("New maker", savedVideo.maker)
        assertEquals("New label", savedVideo.label)
        assertEquals("New series", savedVideo.series)
        assertEquals("New director", savedVideo.director)
        assertEquals(125, savedVideo.duration)
        assertEquals(9.5, savedVideo.rating)
        assertEquals("New tag", savedVideo.tags.single())
        assertEquals("JavBus", savedVideo.source)
        assertEquals("https://example.com/detail", savedVideo.detailUrl)
        assertEquals("https://example.com/cover.jpg", savedVideo.coverUrl)
        assertEquals("https://example.com/poster.jpg", savedVideo.posterUrl)
        assertEquals("https://example.com/sample.jpg", savedVideo.sampleImages.single())
    }
}
