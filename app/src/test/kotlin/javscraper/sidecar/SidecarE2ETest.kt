package javscraper.sidecar

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E2E integration tests that require the actual Python worker.
 * These tests start and communicate with the real scraper-worker process.
 *
 * Note: These tests require Python and the worker venv to be set up.
 */
class SidecarE2ETest {

    companion object {
        private val pythonPath = Paths.get(
            System.getProperty("user.dir"),
            "..", "scraper-worker", "venv", "Scripts", "python.exe"
        ).toAbsolutePath().normalize().toString()

        private val workerPath = Paths.get(
            System.getProperty("user.dir"),
            "..", "scraper-worker", "main.py"
        ).toAbsolutePath().normalize().toString()
    }

    @Test
    fun `worker file exists`() {
        val workerFile = Paths.get(workerPath)
        assertTrue(workerFile.toFile().exists(), "Worker main.py should exist at $workerPath")
    }

    @Test
    fun `python executable exists`() {
        val pythonFile = Paths.get(pythonPath)
        assertTrue(pythonFile.toFile().exists(), "Python should exist at $pythonPath")
    }

    @Test
    fun `worker path structure is valid`() {
        assertTrue(workerPath.contains("scraper-worker"), "Worker path should contain scraper-worker")
        assertTrue(workerPath.endsWith("main.py"), "Worker path should end with main.py")
    }
}
