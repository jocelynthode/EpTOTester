package epto.udp

import epto.pss.PeerSamplingService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Created by jocelyn on 29.09.16.
 */
class Core(val myIp: InetAddress, val myPort: Int = 10353, K: Int) {

    val socket = DatagramSocket(myPort, myIp)
    val pss = PeerSamplingService(15000, this)
    val gossip = Gossip(this, K)

    init {
        pss.start()
    }

    fun send(message: ByteArray, target: InetAddress) {
        val socket = DatagramSocket()
        val datagramPacket = DatagramPacket(message, message.size, target, 10353)
        socket.send(datagramPacket)
    }

    fun sendPss(message: ByteArray, target: InetAddress) {
        val socket = DatagramSocket()
        val datagramPacket = DatagramPacket(message, message.size, target, 10453)
        socket.send(datagramPacket)
    }

}