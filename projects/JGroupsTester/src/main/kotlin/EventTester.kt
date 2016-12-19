import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import org.apache.logging.log4j.LogManager
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.ReceiverAdapter
import org.jgroups.View
import java.net.SocketException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This Class connects to a cluster, waits for other peers to join and then sends a number of events
 *
 * This implementation uses the SEQUENCER Channel. This uses Total Order and UDP
 *
 * @author Jocelyn Thode
 */
class EventTester(val peerNumber: Int, val rate: Long, val startTime: Long, val timeToRun: Int, val fixedRate: Int)
: ReceiverAdapter() {

    val logger = LogManager.getLogger(this.javaClass)!!

    var channel = JChannel("sequencer-tcpgossip.xml")
    private val sendTimes = ConcurrentHashMap<UUID, Long>()
    val runJGroups = Runnable {
        try {
            val endTime = startTime + timeToRun
            logger.info("JGroups will end at {} UTC",
                    LocalDateTime.ofEpochSecond((endTime / 1000), 0, ZoneOffset.ofHours(0)))
            logger.info("View: ${channel.view.members.joinToString(separator = ",") { it.toString().substringBefore('-') } }")
            val probability: Double = if (fixedRate == -1) 1.0 else (fixedRate / peerNumber.toDouble())
            logger.info("Sending 1 event every ${rate}ms with a probability of $probability")
            while (System.currentTimeMillis() < endTime) {
                Thread.sleep(rate)
                if (Math.random() < probability) {
                    val msg = Message(null, channel.address, UUID.randomUUID())
                    logger.info("Sending: ${msg.`object` as UUID}")
                    sendTimes[msg.`object` as UUID] = System.nanoTime()
                    channel.send(msg)
                }
                logger.debug("Coordinator is {}", channel.view.creator)
            }
            var i = 0
            while (i < 30) {
                Thread.sleep(10000)
                i++
            }
        } catch (e: SocketException) {
            //Do nothing
        } finally {
            stop()
        }
    }

    /**
     * Starts the peer
     */
    fun start() {
            channel.receiver = this
            channel.connect("EventCluster")

            logger.info(channel.address)
            logger.info("Coordinator is ${channel.view.creator}")
            logger.info("Peer Number: $peerNumber")
            val scheduler = Executors.newScheduledThreadPool(1)
            scheduler.schedule(runJGroups, scheduleAt(startTime), TimeUnit.MILLISECONDS)
    }

    private fun scheduleAt(date: Long): Long {
        if (date < System.currentTimeMillis()) {
            logger.warn("Time given was smaller than current time, running JGroups immediately, but some events might get lost")
            return 0
        } else {
            logger.warn("JGroups will start at {} UTC",
                    LocalDateTime.ofEpochSecond((date / 1000), 0, ZoneOffset.ofHours(0)))
            return (date - System.currentTimeMillis())
        }
    }

    /**
     * Stops the peer
     */
    fun stop() {
        channel.disconnect()
        logger.info("Events sent: ${channel.sentMessages}")
        logger.info("Events received: ${channel.receivedMessages}")
        channel.close()
        System.exit(0)
    }

    override fun viewAccepted(newView: View) {
        logger.info("View size: ${channel.view.size()}")
        logger.debug("** size: {} ** view: {}", newView.size(), newView)
    }

    override fun receive(msg: Message) {
        val msgObject: UUID = msg.`object` as UUID
        if (sendTimes.containsKey(msgObject)) {
            val sendTime = sendTimes[msgObject] as Long
            val delta = System.nanoTime() - sendTime
            logger.info("Delivered: $msgObject -- Local Delta: $delta")
            sendTimes.remove(msgObject)
        } else {
            logger.info("Delivered: $msgObject")
        }
    }
}

/**
 * Main function to be called when starting the Program
 */
fun main(args: Array<String>) {
    val parser = ArgumentParsers.newArgumentParser("JGroups tester")
    parser.defaultHelp(true)
    parser.addArgument("peerNumber").help("Peer number")
            .type(Integer.TYPE)
    parser.addArgument("scheduleAt").help("Schedule Jgroups to start at a specific time in milliseconds")
            .type(Long::class.java)
    parser.addArgument("timeToRun").help("Delay after which to stop from scheduleAt (in ms)")
            .type(Integer.TYPE)
    parser.addArgument("-r", "--rate").help("Time between each event broadcast in ms")
            .type(Long::class.java)
            .setDefault(1000L)
    parser.addArgument("-u", "--fixed-rate")
            .help("If this option is set a probability will be calculated to ensure the overall event broadcast rate is at the value fixed (events/s")
            .type(Integer.TYPE)
            .setDefault(-1)

    try {
        val res = parser.parseArgs(args)
        val eventTester = EventTester(res.getInt("peerNumber"), res.getLong("rate"),
                res.getLong("scheduleAt"), res.getInt("timeToRun"), res.getInt("fixed_rate"))
        eventTester.start()
        while (true) Thread.sleep(500)
    } catch (e: ArgumentParserException) {
        parser.handleError(e)
    }
}
