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

        val eventsPerSecond = 1.0
        val timeToRun = 0.2
        var expected_events = 0.0

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

            expected_events = eventsPerSecond * timeToRun * 60 * n
            //TODO add expected events
            val app = App(neem, ttl, k, args[1], expected_events.toInt())

            //Give some time for the PSS to have a randomized view
            Thread.sleep(135000)
            //System.exit(0)

            println("Peer ID : ${app.peer.uuid}")
            println("Peer Number : ${n.toInt()}")
            println("TTL : $ttl, K : $k")

            app.start()
            // sleep for 5sec
            Thread.sleep(15000)
            val start = System.currentTimeMillis()
            val end = start + (timeToRun * 60 * 1000)
            var j = 1
            while (System.currentTimeMillis() < end) {
                Thread.sleep((1000 / eventsPerSecond).toLong())
                print(j++)
                app.broadcast()
            }
            while (true) {
                Thread.sleep(15000)
            }
            //neem.close();

        }
    }
}