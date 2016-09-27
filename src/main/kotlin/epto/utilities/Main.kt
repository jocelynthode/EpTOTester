package epto.utilities

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import net.sf.neem.MulticastChannel
import java.net.InetSocketAddress

/**
 * Created by jocelyn on 19.09.16.
 */
class Main {

    companion object {

        const val EVENTS_TO_SEND = 12
        var expectedEvents = 0

        @JvmStatic fun main(args: Array<String>) {
            if (args.size < 3) {
                System.err.println("Usage: apps.Main local tracker peer_number")
                System.exit(1)
            }
            System.err.println(args[0])
            System.err.println(args[1])
            System.err.println(args[2])
            println(args[0])
            println(args[1])
            println(args[2])

            val neem = MulticastChannel(InetSocketAddress(args[0], 10353))

            println("Started: ${neem.localSocketAddress}")

            //c = 4 for 99.9875% =>  c+1 = 5
            val n = args[2].toDouble()
            val log2N = Math.log(n) / Math.log(2.0)
            val ttl = (2 * Math.ceil(5 * log2N) + 1).toInt()
            val k = Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()

            if (neem.localSocketAddress.address.isLoopbackAddress)
                println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

            expectedEvents = EVENTS_TO_SEND * n.toInt()

            val app = App(neem, ttl, k, args[1], expectedEvents)

            //Give some time for the PSS to have a randomized view 4cycles approx
            Thread.sleep(105000)
            //System.exit(0)

            println("Peer ID : ${app.peer.uuid}")
            println("Peer Number : ${n.toInt()}")
            println("TTL : $ttl, K : $k")

            app.start()
            // sleep for 5sec
            Thread.sleep(5000)

            var eventsSent = 0
            while (eventsSent != EVENTS_TO_SEND) {
                app.broadcast()
                Thread.sleep(1000)
                eventsSent++
            }
            while (true) {
                Thread.sleep(1000)
            }
            //neem.close();

        }
    }
}