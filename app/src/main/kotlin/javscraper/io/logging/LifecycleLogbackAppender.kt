package javscraper.io.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase

class LifecycleLogbackAppender(
    private val store: LifecycleLogStore
) : AppenderBase<ILoggingEvent>() {
    override fun append(event: ILoggingEvent) {
        val throwableStack = event.throwableProxy?.let { ThrowableProxyUtil.asString(it) }
        store.record(
            timestampMillis = event.timeStamp,
            level = event.level.toString(),
            loggerName = event.loggerName,
            message = event.formattedMessage,
            throwableStack = throwableStack
        )
    }
}