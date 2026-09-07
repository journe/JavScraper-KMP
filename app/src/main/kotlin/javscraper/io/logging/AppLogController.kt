package javscraper.io.logging

import ch.qos.logback.classic.Logger
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import mu.KotlinLogging

class AppLogController(
    val logFilePath: Path = Path.of(
        System.getProperty("user.home"),
        ".javscraper",
        "logs",
        "javscraper.log"
    )
) : AutoCloseable {
    private val log = KotlinLogging.logger {}
    private val store = LifecycleLogStore()
    private val appender = LifecycleLogbackAppender(store)
    private val rootLogger = org.slf4j.LoggerFactory.getLogger(
        org.slf4j.Logger.ROOT_LOGGER_NAME
    ) as Logger
    private val fileLock = Any()
    private var fileWriter: BufferedWriter? = null
    private var lastWrittenSequence = 0L

    val entries: List<LogEntry>
        get() = store.entries

    init {
        appender.start()
        rootLogger.addAppender(appender)
        store.addListener(::appendToFile)
    }

    fun setFileLoggingEnabled(enabled: Boolean) {
        val stateChanged = synchronized(fileLock) {
            if (enabled) {
                if (fileWriter != null) {
                    false
                } else {
                    Files.createDirectories(logFilePath.parent)
                    fileWriter = Files.newBufferedWriter(
                        logFilePath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                    )
                    entries
                        .filter { it.sequence > lastWrittenSequence }
                        .forEach(::writeEntry)
                    fileWriter?.flush()
                    true
                }
            } else if (fileWriter == null) {
                false
            } else {
                fileWriter?.close()
                fileWriter = null
                true
            }
        }
        if (stateChanged) {
            log.info { "File logging ${if (enabled) "enabled" else "disabled"}: path=$logFilePath" }
        }
    }

    fun dispose() = close()

    override fun close() {
        log.info { "Lifecycle logging stopped" }
        rootLogger.detachAppender(appender)
        appender.stop()
        synchronized(fileLock) {
            fileWriter?.close()
            fileWriter = null
        }
    }

    private fun appendToFile(entry: LogEntry) {
        synchronized(fileLock) {
            if (fileWriter != null && entry.sequence > lastWrittenSequence) {
                writeEntry(entry)
                fileWriter?.flush()
            }
        }
    }

    private fun writeEntry(entry: LogEntry) {
        val writer = fileWriter ?: return
        writer.write(format(entry))
        writer.newLine()
        lastWrittenSequence = entry.sequence
    }

    private fun format(entry: LogEntry): String {
        val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(entry.timestampMillis))
        val throwable = entry.throwableStack?.let { "$NEWLINE$it" }.orEmpty()
        return "$timestamp [${entry.level}] ${entry.loggerName} - ${entry.message}$throwable"
    }

    private companion object {
        private const val NEWLINE = "\n"
    }
}