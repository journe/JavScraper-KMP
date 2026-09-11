package javscraper.ui.components

import javscraper.models.Video
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtraFanartCarouselTest {

    @Test
    fun `returns empty when extrafanart directory missing`() {
        val directory = Files.createTempDirectory("javscraper-extra")
        try {
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertTrue(listExtraFanartImages(video).isEmpty())
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `returns empty when extrafanart directory has no images`() {
        val directory = Files.createTempDirectory("javscraper-extra")
        try {
            val extra = directory.resolve("extrafanart")
            Files.createDirectories(extra)
            Files.createFile(extra.resolve("note.txt"))
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            assertTrue(listExtraFanartImages(video).isEmpty())
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `lists image files in natural filename order`() {
        val directory = Files.createTempDirectory("javscraper-extra")
        try {
            val extra = directory.resolve("extrafanart")
            Files.createDirectories(extra)
            // 文件名长度混排:fanart10 应排在 fanart2 之后(自然顺序而非字典序)
            listOf("fanart1.jpg", "fanart2.jpg", "fanart10.jpg", "fanart3.png").forEach {
                Files.createFile(extra.resolve(it))
            }
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            val images = listExtraFanartImages(video)

            assertEquals(
                listOf("fanart1.jpg", "fanart2.jpg", "fanart3.png", "fanart10.jpg"),
                images.map { it.name }
            )
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `ignores subdirectories inside extrafanart`() {
        val directory = Files.createTempDirectory("javscraper-extra")
        try {
            val extra = directory.resolve("extrafanart")
            Files.createDirectories(extra.resolve("nested"))
            Files.createFile(extra.resolve("fanart1.jpg"))
            val video = Video(number = "ABP-123", path = directory.resolve("ABP-123.mp4").toString())

            val images = listExtraFanartImages(video)

            assertEquals(1, images.size)
            assertEquals("fanart1.jpg", images.first().name)
        } finally {
            deleteRecursively(directory)
        }
    }

    private fun deleteRecursively(path: Path) {
        path.toFile().walkBottomUp().forEach { it.delete() }
    }
}
