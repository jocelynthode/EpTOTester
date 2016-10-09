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
class TesterApplication(ttl: Int, k: Int, trackerURL: String, var expectedEvents: Int = -1, val peerNumber: Int,
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

    /**
     * {@inheritDoc}
     */
    override fun start() {
        Thread(peer).start()
        logger.info("Started: ${myIp.hostAddress}")
        logger.info("Peer ID: ${peer.uuid}")
        logger.info("Peer Number: $peerNumber")
        logger.info("TTL: ${peer.oracle.TTL}, K: ${peer.disseminationComponent.K}")
        logger.info("Delta: ${peer.disseminationComponent.delta}")
    }

    /**
     * {@inheritDoc}
     */
    override fun stop() {
        peer.stop()
        logger.info("Quitting EpTO tester")
        logger.info("EpTO messages sent: ${peer.core.gossipMessages}")
        logger.info("EpTO messages received: ${peer.messagesReceived}")
        logger.info("PSS messages sent: ${peer.core.pssMessages}")
        logger.info("PSS messages received: ${peer.core.pss.passiveThread.messagesReceived}")
    }
}
