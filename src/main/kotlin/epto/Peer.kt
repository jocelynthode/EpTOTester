package epto

import epto.udp.Core
import epto.utilities.Application
import epto.utilities.Event
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.AsynchronousCloseException
import java.util.*

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 *
 * Initializes a peer
 *
 * @param neem MultiCast object
 */
class Peer(application: Application, TTL: Int, val K: Int, myIp: InetAddress, myPort: Int = 10353) : Runnable {
    val uuid = UUID.randomUUID()!!
    val core = Core(myIp, myPort, K)
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
                val buf = ByteArray(core.socket.receiveBufferSize)
                val datagramPacket = DatagramPacket(buf, buf.size)
                core.socket.receive(datagramPacket)
                val byteIn = ByteArrayInputStream(datagramPacket.data)
                val inputStream = FSTObjectInput(byteIn)
                disseminationComponent.receive(inputStream.readObject() as HashMap<UUID, Event>)
                inputStream.close()
            }
        } catch (ace: AsynchronousCloseException) {
            // Exiting.
        } catch (e: Exception) {
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
