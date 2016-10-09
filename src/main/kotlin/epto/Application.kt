package epto

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService
import org.nustaq.serialization.FSTConfiguration
import java.net.InetAddress
import java.util.*

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
 */
abstract class Application(ttl: Int, k: Int, trackerURL: String, delta: Long, val myIp: InetAddress, gossipPort: Int, pssPort: Int) {

    val logger by logger()

    internal val peer = Peer(this, ttl, k, delta, myIp, gossipPort, pssPort)

    init {
        var result: String?
        var tmp_view: MutableList<String> = ArrayList()
        FuelManager.instance.basePath = trackerURL
        var retry = 0
        do {
            try {
                Thread.sleep(5000)
                result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
                tmp_view = result.split('|').toMutableList()
                logger.debug(result)
            } catch (e: Exception) {
                logger.error("Error while trying to get a view from the tracker", e)
                retry++
                if (retry > 25) {
                    logger.error("Too many retries, Aborting...")
                    System.exit(1)
                }
            }
        } while (tmp_view.size < (k + 5))

        if (tmp_view.contains(myIp.hostAddress)) {
            tmp_view.remove(myIp.hostAddress)
        }
        //Add seeds to the PSS view
        peer.core.pss.view.addAll(tmp_view.map { PeerSamplingService.PeerInfo(InetAddress.getByName(it)) })
        //Start after we have a view
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
    abstract fun broadcast(event: Event = Event())

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