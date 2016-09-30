package epto.udp

import epto.libs.Delegates.logger
import epto.pss.PeerSamplingService
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * Represent the core of the UDP protocols in EpTO/PSS
 *
 * @param myIp The IP on which the main channel will be bound
 *
 * @param K the gossip fanout parameter
 *
 * @param gossipPort the port used for the main channel
 *
 * @param pssPort the port used for the PSS
 */
class Core(val myIp: InetAddress, K: Int, val gossipPort: Int = 10353, val pssPort: Int = 10453) {

    val logger by logger()

    val gossipChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, gossipPort))!!
    val pssChannel = DatagramChannel.open().bind(InetSocketAddress(myIp, pssPort))!!
    val pss = PeerSamplingService(25000, this)
    val gossip = Gossip(this, K)

    init {
        gossipChannel.configureBlocking(true)
        pssChannel.configureBlocking(true)
        pss.start()
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