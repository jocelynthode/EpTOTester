package epto

import epto.libs.Delegates.logger
import epto.udp.Core
import epto.utilities.Application
import epto.utilities.Event
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
 */
class Peer(application: Application, TTL: Int, K: Int, delta: Long, myIp: InetAddress, gossipPort: Int = 10353, pssPort: Int = 10453) : Runnable {

    val logger by logger()

    val uuid = UUID.randomUUID()!!
    val core = Core(myIp, K, gossipPort, pssPort)
    private val oracle = StabilityOracle(TTL)
    val orderingComponent = OrderingComponent(oracle, application)
    val disseminationComponent = DisseminationComponent(oracle, this, core.gossip, orderingComponent, K, delta)
    private var isRunning = false
    var messagesReceived = 0
        private set

    /**
     * The peer main function
     */
    override fun run() {
        logger.debug("Starting Peer")
        disseminationComponent.start()
        isRunning = true
        while (isRunning) {
            try {
                val buf = ByteArray(core.gossipChannel.socket().receiveBufferSize)
                val bb = ByteBuffer.wrap(buf)
                if (core.gossipChannel.receive(bb) != null) {
                    val byteIn = ByteArrayInputStream(bb.array())
                    val inputStream = FSTObjectInput(byteIn)
                    var len = inputStream.readInt()
                    val receivedBall = HashMap<UUID, Event>()
                    logger.debug("ReceivedBall size: $len")
                    while (len > 0) {
                        val event = Event.unserialize(inputStream)
                        receivedBall[event.id] = event
                        len--
                    }
                    inputStream.close()
                    disseminationComponent.receive(receivedBall)
                    messagesReceived++
                }
            } catch (e: IOException) {
                isRunning = false
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


