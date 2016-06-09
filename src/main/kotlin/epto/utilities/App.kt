package epto.utilities


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import epto.Peer
import net.sf.neem.MulticastChannel
import net.sf.neem.impl.Application
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementation of an Application
 */
open class App(private val neem: MulticastChannel, TTL: Int, K: Int) : Application {

    val peer = Peer(neem, this, TTL, K)

    /**
     * {@inheritDoc}
     */
    @Synchronized override fun deliver(byteBuffers: Array<ByteBuffer>) {
        byteBuffers.forEach { byteBuffer ->
            val content = byteBuffer.array()
            val byteIn = ByteArrayInputStream(content)
            val inputStream = ObjectInputStream(byteIn)
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
        println(" sending: " +event.id.toString())
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

                val neem = MulticastChannel(InetSocketAddress(args[0], 10353))
                FuelManager.instance.basePath = args[1]

                println("Started: ${neem.localSocketAddress}")

                if (neem.localSocketAddress.address.isLoopbackAddress)
                    println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

                var n = 1.0
                Thread.sleep(5000)
                val (request, response, result) = "/REST/v1/admin/get_view".httpGet().responseString()
                println(result)
                /*
                * TODO use this view to initialize the Peer Sampling Service in the Peer
                *
                 */
                System.exit(0)



                expected_events = eventsPerSecond*timeToRun*60*n

                //c = 4 for 99.9875% =>  c+1 = 5
                val log2N = Math.log(n) / Math.log(2.0)
                val ttl = (2 * Math.ceil(5 * log2N) + 1).toInt()
                val k = Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()

                val app = App(neem, ttl, k)
                println("Peer ID : ${app.peer.uuid}")
                println("Peer Number : ${n.toInt()}")
                println("TTL : $ttl, K : $k")

                app.start()
		// sleep for 4 minutes
		Thread.sleep(4*60*1000)
                val start = System.currentTimeMillis()
                val end = start + (timeToRun*60*1000)
                var j =  1
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
