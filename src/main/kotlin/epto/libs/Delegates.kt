package epto.libs

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Created by jocelyn on 30.09.16.
 */
object Delegates {

    fun <T : Any> T.logger(): Lazy<Logger> {
        return lazy { LogManager.getLogger(this.javaClass) }
    }

}