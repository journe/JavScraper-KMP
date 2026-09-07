package javscraper.io.logging

data class LogEntry(
    val sequence: Long,
    val timestampMillis: Long,
    val level: String,
    val loggerName: String,
    val message: String,
    val throwableStack: String? = null
)