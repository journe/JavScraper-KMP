package javscraper.ui.components.media

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import androidx.compose.ui.unit.dp
import javscraper.models.Video
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PosterCardTest {

    @Test
    fun `local fanart model is returned when downloaded`() {
        val directory = Files.createTempDirectory("javscraper")
        val fanart = directory.resolve("fanart.jpg")
        try {
            Files.createFile(fanart)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(fanart.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model prefers jpg over png files`() {
        val directory = Files.createTempDirectory("javscraper")
        val jpg = directory.resolve("fanart.jpg")
        val png = directory.resolve("fanart.png")
        try {
            Files.createFile(jpg)
            Files.createFile(png)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(jpg.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model falls back to poster when fanart missing`() {
        val directory = Files.createTempDirectory("javscraper")
        val poster = directory.resolve("poster.jpg")
        try {
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(poster.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model prefers fanart over poster`() {
        val directory = Files.createTempDirectory("javscraper")
        val fanart = directory.resolve("fanart.jpg")
        val poster = directory.resolve("poster.jpg")
        try {
            Files.createFile(fanart)
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(fanart.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model is null when missing`() {
        val video = Video(number = "ABP-123", path = "D:/videos/ABP-123.mp4")

        assertNull(localFanartModel(video))
    }

    @Test
    fun `read image dimensions reports real image size without full decode`() {
        val directory = Files.createTempDirectory("javscraper")
        val fanart = directory.resolve("fanart.jpg")
        try {
            assertTrue(
                ImageIO.write(BufferedImage(800, 538, BufferedImage.TYPE_INT_RGB), "jpg", fanart.toFile())
            )

            assertEquals(800 to 538, readImageDimensions(fanart.toFile()))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `read image dimensions is null for non-image file`() {
        val directory = Files.createTempDirectory("javscraper")
        val junk = directory.resolve("fanart.jpg")
        try {
            Files.write(junk, byteArrayOf(1, 2, 3))

            assertNull(readImageDimensions(junk.toFile()))
        } finally {
            deleteRecursively(directory)
        }
    }
    @Test
    fun `local poster model accepts same-name variant`() {
        val directory = Files.createTempDirectory("javscraper")
        val poster = directory.resolve("FC2-3264420-poster.jpg")
        try {
            Files.createFile(poster)
            val video = Video(
                number = "FC2-3264420",
                path = directory.resolve("FC2-3264420.mp4").toString()
            )

            assertEquals(poster.toFile(), localPosterModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model accepts same-name variant`() {
        val directory = Files.createTempDirectory("javscraper")
        val fanart = directory.resolve("FC2-3264420-fanart.jpg")
        try {
            Files.createFile(fanart)
            val video = Video(
                number = "FC2-3264420",
                path = directory.resolve("FC2-3264420.mp4").toString()
            )

            assertEquals(fanart.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local fanart model prefers same-name variant over generic fanart`() {
        val directory = Files.createTempDirectory("javscraper")
        val sameName = directory.resolve("ABP-123 - cd2-fanart.jpg")
        val generic = directory.resolve("fanart.jpg")
        try {
            Files.createFile(sameName)
            Files.createFile(generic)
            val video = Video(
                number = "ABP-123",
                path = directory.resolve("ABP-123 - cd2.mp4").toString()
            )

            assertEquals(sameName.toFile(), localFanartModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local poster path is resolved beside the video`() {
        val path = localPosterPath(Video(number = "ABP-123", path = "D:/videos/ABP-123.mp4"))

        assertEquals("D:/videos/poster.jpg", path.replace('\\', '/'))
    }

    @Test
    fun `local poster model is returned when downloaded`() {
        val directory = Files.createTempDirectory("javscraper")
        val poster = directory.resolve("poster.jpg")
        try {
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(poster.toFile(), localPosterModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local poster model supports png files`() {
        val directory = Files.createTempDirectory("javscraper")
        val poster = directory.resolve("poster.png")
        try {
            Files.createFile(poster)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(poster.toFile(), localPosterModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local poster model prefers jpg over png files`() {
        val directory = Files.createTempDirectory("javscraper")
        val jpg = directory.resolve("poster.jpg")
        val png = directory.resolve("poster.png")
        try {
            Files.createFile(jpg)
            Files.createFile(png)
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertEquals(jpg.toFile(), localPosterModel(video))
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local poster model is loadable by coil`() {
        val directory = Files.createTempDirectory("javscraper")
        val poster = directory.resolve("poster.jpg")
        try {
            assertTrue(ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", poster.toFile()))
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())
            val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()

            val result = runBlocking {
                imageLoader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(localPosterModel(video)).build())
            }

            assertNotNull(result.image)
            imageLoader.shutdown()
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `local poster model is null when missing`() {
        val video = Video(number = "ABP-123", path = "D:/videos/ABP-123.mp4")

        assertNull(localPosterModel(video))
    }

    @Test
    fun `local poster model is null without a parent directory`() {
        assertNull(localPosterModel(Video(number = "ABP-123", path = "ABP-123.mp4")))
    }
}

private fun deleteRecursively(path: Path) {
    Files.walk(path).use { files ->
        files.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
