package javscraper.io.logging

import mu.KotlinLogging
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLogControllerTest {

    @Test
    fun `file logging writes cached and subsequent entries`() {
        val directory = createTempDirectory("javscraper-logs")
        val logFile = directory.resolve("javscraper.log")
        val controller = AppLogController(logFile)
        val logger = KotlinLogging.logger("javscraper.test.logging")

        try {
            logger.info { "cached-before-enabling" }
            assertTrue(controller.entries.any { it.message.contains("cached-before-enabling") })

            controller.setFileLoggingEnabled(true)
            logger.warn { "subsequent-after-enabling" }

            assertTrue(logFile.toFile().exists())
            val content = logFile.readText()
            assertTrue(content.contains("cached-before-enabling"))
            assertTrue(content.contains("subsequent-after-enabling"))
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun `file logging disabled keeps file absent`() {
        val directory = createTempDirectory("javscraper-logs-disabled")
        val logFile = directory.resolve("javscraper.log")
        val controller = AppLogController(logFile)

        try {
            KotlinLogging.logger("javscraper.test.disabled").info { "memory-only-entry" }
            assertFalse(logFile.toFile().exists())
            assertTrue(controller.entries.any { it.message.contains("memory-only-entry") })
        } finally {
            controller.dispose()
        }
    }
}