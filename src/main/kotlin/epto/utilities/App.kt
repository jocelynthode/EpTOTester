package epto.utilities


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import epto.Peer
import net.sf.neem.MulticastChannel
import net.sf.neem.impl.Application
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Implementation of an Application
 */
open class App(neem: MulticastChannel, TTL: Int, K: Int, baseURL: String, var expected_events: Int = -1) : Application {

    val peer = Peer(neem, this, TTL, K)

    init {
        var result : String?
        var tmp_view : MutableList<String>?
        FuelManager.instance.basePath = baseURL
        do {
            result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
            tmp_view = result.split('|').toMutableList()
            Thread.sleep(5000)
        } while (tmp_view!!.size < 10)

        println(result)
        System.err.println(neem.localSocketAddress.address.toString())
        if (tmp_view.contains(neem.localSocketAddress.address.toString())) {
            tmp_view.remove(neem.localSocketAddress.address.toString())
        }

        for (hostname in tmp_view) {
            neem.connect(InetSocketAddress(hostname, 10353))
        }

    }

    /**
     * {@inheritDoc}
     */
    @Synchronized override fun deliver(byteBuffers: Array<ByteBuffer>) {
        //TODO review forEach
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
            expected_events--;
            println("Expected events: ${expected_events}")
            if (expected_events == 0) {
                println("All events delivered !")
                System.exit(0)
            }
        }
    }

    fun start() = Thread(peer).start()

    @Throws(InterruptedException::class)
    open fun broadcast(event: Event = Event()) {
        peer.disseminationComponent.broadcast(event)
        println(" sending: " + event.id.toString())
    }



}
