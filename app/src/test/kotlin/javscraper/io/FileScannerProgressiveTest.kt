package javscraper.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class FileScannerProgressiveTest {
    @Test
    fun `scanDirectoryFlow emits scanned files in batches`() {
        val dir = Files.createTempDirectory("javscraper-progressive-scan-")
        try {
            repeat(55) { index ->
                val stem = "ABC-${index.toString().padStart(3, '0')}"
                Files.createFile(dir.resolve("$stem.mp4"))
            }

            val batches = runBlocking {
                FileScanner.scanDirectoryFlow(dir, recursive = false, batchSize = 20).toList()
            }

            assertEquals(listOf(20, 20, 15), batches.map { it.size })
            assertEquals(55, batches.sumOf { it.size })
            assertEquals("ABC-000", batches.first().first().number)
            assertEquals("ABC-054", batches.last().last().number)
        } finally {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}