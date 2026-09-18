package javscraper.ui.components.media

import javscraper.models.Video
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CropSourceModelTest {

    private fun cleanup(dir: Path) {
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }

    @Test
    fun `crop source prefers fanart over poster`() {
        val dir = Files.createTempDirectory("javscraper-cropsrc-")
        try {
            val fanart = dir.resolve("fanart.jpg")
            val poster = dir.resolve("poster.jpg")
            Files.createFile(fanart)
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = dir.resolve("ABP-123.mp4").toString())

            assertEquals(fanart.toFile(), cropSourceModel(video))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop source falls back to poster when fanart missing`() {
        val dir = Files.createTempDirectory("javscraper-cropsrc-")
        try {
            val poster = dir.resolve("poster.png")
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = dir.resolve("ABP-123.mp4").toString())

            assertEquals(poster.toFile(), cropSourceModel(video))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop source is null when no cover exists`() {
        val dir = Files.createTempDirectory("javscraper-cropsrc-")
        try {
            val video = Video(number = "ABP-123", path = dir.resolve("ABP-123.mp4").toString())

            assertNull(cropSourceModel(video))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop source is null without parent directory`() {
        assertNull(cropSourceModel(Video(number = "ABP-123", path = "ABP-123.mp4")))
    }

    @Test
    fun `crop source resolves beside the video`() {
        val dir = Files.createTempDirectory("javscraper-cropsrc-")
        try {
            val fanart = dir.resolve("fanart.jpg")
            Files.createFile(fanart)
            // 视频在子目录中时,裁剪源与 poster 一样取视频同目录,不取父目录
            val videoDir = dir.resolve("movie")
            Files.createDirectories(videoDir)
            val video = Video(number = "ABP-123", path = videoDir.resolve("ABP-123.mp4").toString())

            assertNull(cropSourceModel(video))
            assertNull(localPosterModel(video))
        } finally {
            cleanup(dir)
        }
    }
}
