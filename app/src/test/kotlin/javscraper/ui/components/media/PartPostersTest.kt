package javscraper.ui.components.media

import javscraper.models.Video
import javscraper.ui.components.media.partposter.listPartEntries
import javscraper.ui.components.media.partposter.listPartPosters
import java.io.File
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

    @Test
    fun `part entries expose each segment video in part order`() {
        val root = createScrapeFolder("part-entries")
        Files.createFile(root.resolve("ABC-123 - cd2.mp4"))
        Files.createFile(root.resolve("ABC-123 - cd1.mp4"))
        Files.writeString(root.resolve("ABC-123 - cd1.nfo"), "<movie><title>Part One</title><num>ABC-123</num></movie>")
        Files.writeString(root.resolve("ABC-123 - cd2.nfo"), "<movie><title>Part Two</title><num>ABC-123</num></movie>")

        val entries = listPartEntries(videoAt(root.resolve("ABC-123 - cd2.mp4")))

        assertEquals(
            listOf("ABC-123 - cd1.mp4", "ABC-123 - cd2.mp4"),
            entries.map { File(it.video.path).name }
        )
        assertEquals("Part One", entries[0].video.title)
        assertEquals("Part Two", entries[1].video.title)
    }

    @Test
    fun `first part entry prefers movie nfo over part nfo`() {
        val root = createScrapeFolder("first-part-master")
        Files.createFile(root.resolve("ABC-123 - cd1.mp4"))
        Files.createFile(root.resolve("ABC-123 - cd2.mp4"))
        Files.writeString(
            root.resolve("movie.nfo"),
            "<movie><title>Master updated</title><num>ABC-123</num></movie>"
        )
        Files.writeString(root.resolve("ABC-123 - cd1.nfo"), "<movie><title>Old cd1</title><num>ABC-123</num></movie>")
        Files.writeString(root.resolve("ABC-123 - cd2.nfo"), "<movie><title>Part Two</title><num>ABC-123</num></movie>")

        val entries = listPartEntries(videoAt(root.resolve("ABC-123 - cd1.mp4")))

        assertEquals("Master updated", entries[0].video.title)
        assertEquals("Part Two", entries[1].video.title)
    }

    @Test
    fun `part entry falls back to master metadata when part nfo is missing`() {
        val root = createScrapeFolder("part-entry-fallback")
        Files.createFile(root.resolve("ABC-123 - cd1.mp4"))
        Files.createFile(root.resolve("ABC-123 - cd2.mp4"))
        val master = Video(
            number = "ABC-123",
            title = "Master title",
            path = root.resolve("ABC-123 - cd1.mp4").toString()
        )

        val entries = listPartEntries(master)

        assertEquals(listOf("Master title", "Master title"), entries.map { it.video.title })
        assertEquals(root.resolve("ABC-123 - cd2.mp4").toString(), entries[1].video.path)
    }

    private fun createScrapeFolder(name: String): Path = Files.createTempDirectory(name)

    private fun videoAt(path: Path): Video = Video(number = "ABC-123", path = path.toString())
}
