package utilities

import epto.libs.Utilities
import epto.libs.Utilities.logger
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import java.io.File
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Main class used to start the benchmarks
 *
 * @author Jocelyn Thode
 */
class Main {

    companion object {

        private val logger by logger()
        private var expectedEvents = 0

        @JvmStatic fun main(args: Array<String>) {

            val parser = ArgumentParsers.newArgumentParser("EpTO tester")
            parser.defaultHelp(true)
            parser.addArgument("localIp").help("Peer local IP")
            parser.addArgument("tracker").help("Tracker used to fetch initial view").
                    setDefault("http://localhost:4321")
            parser.addArgument("peerNumber").help("Peer number")
                    .type(Integer.TYPE)
                    .setDefault(35)
            parser.addArgument("scheduleAt").help("Schedule EpTO to start at a specific time in milliseconds")
                    .type(Long::class.java)
            parser.addArgument("-e", "--events").help("Number of events to send")
                    .type(Integer.TYPE)
                    .setDefault(12)
            parser.addArgument("-r", "--rate").help("Time between each event broadcast in ms")
                    .type(Long::class.java)
                    .setDefault(1000L)
            parser.addArgument("-k", "--fanout").help("Number of peers to gossip")
                    .type(Integer.TYPE)
            parser.addArgument("-t", "--ttl").help("Number of rounds before considering an event mature")
                    .type(Integer.TYPE)
            parser.addArgument("-d", "--delta").help("EpTO dissemination period in milliseconds")
                    .type(Long::class.java)
                    .setDefault(6000L)
            parser.addArgument("-g", "--gossip-port").help("Port on which the gossip channel will listen")
                    .type(Integer.TYPE)
                    .setDefault(10353)
            parser.addArgument("-p", "--pss-port").help("Port on which the pss channel will listen")
                    .type(Integer.TYPE)
                    .setDefault(10453)


            try {
                val res = parser.parseArgs(args)
                startProgram(res)
            } catch (e: ArgumentParserException) {
                parser.handleError(e)
            }
        }

        @JvmStatic private fun startProgram(namespace: Namespace) {

            val eventsToSend = namespace.getInt("events")
            val rate = namespace.getLong("rate")
            val localIp = namespace.getString("localIp")
            val tracker = namespace.getString("tracker")
            val startTime = namespace.getLong("scheduleAt")
            val n = namespace.getInt("peerNumber").toDouble()
            val delta = namespace.getLong("delta")
            val gossipPort = namespace.getInt("gossip_port")
            val pssPort = namespace.getInt("pss_port")

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

            val application = TesterApplication(ttl, k, tracker, expectedEvents, n.toInt(), delta, InetAddress.getByName(localIp),
                    gossipPort, pssPort)
            application.start()

            /*
            Runtime.getRuntime().addShutdownHook(Thread {
                println("Quitting EpTO tester")
                println("EpTO messages sent: ${application.peer.core.gossipMessagesSent}")
                println("EpTO messages received: ${application.peer.core.gossipMessagesReceived}")
                println("PSS messages sent: ${application.peer.core.pssMessagesSent}")
                println("PSS messages received: ${application.peer.core.gossipMessagesReceived}")
                application.stop()
            })
            */
            val scheduler = Executors.newScheduledThreadPool(1)

            val runEpto = Runnable {
                /*
                val randomDelay = Random().nextInt(10) * 1000L
                logger.info("Sleeping for {}ms before sending events", randomDelay)
                Thread.sleep(randomDelay)
                */
                var eventsSent = 0
                logger.info("Sending: $eventsToSend events (rate: 1 every ${rate}ms)")
                while (eventsSent != eventsToSend) {
                    Thread.sleep(rate)
                    application.broadcast()
                    eventsSent++
                }
                var i = 0
                while (i < 120) {
                    if (application.expectedEvents <= 0) {
                        Thread.sleep(2*(ttl * delta))
                        break
                    }
                    logger.debug("Events not yet delivered: {}", application.peer.orderingComponent.received.size)
                    Thread.sleep(10000)
                    i++
                }
                application.stop()
                System.exit(0)
            }
            scheduler.schedule(runEpto, Utilities.scheduleAt(startTime), TimeUnit.MILLISECONDS)
        }
    }
}