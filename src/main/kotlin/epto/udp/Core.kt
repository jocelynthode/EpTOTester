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
 *
 * @property k the gossip fanout parameter
 *
 * @property gossipPort the port used for the main channel
 *
 * @property pssPort the port used for the PSS
 *
 * @property gossipChannel the channel on which EpTO gossips
 *
 * @property pssChannel the channel on which the PSS gossips
 *
 * @property pss the PSS
 *
 * @property gossip the Gossip
 *
 * @see PeerSamplingService
 * @see Gossip
 */
class Core(val myIp: InetAddress, k: Int, val gossipPort: Int = 10353, val pssPort: Int = 10453) {

    private val logger by logger()


    val gossipChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, gossipPort))!!
    val pssChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, pssPort))!!
    val pss = PeerSamplingService(50000, this)
    val gossip = Gossip(this, k)
    internal var pssMessages = 0
    internal var gossipMessages = 0

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
        gossipChannel.send(ByteBuffer.wrap(message), InetSocketAddress(target, gossipPort))
        gossipMessages++
    }

    /**
     * Send a datagram to the target PSS port
     *
     * @param message the message to send
     *
     * @param target the peer to send the message
     */
    fun sendPss(message: ByteArray, target: InetAddress) {
        pssChannel.send(ByteBuffer.wrap(message), InetSocketAddress(target, pssPort))
        pssMessages++
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