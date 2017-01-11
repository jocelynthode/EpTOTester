package epto.udp

import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * Represent the core of the UDP protocols in EpTO/PSS
 *
 * @property myIp The IP on which the main channel will be bound
 * @param k the gossip fanout parameter
 * @property gossipPort the port used for the main channel
 * @property pssPort the port used for the PSS
 * @property gossipChannel the channel on which EpTO gossips
 * @property pssChannel the channel on which the PSS gossips
 * @property gossip the Gossip
 * @property pss the PSS
 *
 * @see PeerSamplingService
 * @see Gossip
 */
class Core(val myIp: InetAddress, k: Int, p: Int, val gossipPort: Int = 10353, val pssPort: Int = 10453, trackerURL: String) {

    private val logger by logger()


    val gossipChannel: DatagramChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, gossipPort))
    val pssChannel: DatagramChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, pssPort))
    val pss = PeerSamplingService(15000, this, trackerURL = trackerURL)
    val gossip = Gossip(this, k, p)

    internal var gossipMessagesSent = 0
    internal var gossipMessagesReceived = 0
    internal var pssMessagesSent = 0
    internal var pssMessagesReceived = 0
    internal var pullRequestSent = 0
    internal var pullRequestReceived = 0
    internal var pullReplyReceived = 0
    internal var pullReplySent = 0

    init {
        gossipChannel.configureBlocking(true)
        pssChannel.configureBlocking(true)
    }

    /**
     * Send a datagram to the target main port
     *
     * @param message the message to send
     *
     * @param target the peer to send the message
     */
    fun send(message: ByteArray, target: InetAddress) {
        //logger.debug("Is target reachable: {}", target.isReachable(1000))
        logger.debug("Sending gossip to : {}", target.hostAddress)
        logger.debug("Message size: {}", message.size)
        val bytesSent = gossipChannel.send(ByteBuffer.wrap(message), InetSocketAddress(target, gossipPort))
        if (bytesSent == 0) {
            logger.error("Message could not be sent due to insufficient space in the underlying buffer")
            return
        }
        logger.debug("Balls sent: {}", gossipMessagesSent)
    }

    /**
     * Send a datagram to the target PSS port
     *
     * @param message the message to send
     *
     * @param target the peer to send the message
     */
    fun sendPss(message: ByteArray, target: InetAddress) {
        logger.debug("PSS Bytes size : {}", message.size)
        val bytesSent = pssChannel.send(ByteBuffer.wrap(message), InetSocketAddress(target, pssPort))
        if (bytesSent == 0) {
            logger.error("Message could not be sent due to insufficient space in the underlying buffer")
            return
        }
        pssMessagesSent++
    }

    /**
     * Start the pss
     */
    fun startPss() {
        pss.start()
    }

    /**
     * Close the channels and stop the PSS
     */
    fun stop() {
        gossipChannel.close()
        pssChannel.close()
        pss.stop()
    }
}