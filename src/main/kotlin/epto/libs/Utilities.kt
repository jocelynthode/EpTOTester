package epto.libs

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Delegate Property object used to instantiate loggers
 */
object Utilities {

    private val logger by logger()

    /**
     * Creates a logger
     *
     * @return a lazy instanciating a logger on the right class
     */
    fun <T : Any> T.logger(): Lazy<Logger> {
        return lazy { LogManager.getLogger(this.javaClass) }
    }

    /**
     * Returns the separation in milliseconds before we have to execute the task
     *
     * @param date the date at which to execute in millis
     *
     * @return the delay to scheduleAt
     */
    fun scheduleAt(date: Long): Long {
        if (date < System.currentTimeMillis()) {
            logger.warn("Time given was smaller than current time, running EpTO immediately, but some events might get lost")
            return 0
        } else {
            logger.warn("EpTo will start at {} UTC+2",
                    LocalDateTime.ofEpochSecond((date / 1000), 0, ZoneOffset.ofHours(2)))
            return (date - System.currentTimeMillis())
        }
    }

}