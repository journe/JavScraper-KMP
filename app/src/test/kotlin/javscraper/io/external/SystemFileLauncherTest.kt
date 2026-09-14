package javscraper.io.external

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SystemFileLauncherTest {
    @Test
    fun `open video opens the existing file with system handler`() {
        val directory = Files.createTempDirectory("javscraper-launcher")
        try {
            val video = createFile(directory, "movie.mp4")
            val opened = mutableListOf<File>()
            val launcher = SystemFileLauncher { opened.add(it) }

            val result = launcher.openVideo(video.path)

            assertEquals(true, result)
            assertEquals(listOf(video), opened)
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `open containing directory opens parent for video path`() {
        val directory = Files.createTempDirectory("javscraper-launcher")
        try {
            val video = createFile(directory, "movie.mp4")
            val opened = mutableListOf<File>()
            val launcher = SystemFileLauncher { opened.add(it) }

            val result = launcher.openContainingDirectory(video.path)

            assertEquals(true, result)
            assertEquals(listOf(directory.toFile()), opened)
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `open containing directory accepts directory path directly`() {
        val directory = Files.createTempDirectory("javscraper-launcher")
        try {
            val opened = mutableListOf<File>()
            val launcher = SystemFileLauncher { opened.add(it) }

            val result = launcher.openContainingDirectory(directory.toFile().path)

            assertEquals(true, result)
            assertEquals(listOf(directory.toFile()), opened)
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `missing paths are not opened`() {
        val opened = mutableListOf<File>()
        val launcher = SystemFileLauncher { opened.add(it) }

        assertFalse(launcher.openVideo("missing/movie.mp4"))
        assertFalse(launcher.openContainingDirectory("missing/movie.mp4"))
        assertEquals(emptyList(), opened)
    }

    @Test
    fun `system handler failures are reported as false`() {
        val directory = Files.createTempDirectory("javscraper-launcher")
        try {
            val video = createFile(directory, "movie.mp4")
            val launcher = SystemFileLauncher { throw IllegalStateException("no handler") }

            assertFalse(launcher.openVideo(video.path))
        } finally {
            deleteRecursively(directory)
        }
    }
}

private fun createFile(directory: Path, name: String): File {
    val file = directory.resolve(name).toFile()
    Files.createFile(file.toPath())
    return file
}

private fun deleteRecursively(path: Path) {
    Files.walk(path).use { files ->
        files.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}