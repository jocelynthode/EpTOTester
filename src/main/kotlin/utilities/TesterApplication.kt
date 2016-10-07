package utilities


import epto.Application
import epto.Event
import java.net.InetAddress

/**
 * Implementation of an Application to test EpTO
 *
 * @property expectedEvents the number of events we expect to deliver
 * @see Application
 */
class TesterApplication(ttl: Int, k: Int, trackerURL: String, var expectedEvents: Int = -1,
                        delta: Long, myIp: InetAddress, gossipPort: Int, pssPort: Int) :
        Application(ttl, k, trackerURL, delta, myIp, gossipPort, pssPort) {

    /**
     * Delivers the event to STDOUT
     *
     * {@inheritDoc}
     */
    @Synchronized override fun deliver(event: Event) {
        expectedEvents--
        logger.info("Delivered: ${event.id}")
        logger.debug("Expected events: ${expectedEvents}")
        if (expectedEvents <= 0) {
            logger.info("All events delivered !")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun broadcast(event: Event) {
        peer.disseminationComponent.broadcast(event)
        logger.info("Sending: ${event.id}")
    }
}
