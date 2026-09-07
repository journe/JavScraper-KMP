package javscraper.io.logging

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class LifecycleLogStore {
    var entries by mutableStateOf<List<LogEntry>>(emptyList())
        private set

    private val nextSequence = AtomicLong(0L)
    private val listeners = CopyOnWriteArrayList<(LogEntry) -> Unit>()

    fun record(
        timestampMillis: Long,
        level: String,
        loggerName: String,
        message: String,
        throwableStack: String? = null
    ) {
        val entry = LogEntry(
            sequence = nextSequence.incrementAndGet(),
            timestampMillis = timestampMillis,
            level = level,
            loggerName = loggerName,
            message = message,
            throwableStack = throwableStack
        )
        entries = entries + entry
        listeners.forEach { listener -> listener(entry) }
    }

    fun addListener(listener: (LogEntry) -> Unit) {
        listeners += listener
    }
}