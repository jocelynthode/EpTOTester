package epto.utilities

import epto.libs.Delegates.logger
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import java.net.InetAddress
import kotlin.concurrent.thread

/**
 * Created by jocelyn on 19.09.16.
 */
class Main {

    companion object {

        val logger by logger()
        var expectedEvents = 0

        @JvmStatic fun main(args: Array<String>) {

            val parser = ArgumentParsers.newArgumentParser("EpTO tester")
            parser.defaultHelp(true)
            parser.addArgument("localIp").help("Peer local IP")
            parser.addArgument("tracker").help("Tracker used to fetch initial view").
                    setDefault("http://epto-tracker:4321")
            parser.addArgument("peerNumber").help("Peer number")
                    .type(Integer.TYPE)
                    .setDefault(35)
            parser.addArgument("-e", "--events").help("Number of events to send")
                    .type(Integer.TYPE)
                    .setDefault(12)
            parser.addArgument("-k", "--fanout").help("Number of peers to gossip")
                    .type(Integer.TYPE)
            parser.addArgument("-t", "--ttl").help("Number of rounds before considering an event mature")
                    .type(Integer.TYPE)


            try {
                val res = parser.parseArgs(args)
                startProgram(res)
            } catch (e: ArgumentParserException) {
                parser.handleError(e)
            }
        }

        @JvmStatic private fun  startProgram(namespace: Namespace) {

            val eventsToSend = namespace.getInt("events")
            val localIp = namespace.getString("localIp")
            val tracker = namespace.getString("tracker")
            val n = namespace.getInt("peerNumber").toDouble()

            //c = 4 for 99.9875% =>  c+1 = 5
            val log2N = Math.log(n) / Math.log(2.0)
            val ttl = if (namespace.getInt("ttl") == null) {
                (2 * Math.ceil(5 * log2N) + 1).toInt()
            } else {
                namespace.getInt("ttl")
            }
            val k = if (namespace.getInt("fanout") == null) {
                Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()
            } else {
                namespace.getInt("fanout")
            }

            if (InetAddress.getByName(localIp).isLoopbackAddress)
                logger.warn("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

            expectedEvents = eventsToSend * n.toInt()

            val application = Application(ttl, k, tracker, expectedEvents, InetAddress.getByName(localIp))

            Runtime.getRuntime().addShutdownHook(thread {
                logger.info("Quitting EpTO tester")
                application.stop()
                logger.info("EpTO messages sent: ${application.peer.core.gossipMessages}")
                logger.info("EpTO messages received: ${application.peer.messagesReceived}")
                logger.info("PSS messages sent: ${application.peer.core.pssMessages}")
            })

            application.start()
            //Let all docker instances be created
            logger.info("Started: $localIp")
            logger.info("Peer ID : ${application.peer.uuid}")
            logger.info("Peer Number : ${n.toInt()}")
            logger.info("TTL : $ttl, K : $k")
            Thread.sleep(70000)

            var eventsSent = 0
            while (eventsSent != eventsToSend) {
                application.broadcast()
                Thread.sleep(1000)
                eventsSent++
            }
            while (true) {
                logger.info("Events not yet delivered: ${application.peer.orderingComponent.received.size}")
                Thread.sleep(10000)
            }
        }
    }
}