package epto

import epto.libs.Utilities.logger
import epto.udp.Core
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 *
 * Initializes a peer
 *
 * @param application the application
 * @param ttl the ttl
 * @param k the fanout
 * @param delta the delta
 * @param myIp the Peer IP address
 * @param gossipPort the gossip port
 * @param pssPort the PSS port
 *
 * @property uuid the Peer UUID
 * @property core the UDP core
 * @property oracle the Stability oracle
 * @property orderingComponent the Ordering component
 * @property disseminationComponent the Dissemination component
 *
 * @see Application
 * @see Core
 * @see StabilityOracle
 * @see OrderingComponent
 * @see DisseminationComponent
 *
 *@author Jocelyn Thode
 */
class Peer(application: Application, ttl: Int, k: Int, delta: Long, myIp: InetAddress,
           gossipPort: Int = 10353, pssPort: Int = 10453, trackerURL: String) : Runnable {

    private val logger by logger()

    val uuid = UUID.randomUUID()!!
    val core = Core(myIp, k, gossipPort, pssPort, trackerURL)
    val oracle = StabilityOracle(ttl)
    val orderingComponent = OrderingComponent(oracle, application)
    val disseminationComponent = DisseminationComponent(oracle, this, core.gossip, orderingComponent, k, delta)
    private var isRunning = false

    /**
     * The peer main function
     */
    override fun run() {
        logger.debug("Starting Peer")
        disseminationComponent.start()
        isRunning = true
        while (isRunning) {
            try {
                val buf = ByteArray(core.gossip.maxSize)
                val bb = ByteBuffer.wrap(buf)
                if (core.gossipChannel.receive(bb) != null) {
                    val byteIn = ByteArrayInputStream(bb.array())
                    val inputStream = FSTObjectInput(byteIn)

                    var len = inputStream.readInt()
                    val receivedBall = HashMap<UUID, Event>()
                    logger.debug("ReceivedBall size: {}", len)
                    while (len > 0) {
                        val event = Event.unserialize(inputStream)
                        receivedBall[event.id] = event
                        len--
                    }
                    inputStream.close()
                    disseminationComponent.receive(receivedBall)
                    core.gossipMessagesReceived++
                    logger.debug("Balls received: {}", core.gossipMessagesReceived)
                }
            } catch (e: IOException) {
                isRunning = false
                logger.error(e)
            } catch (e: Event.EventUnserializeException) {
                logger.error(e)
            }
        }
    }

    fun stop() {
        isRunning = false
        disseminationComponent.stop()
        core.stop()
    }
}


