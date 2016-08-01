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
open class App(private val neem: MulticastChannel, TTL: Int, K: Int) : Application {

    val peer = Peer(neem, this, TTL, K)

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
            println("Expected events: ${expected_events.toInt()}")
            if (expected_events == 0.0) {
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

    companion object {

        val eventsPerSecond = 1.0
        val timeToRun = 0.2
        var expected_events = 0.0

        @JvmStatic fun main(args: Array<String>) {
            if (args.size < 1) {
                System.err.println("Usage: apps.App local tracker")
                System.exit(1)
            }

            try {
                var result : String?
                var tmp_view : MutableList<String>?

                println(args[0])
                println(args[1])
                //Don't start until we have at least an other peer to talk to.
                FuelManager.instance.basePath = args[1]
                do {
                    result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
                    tmp_view = result.split('|').toMutableList()
                    Thread.sleep(5000)
                } while (tmp_view!!.size < 10)


                println(result)
                if (tmp_view.contains(args[0])) {
                    tmp_view.remove(args[0])
                }

                val neem = MulticastChannel(InetSocketAddress(args[0], 10353))

                println("Started: ${neem.localSocketAddress}")

                if (neem.localSocketAddress.address.isLoopbackAddress)
                    println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

                val n = 60.0

                for (hostname in tmp_view) {
                    neem.connect(InetSocketAddress(hostname, 10353))
                }

                //Give some time for the PSS to have a randomized view
                while (true) {
                    Thread.sleep(1000)
                }
                //System.exit(0)


                expected_events = eventsPerSecond * timeToRun * 60 * n

                //c = 4 for 99.9875% =>  c+1 = 5
                val log2N = Math.log(n) / Math.log(2.0)
                val ttl = (2 * Math.ceil(5 * log2N) + 1).toInt()
                val k = Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()

                val app = App(neem, ttl, k)
                println("Peer ID : ${app.peer.uuid}")
                println("Peer Number : ${n.toInt()}")
                println("TTL : $ttl, K : $k")


                app.start()
                // sleep for 10sec
                Thread.sleep(2000)
                val start = System.currentTimeMillis()
                val end = start + (timeToRun * 60 * 1000)
                var j = 1
                while (System.currentTimeMillis() < end) {
                    Thread.sleep((1000 / eventsPerSecond).toLong())
                    print(j++)
                    app.broadcast()
                }
                while (true) {
                    Thread.sleep(1000)
                }
                //neem.close();

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}
