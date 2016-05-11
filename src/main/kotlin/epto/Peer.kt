package epto

import epto.utilities.Event
import net.sf.neem.MulticastChannel
import net.sf.neem.impl.Application
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.*

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 *
 * Initializes a peer
 *
 * @param neem MultiCast object
 */
class Peer(private val neem: MulticastChannel, app: Application, TTL: Int, K: Int) : Runnable {
    val uuid = neem.protocolMBean.localId
    private val oracle = StabilityOracle(TTL)
    val orderingComponent = OrderingComponent(oracle, app)
    val disseminationComponent = DisseminationComponent(oracle, this, neem, orderingComponent, K)
    private var is_running: Boolean = false

    init {
        neem.protocolMBean.gossipFanout = disseminationComponent.K
    }

    /**
     * The peer main function
     */
    override fun run() {
        disseminationComponent.start()
        try {
            is_running = true
            while (is_running) {
                val buf = ByteArray(100000)
                val bb = ByteBuffer.wrap(buf)

                neem.read(bb)
                val byteIn = ByteArrayInputStream(bb.array())
                val inputStream = ObjectInputStream(byteIn)
                disseminationComponent.receive(inputStream.readObject() as HashMap<UUID, Event>)
            }
        } catch (ace: AsynchronousCloseException) {
            // Exiting.
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun stop() {
        is_running = false
    }

    companion object {

        internal val DELTA = 2000
    }
}
