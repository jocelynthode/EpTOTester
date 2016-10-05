package epto.utilities


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import epto.Peer
import epto.libs.Delegates.logger
import epto.pss.PeerSamplingService
import org.nustaq.serialization.FSTConfiguration
import java.net.InetAddress
import java.util.*

/**
 * Implementation of an Application
 */
open class Application(TTL: Int, K: Int, baseURL: String, var expectedEvents: Int = -1, myIp: InetAddress, myPort: Int = 10353) {

    val logger by logger()
    val peer = Peer(this, TTL, K, myIp, myPort)

    init {
        conf.registerClass(HashMap::class.java, Pair::class.java, Event::class.java)
        var result: String?
        var tmp_view: MutableList<String>?
        FuelManager.instance.basePath = baseURL
        do {
            Thread.sleep(5000)
            result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
            tmp_view = result.split('|').toMutableList()
        } while (tmp_view!!.size < (K + 5))

        logger.debug(result)
        if (tmp_view.contains(myIp.hostAddress)) {
            tmp_view.remove(myIp.hostAddress)
        }
        //Add seeds to the PSS view
        peer.core.pss.view.addAll(tmp_view.map { PeerSamplingService.PeerInfo(InetAddress.getByName(it)) })
        //Start after we have a view
        peer.core.startPss()
    }

    @Synchronized fun deliver(event: Event) {
        expectedEvents--
        logger.info("Delivered : ${event.id}")
        logger.debug("Expected events: ${expectedEvents}")
        if (expectedEvents <= 0) {
            logger.info("All events delivered !")
        }
    }

    fun start() = Thread(peer).start()

    fun stop() = peer.stop()

    @Throws(InterruptedException::class)
    open fun broadcast(event: Event = Event()) {
        peer.disseminationComponent.broadcast(event)
        logger.info(" sending: " + event.id.toString())
    }

    companion object {
        val conf = FSTConfiguration.createDefaultConfiguration()!!
    }
}
