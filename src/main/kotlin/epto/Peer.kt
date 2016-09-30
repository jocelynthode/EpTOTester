package epto

import epto.libs.Delegates.logger
import epto.udp.Core
import epto.utilities.Application
import epto.utilities.Event
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.*

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 *
 * Initializes a peer
 *
 */
class Peer(application: Application, TTL: Int, K: Int, myIp: InetAddress, gossipPort: Int = 10353, pssPort: Int = 10453) : Runnable {

    val logger by logger()

    val uuid = UUID.randomUUID()!!
    val core = Core(myIp, K, gossipPort, pssPort)
    private val oracle = StabilityOracle(TTL)
    val orderingComponent = OrderingComponent(oracle, application)
    val disseminationComponent = DisseminationComponent(oracle, this, core.gossip, orderingComponent, K)
    private var is_running: Boolean = false

    /**
     * The peer main function
     */
    override fun run() {
        disseminationComponent.start()
        try {
            is_running = true
            while (is_running) {
                val buf = ByteArray(core.gossipChannel.socket().receiveBufferSize)
                logger.debug("peer size : ${core.gossipChannel.socket().receiveBufferSize}")
                val bb = ByteBuffer.wrap(buf)
                if (core.gossipChannel.receive(bb) != null) {
                    logger.debug("Peer  RECEIVED message")
                    val byteIn = ByteArrayInputStream(bb.array())
                    val inputStream = FSTObjectInput(byteIn)
                    disseminationComponent.receive(inputStream.readObject() as HashMap<UUID, Event>)
                    inputStream.close()
                }
            }
        } catch (ace: AsynchronousCloseException) {
            // Exiting.
        } catch (e: Exception) {
            logger.error("Error in the peer main loop")
            logger.error(e.message)
            e.printStackTrace()
        }

    }

    fun stop() {
        is_running = false
        disseminationComponent.stop()
        core.stop()
    }

    companion object {

        const internal val DELTA = 5000L
    }
}
