package epto

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import epto.libs.Utilities.logger
import epto.udp.Core
import epto.udp.Gossip
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
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
class Peer(application: Application, ttl: Int, k: Int, p: Int, delta: Long, myIp: InetAddress,
           gossipPort: Int = 10353, pssPort: Int = 10453, trackerURL: String) : Runnable {

    private val logger by logger()

    val uuid = UUID.randomUUID()!!
    val core = Core(myIp, k, p, gossipPort, pssPort, trackerURL)
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
                //TODO fix array as right now we allow bigger packet size
                val buf = ByteArray(core.gossip.maxSize)
                val bb = ByteBuffer.wrap(buf)
                val address = core.gossipChannel.receive(bb)
                if (address != null) {
                    val byteIn = ByteArrayInputStream(bb.array())
                    val inputStream = FSTObjectInput(byteIn)

                    val messageType = inputStream.readInt()
                    when (messageType) {
                        Gossip.Companion.MessageType.GOSSIP.ordinal-> receiveGossip(inputStream)
                        Gossip.Companion.MessageType.PULL_REQUEST.ordinal ->
                            receivePullRequest(inputStream, (address as InetSocketAddress))
                        Gossip.Companion.MessageType.PULL_REPLY.ordinal ->
                                receivePullReply(inputStream)
                    }
                }
            } catch (e: IOException) {
                isRunning = false
                logger.error(e)
            } catch (e: Event.EventUnserializeException) {
                logger.error("Error while unserializing an event", e)
            }
        }
    }

    /**
     * Stops the peer
     */
    fun stop() {
        isRunning = false
        disseminationComponent.stop()
        core.stop()
    }


    private fun receiveGossip(inputStream: FSTObjectInput) {
        var len = inputStream.readInt()
        val receivedBall = HashMap<String, Event>()
        logger.debug("ReceivedBall size: {}", len)
        while (len > 0) {
            val event = Event.unserialize(inputStream)
            receivedBall[event.toIdentifier()] = event
            len--
        }
        inputStream.close()
        disseminationComponent.receiveGossip(receivedBall)
        core.gossipMessagesReceived++
        logger.debug("Balls received: {}", core.gossipMessagesReceived)
    }

    private fun receivePullRequest(inputStream: FSTObjectInput, address: InetSocketAddress) {
        val timestamp = inputStream.readInt()
        val filter= inputStream.readObject() as  CuckooFilter<Event>
        inputStream.close()
        disseminationComponent.receivePullRequest(timestamp, filter, address)
        core.pullRequestReceived++
    }

    private fun  receivePullReply(inputStream: FSTObjectInput) {
        var len = inputStream.readInt()
        val receivedBall = HashMap<String, Event>()
        logger.debug("ReceivedBall size: {}", len)
        while (len > 0) {
            val event = Event.unserialize(inputStream)
            receivedBall[event.toIdentifier()] = event
            len--
        }
        inputStream.close()
        disseminationComponent.receivePullReply(receivedBall)
        core.pullReplyReceived++
    }
}
