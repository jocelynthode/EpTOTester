package epto.utilities


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import epto.Peer
import epto.pss.PeerSamplingService
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Implementation of an Application
 */
open class Application(TTL: Int, K: Int, baseURL: String, var expectedEvents: Int = -1, myIp: InetAddress, myPort: Int = 10353) {

    val peer = Peer(this, TTL, K, myIp, myPort)

    init {
        var result: String?
        var tmp_view: MutableList<String>?
        FuelManager.instance.basePath = baseURL
        do {
            Thread.sleep(5000)
            result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
            tmp_view = result.split('|').toMutableList()
        } while (tmp_view!!.size < 15)

        println(result)
        println(myIp.hostAddress)
        if (tmp_view.contains(myIp.hostAddress)) {
            tmp_view.remove(myIp.hostAddress)
        }

        //Add seeds to the PSS view
        peer.core.pss.view.addAll(tmp_view.map { PeerSamplingService.PeerInfo(InetAddress.getByName(it)) })
    }

    @Synchronized fun deliver(byteBuffers: Array<ByteBuffer>) {
        byteBuffers.forEach { byteBuffer ->
            val content = byteBuffer.array()
            val byteIn = ByteArrayInputStream(content)
            val inputStream = FSTObjectInput(byteIn)
            try {
                val event = inputStream.readObject() as Event
                println("Delivered : ${event.id}")
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream.close()
            }
            expectedEvents--
            println("Expected events: ${expectedEvents}")
            if (expectedEvents <= 0) {
                println("All events delivered !")
            }
        }
    }

    fun start() = Thread(peer).start()

    fun stop() = peer.stop()

    @Throws(InterruptedException::class)
    open fun broadcast(event: Event = Event()) {
        peer.disseminationComponent.broadcast(event)
        println(" sending: " + event.id.toString())
    }


}
