package javscraper.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaArtPathsTest {

    @Test
    fun `video base name strips the extension`() {
        assertEquals("FC2-3264420", MediaArtPaths.videoBaseName("FC2-3264420.mp4"))
        assertEquals("ABC-001", MediaArtPaths.videoBaseName("ABC-001.mkv"))
        assertEquals("noext", MediaArtPaths.videoBaseName("noext"))
    }

    @Test
    fun `finds generic poster and fanart`() {
        val dir = createTempDirectory("media-art")
        try {
            dir.resolve("poster.jpg").let(Files::createFile)
            dir.resolve("fanart.jpg").let(Files::createFile)

            assertEquals(dir.resolve("poster.jpg"), MediaArtPaths.findPoster(dir, "FC2-3264420"))
            assertEquals(dir.resolve("fanart.jpg"), MediaArtPaths.findFanart(dir, "FC2-3264420"))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `finds same-name poster and fanart variants`() {
        val dir = createTempDirectory("media-art")
        try {
            dir.resolve("FC2-3264420-poster.jpg").let(Files::createFile)
            dir.resolve("FC2-3264420-fanart.jpg").let(Files::createFile)

            assertEquals(
                dir.resolve("FC2-3264420-poster.jpg"),
                MediaArtPaths.findPoster(dir, "FC2-3264420")
            )
            assertEquals(
                dir.resolve("FC2-3264420-fanart.jpg"),
                MediaArtPaths.findFanart(dir, "FC2-3264420")
            )
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `generic names take precedence over same-name variants`() {
        val dir = createTempDirectory("media-art")
        try {
            dir.resolve("poster.jpg").let(Files::createFile)
            dir.resolve("FC2-3264420-poster.jpg").let(Files::createFile)

            assertEquals(dir.resolve("poster.jpg"), MediaArtPaths.findPoster(dir, "FC2-3264420"))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `png variants are accepted`() {
        val dir = createTempDirectory("media-art")
        try {
            dir.resolve("FC2-3264420-fanart.png").let(Files::createFile)

            assertEquals(
                dir.resolve("FC2-3264420-fanart.png"),
                MediaArtPaths.findFanart(dir, "FC2-3264420")
            )
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `jpg is preferred over png within the same naming pattern`() {
        val dir = createTempDirectory("media-art")
        try {
            dir.resolve("fanart.png").let(Files::createFile)
            dir.resolve("FC2-3264420-fanart.jpg").let(Files::createFile)

            // 通用名 png 依然优先于同名变体 jpg
            assertEquals(dir.resolve("fanart.png"), MediaArtPaths.findFanart(dir, "FC2-3264420"))
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `returns null when nothing matches`() {
        val dir = createTempDirectory("media-art")
        try {
            assertNull(MediaArtPaths.findPoster(dir, "FC2-3264420"))
            assertNull(MediaArtPaths.findFanart(dir, "FC2-3264420"))
            assertFalse(Files.exists(dir.resolve("poster.jpg")))
            assertTrue(dir.resolve("other.txt").let { !Files.exists(it) })
        } finally {
            cleanup(dir)
        }
    }

    private fun cleanup(dir: Path) {
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
