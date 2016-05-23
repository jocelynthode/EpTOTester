package epto.utilities


import epto.Peer
import net.sf.neem.MulticastChannel
import net.sf.neem.apps.Addresses
import net.sf.neem.impl.Application
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
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
        byteBuffers.forEach { byteBuffer ->
            val content = byteBuffer.array()
            val byteIn = ByteArrayInputStream(content)
            val inputStream = ObjectInputStream(byteIn)
            val event = inputStream.readObject() as Event
            println("Delivered : ${event.id}")

        }
    }

    fun start() = Thread(peer).start()

    @Throws(InterruptedException::class)
    open fun broadcast(event: Event = Event()) = peer.disseminationComponent.broadcast(event)

    companion object {

        @JvmStatic fun main(args: Array<String>) {
            if (args.size < 1) {
                System.err.println("Usage: apps.App local peer1 ... peerN")
                System.exit(1)
            }

            try {

                val neem = MulticastChannel(Addresses.parse(args[0], true))

                println("Started: ${neem.localSocketAddress}")

                if (neem.localSocketAddress.address.isLoopbackAddress)
                    println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

                val n = args.size.toDouble()
                //c = 4 for 99.9875% =>  c+1 = 5
                val log2N = Math.log(n) / Math.log(2.0)
                val ttl = (2 * Math.ceil(5 * log2N) + 1).toInt()
                val k = Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()

                val app = App(neem, ttl, k)
                println("Peer ID : ${app.peer.uuid}")
                println("Peer Number : ${n.toInt()}")
                println("TTL : $ttl, K : $k")

                for (arg in args)
                    neem.connect(Addresses.parse(arg, false))

                app.start()
                Thread.sleep(1000)
                app.broadcast()
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
