package epto.libs

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Delegate Property object used to instantiate loggers
 */
object Utilities {

    val logger by logger()

    fun <T : Any> T.logger(): Lazy<Logger> {
        return lazy { LogManager.getLogger(this.javaClass) }
    }

    fun scheduleAt(date: Long): Long {
        if (date < System.currentTimeMillis()) {
            logger.warn("Time given was smaller than current time, running EpTO immediately, but some events might get lost")
            return 0
        } else {
            logger.debug("EpTo will start at ${LocalDateTime.ofEpochSecond((date / 1000), 0, ZoneOffset.UTC)}")
            // add between 0 and 500ms
            return (date - System.currentTimeMillis() + ((Math.random() / 2f) * 1000).toLong())
        }
    }

}