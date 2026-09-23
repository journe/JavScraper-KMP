package javscraper.ui.components.media

import javscraper.models.Video
import javscraper.ui.components.media.partposter.listPartPosters
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class PartPostersTest {
    @Test
    fun `single video has no part poster carousel`() {
        val root = createScrapeFolder("single")
        Files.createFile(root.resolve("ABC-123.mp4"))
        Files.writeString(root.resolve("poster.jpg"), "poster")

        val posters = listPartPosters(videoAt(root.resolve("ABC-123.mp4")))

        assertEquals(emptyList(), posters)
    }

    @Test
    fun `part posters follow video order and prefer part-specific artwork`() {
        val root = createScrapeFolder("ordered-parts")
        Files.createFile(root.resolve("ABC-123 - cd2.mp4"))
        Files.createFile(root.resolve("ABC-123 - cd1.mp4"))
        Files.writeString(root.resolve("poster.jpg"), "generic poster")
        Files.writeString(root.resolve("ABC-123 - cd2-poster.jpg"), "cd2 poster")

        val posters = listPartPosters(videoAt(root.resolve("ABC-123 - cd2.mp4")))

        assertEquals(
            listOf(
                root.resolve("poster.jpg").toFile(),
                root.resolve("ABC-123 - cd2-poster.jpg").toFile()
            ),
            posters
        )
    }

    @Test
    fun `segments without available posters are skipped`() {
        val root = createScrapeFolder("missing-part-poster")
        Files.createFile(root.resolve("ABC-123 - cd1.mp4"))
        Files.createFile(root.resolve("ABC-123 - cd2.mp4"))
        Files.writeString(root.resolve("ABC-123 - cd2-poster.jpg"), "cd2 poster")

        val posters = listPartPosters(videoAt(root.resolve("ABC-123 - cd1.mp4")))

        assertEquals(listOf(root.resolve("ABC-123 - cd2-poster.jpg").toFile()), posters)
    }

    private fun createScrapeFolder(name: String): Path = Files.createTempDirectory(name)

    private fun videoAt(path: Path): Video = Video(number = "ABC-123", path = path.toString())
}