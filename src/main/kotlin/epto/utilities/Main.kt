package epto.utilities

import epto.libs.Delegates.logger
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.ArgumentParser
import java.net.InetAddress

/**
 * Created by jocelyn on 19.09.16.
 */
class Main {

    companion object {

        val logger by logger()

        const val EVENTS_TO_SEND = 12
        var expectedEvents = 0

        @JvmStatic fun main(args: Array<String>) {
            if (args.size < 3) {
                logger.error("Usage: apps.Main local tracker peer_number")
                System.exit(1)
            }
/*
            val parser = ArgumentParsers.newArgumentParser("EpTO tester")
            parser.addArgument("-i", "--local-ip").help("Peer local IP")
            parser.addArgument("-t", "--tracker").help("Tracker used to fetch initial view").
                    setDefault("http://epto-tracker:4321")
            parser.addArgument("-n", "--peer-number").help("Peer number")
                    .type(Integer.TYPE)
                    .setDefault(35)
                    */

            //c = 4 for 99.9875% =>  c+1 = 5
            val n = args[2].toDouble()
            val log2N = Math.log(n) / Math.log(2.0)
            val ttl = (2 * Math.ceil(5 * log2N) + 1).toInt()
            val k = Math.ceil(2.0 * Math.E * Math.log(n) / Math.log(Math.log(n))).toInt()

            if (InetAddress.getByName(args[0]).isLoopbackAddress)
                logger.warn("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.")

            expectedEvents = EVENTS_TO_SEND * n.toInt()

            val application = Application(ttl, k, args[1], expectedEvents, InetAddress.getByName(args[0]))

            application.start()
            //Let all docker instances be created
            Thread.sleep(70000)
            logger.info("Started: ${args[0]}")
            logger.info("Peer ID : ${application.peer.uuid}")
            logger.info("Peer Number : ${n.toInt()}")
            logger.info("TTL : $ttl, K : $k")

            var eventsSent = 0
            while (eventsSent != EVENTS_TO_SEND) {
                application.broadcast()
                Thread.sleep(1000)
                eventsSent++
            }
            while (true) {
                logger.info("Events not yet delivered: ${application.peer.orderingComponent.received.size}")
                Thread.sleep(10000)
            }
            //neem.close();

        }
    }
}