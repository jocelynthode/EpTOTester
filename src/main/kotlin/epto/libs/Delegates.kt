package epto.libs

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Delegate Property object used to instantiate loggers
 */
object Delegates {

    fun <T : Any> T.logger(): Lazy<Logger> {
        return lazy { LogManager.getLogger(this.javaClass) }
    }

}