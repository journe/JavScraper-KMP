package javscraper.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileScannerTest {

    // --- extractNumber tests ---

    @Test
    fun `extractNumber standard pattern SONE-205`() {
        assertEquals("SONE-205", FileScanner.extractNumber("SONE-205.mp4"))
    }

    @Test
    fun `extractNumber standard pattern ABP-123`() {
        assertEquals("ABP-123", FileScanner.extractNumber("ABP-123.avi"))
    }

    @Test
    fun `extractNumber FC2 pattern direct concatenation`() {
        assertEquals("FC21234567", FileScanner.extractNumber("FC21234567.mp4"))
    }

    @Test
    fun `extractNumber HEYZO pattern`() {
        assertEquals("HEYZO-1234", FileScanner.extractNumber("HEYZO-1234.mkv"))
    }

    @Test
    fun `extractNumber six-digit pattern`() {
        assertEquals("123456-789", FileScanner.extractNumber("123456-789.mp4"))
    }

    @Test
    fun `extractNumber with brackets around site prefix`() {
        assertEquals("", FileScanner.extractNumber("[FC2]1234567.mp4"))
    }

    @Test
    fun `extractNumber lowercase returns uppercase`() {
        assertEquals("SONE-205", FileScanner.extractNumber("sone-205.mkv"))
    }

    @Test
    fun `extractNumber em dash normalized to hyphen`() {
        assertEquals("SONE-205", FileScanner.extractNumber("SONE\u2013205.mp4"))
    }

    @Test
    fun `extractNumber sample file returns empty`() {
        assertEquals("", FileScanner.extractNumber("sample-SONE-205.mp4"))
    }

    @Test
    fun `extractNumber trailer file returns empty`() {
        assertEquals("", FileScanner.extractNumber("SONE-205-trailer.mp4"))
    }

    @Test
    fun `extractNumber no match returns empty`() {
        assertEquals("", FileScanner.extractNumber("home_video.mp4"))
    }

    // --- isVideo tests ---

    @Test
    fun `isVideo mp4 returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.mp4")))
    }

    @Test
    fun `isVideo mkv returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.mkv")))
    }

    @Test
    fun `isVideo avi returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.avi")))
    }

    @Test
    fun `isVideo ts returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.ts")))
    }

    @Test
    fun `isVideo txt returns false`() {
        assertFalse(FileScanner.isVideo(Path.of("test.txt")))
    }

    @Test
    fun `isVideo jpg returns false`() {
        assertFalse(FileScanner.isVideo(Path.of("test.jpg")))
    }

    @Test
    fun `isVideo no extension returns false`() {
        assertFalse(FileScanner.isVideo(Path.of("test")))
    }

    @Test
    fun `isVideo mov returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.mov")))
    }

    @Test
    fun `isVideo webm returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.webm")))
    }

    @Test
    fun `isVideo wmv returns true`() {
        assertTrue(FileScanner.isVideo(Path.of("test.wmv")))
    }

    // --- isSample tests ---

    @Test
    fun `isSample sample returns true`() {
        assertTrue(FileScanner.isSample("sample-SONE-205.mp4"))
    }

    @Test
    fun `isSample trailer returns true`() {
        assertTrue(FileScanner.isSample("SONE-205-trailer.mp4"))
    }

    @Test
    fun `isSample normal file returns false`() {
        assertFalse(FileScanner.isSample("SONE-205.mp4"))
    }

    @Test
    fun `isSample cover returns true`() {
        assertTrue(FileScanner.isSample("cover.jpg"))
    }

    // --- scanDirectory tests ---

    @Test
    fun `scanDirectory nonexistent dir returns empty`() {
        val result = FileScanner.scanDirectory(Path.of("Z:/nonexistent/path"))
        assertEquals(0, result.size)
    }

    @Test
    fun `scanDirectory empty dir returns empty list`() {
        val tmpDir = Files.createTempDirectory("javscanner-test-")
        try {
            val result = FileScanner.scanDirectory(tmpDir, recursive = false)
            assertEquals(0, result.size)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `scanDirectory finds video files`() {
        val tmpDir = Files.createTempDirectory("javscanner-test-")
        try {
            val videoFile = tmpDir.resolve("SONE-205.mp4")
            Files.createFile(videoFile)
            val txtFile = tmpDir.resolve("readme.txt")
            Files.createFile(txtFile)
            val result = FileScanner.scanDirectory(tmpDir, recursive = false)
            assertEquals(1, result.size)
            assertEquals("SONE-205", result[0].number)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `scanDirectory marks matching nfo as scraped and missing nfo as pending`() {
        val tmpDir = Files.createTempDirectory("javscanner-test-")
        try {
            val scrapedVideo = tmpDir.resolve("SONE-205.mp4")
            Files.createFile(scrapedVideo)
            Files.createFile(tmpDir.resolve("SONE-205.nfo"))
            val pendingVideo = tmpDir.resolve("ABP-123.mp4")
            Files.createFile(pendingVideo)
            Files.createFile(tmpDir.resolve("OTHER-001.nfo"))

            val result = FileScanner.scanDirectory(tmpDir, recursive = false)

            assertEquals(2, result.size)
            assertEquals(true, result.first { it.fileName == "SONE-205.mp4" }.isScraped)
            assertEquals(false, result.first { it.fileName == "ABP-123.mp4" }.isScraped)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `scanDirectory recursive finds nested videos`() {
        val tmpDir = Files.createTempDirectory("javscanner-test-")
        try {
            val subDir = tmpDir.resolve("subfolder")
            Files.createDirectories(subDir)
            val videoFile = subDir.resolve("ABP-123.mkv")
            Files.createFile(videoFile)
            val result = FileScanner.scanDirectory(tmpDir, recursive = true)
            assertEquals(1, result.size)
            assertEquals("ABP-123", result[0].number)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `scanDirectory includes sample files but with empty number`() {
        val tmpDir = Files.createTempDirectory("javscanner-test-")
        try {
            val sampleFile = tmpDir.resolve("sample-SONE-205.mp4")
            Files.createFile(sampleFile)
            val goodFile = tmpDir.resolve("SONE-206.mp4")
            Files.createFile(goodFile)
            val result = FileScanner.scanDirectory(tmpDir, recursive = false)
            assertEquals(2, result.size, "Both video files are found; samples not filtered")
            val sampleResult = result.find { it.number.isEmpty() }
            assertTrue(sampleResult != null, "Sample file should have empty number")
            assertEquals("SONE-206", result.find { it.number == "SONE-206" }?.number)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
