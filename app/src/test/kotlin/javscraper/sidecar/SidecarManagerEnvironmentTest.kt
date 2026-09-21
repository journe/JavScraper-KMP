package javscraper.sidecar

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SidecarManagerEnvironmentTest {
    @Test
    fun `start passes configured environment to worker process`() = runBlocking {
        val directory = Files.createTempDirectory("javscraper-worker-env-")
        val batchPath = directory.resolve("worker.bat")
        val environmentPath = directory.resolve("environment.txt")
        batchPath.toFile().writeText(
            """
            @echo off
            set > "${environmentPath.toFile().absolutePath}"
            :loop
            ping -n 2 127.0.0.1 >nul
            goto loop
            """.trimIndent().replace("\n", "\r\n")
        )
        val manager = SidecarManager(
            batchPath.toString(),
            environment = mapOf("JAVDB_SESSION" to "test-session")
        )

        try {
            assertTrue(manager.start(), "Fake worker should start")
            val environment = environmentPath.toFile().readText()
            assertTrue(environment.contains("JAVDB_SESSION=test-session"), environment)
        } finally {
            manager.close()
            directory.toFile().deleteRecursively()
        }
    }
}