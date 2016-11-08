package epto

import epto.libs.Utilities.logger
import org.nustaq.serialization.FSTConfiguration
import java.net.InetAddress

/**
 * Abstract class implementing the API needed to work with EpTO
 *
 * @param ttl the number of rounds before an event is mature
 * @param k the gossip fanout
 * @param trackerURL the base URL where to contact the tracker
 * @param delta the delay between each execution of EpTO
 * @property myIp the Application IP address
 * @param gossipPort the port on which EpTO will communicate
 * @param pssPort the port on which the PSS will communicate
 *
 * @property peer the peer used to gossip
 *
 * @see Peer
 *
 * @author Jocelyn Thode
 */
abstract class Application(ttl: Int, k: Int, trackerURL: String, delta: Long, val myIp: InetAddress, gossipPort: Int, pssPort: Int) {

    internal val logger by logger()

    internal val peer = Peer(this, ttl, k, delta, myIp, gossipPort, pssPort, trackerURL)

    init {
        peer.core.startPss()
    }

    /**
     * Delivers an event from EpTO
     *
     * @param event the event to deliver
     */
    abstract fun deliver(event: Event)

    /**
     * Broadcasts an event to EpTO
     *
     * @param event the event to broadcast
     */
    open fun broadcast(event: Event = Event()) {
        peer.disseminationComponent.broadcast(event)
    }

    /**
     * Starts the application
     */
    open fun start() {
        Thread(peer).start()
    }

    /**
     * Stops the application
     */
    open fun stop() {
        peer.stop()
    }

    companion object {
        val conf = FSTConfiguration.createDefaultConfiguration()!!
    }
}