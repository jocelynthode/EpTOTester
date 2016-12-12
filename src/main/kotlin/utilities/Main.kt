package utilities

import epto.libs.Utilities
import epto.libs.Utilities.logger
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import net.sourceforge.argparse4j.inf.ArgumentType
import net.sourceforge.argparse4j.inf.ArgumentParser
import net.sourceforge.argparse4j.inf.Argument



/**
 * Main class used to start the benchmarks
 *
 * @author Jocelyn Thode
 */
class Main {

    companion object {

        private val logger by logger()
        var eventsSent = 0
            private set

        @JvmStatic fun main(args: Array<String>) {

            val parser = ArgumentParsers.newArgumentParser("EpTO tester")
            parser.defaultHelp(true)
            parser.addArgument("localIp").help("Peer local IP")
            parser.addArgument("tracker").help("Tracker used to fetch initial view").
                    setDefault("http://localhost:4321")
            parser.addArgument("peerNumber").help("Peer number")
                    .type(Integer.TYPE)
            parser.addArgument("scheduleAt").help("Schedule EpTO to start at a specific time in milliseconds")
                    .type(Long::class.java)
            parser.addArgument("timeToRun").help("Delay after which to stop from scheduleAt (in ms)")
                    .type(Integer.TYPE)
            parser.addArgument("-c", "--constant").help("Constant to have a K and TTL meeting an expected delivery ratio")
                    .type(Integer.TYPE)
                    .setDefault(2)
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
            parser.addArgument("-u", "--fixed-rate")
                    .help("If this option is set a probability will be calculated to ensure the overall event broadcast rate is at the value fixed (events/s)")
                    .type(Integer.TYPE)
                    .setDefault(-1)
            parser.addArgument("--churn-rate")
                    .help("Specifies the upper bound of peers removed/added per round")
                    .type(Integer.TYPE)
                    .setDefault(0)
            parser.addArgument("--message-loss")
                    .help("Specifies the message loss of the network as a float value between 0 and 1")
                    .type(ArgumentType(@Suppress("UNUSED_PARAMETER")
                    fun(parser: ArgumentParser, arg: Argument, value: String):Double {
                        try {
                            val n = value.toDouble()
                            if (n > 1.0 || n < 0.0) {
                                throw ArgumentParserException("Message loss must be in the interval [0,1]", parser)
                            }
                            return n
                        } catch (e: NumberFormatException) {
                            throw ArgumentParserException(e, parser)
                        }
                    }))
                    .setDefault(0.0)


            try {
                val res = parser.parseArgs(args)
                startProgram(res)
            } catch (e: ArgumentParserException) {
                parser.handleError(e)
            }
        }

        @JvmStatic private fun startProgram(namespace: Namespace) {

            val rate = namespace.getLong("rate")
            val localIp = namespace.getString("localIp")
            val tracker = namespace.getString("tracker")
            val startTime = namespace.getLong("scheduleAt")
            val timeToRun = namespace.getInt("timeToRun")
            val peerNumber = namespace.getInt("peerNumber").toDouble()
            val delta = namespace.getLong("delta")
            val gossipPort = namespace.getInt("gossip_port")
            val pssPort = namespace.getInt("pss_port")
            val fixedRate = namespace.getInt("fixed_rate")
            val c = namespace.getInt("constant")
            val churnRate = namespace.getInt("churn_rate")
            val messageLossRate = namespace.getDouble("message_loss")

            //c = 4 for 99.9875% =>  c+1 = 5
            val log2N = Math.log(peerNumber) / Math.log(2.0)
            val ttl = if (namespace.getInt("ttl") == null) {
                (2 * Math.ceil((c + 1) * log2N) + 1).toInt()
            } else {
                namespace.getInt("ttl")
            }
            val k = if (namespace.getInt("fanout") == null) {
                val churnParameter = peerNumber / (peerNumber - churnRate) * 1 / (1 - messageLossRate)
                Math.ceil(2.0 * Math.E * Math.log(peerNumber) / Math.log(Math.log(peerNumber)) * churnParameter).toInt()
            } else {
                namespace.getInt("fanout")
            }

            if (InetAddress.getByName(localIp).isLoopbackAddress)
                logger.warn("WARNING: Hostname resolves to loopback address! Please fix network configuration\n" +
                        "or expect only local peers to connect.")


            val application = TesterApplication(ttl, k, tracker, peerNumber.toInt(), delta, InetAddress.getByName(localIp),
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


            val probability: Double = if (fixedRate == -1) 1.0 else (fixedRate / peerNumber)
            val runEpto = Runnable {
                val endTime = startTime + timeToRun
                logger.info("EpTO will end at {} UTC",
                        LocalDateTime.ofEpochSecond((endTime / 1000), 0, ZoneOffset.ofHours(0)))
                logger.info("Sending 1 event every ${rate}ms) with a probability of $probability")
                while (System.currentTimeMillis() < endTime) {
                    Thread.sleep(rate)
                    if (Math.random() < probability) {
                        application.broadcast()
                        eventsSent++
                    }
                }
                var i = 0
                while (i < 40) {
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