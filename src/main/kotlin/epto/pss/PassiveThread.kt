package epto.pss

import epto.pss.PeerSamplingService.PeerInfo
import epto.udp.Core
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.*

/**
 * Created by jocelyn on 29.09.16.
 */
class PassiveThread(val pssLock: Object, val pss: PeerSamplingService, val core: Core) : Runnable {

    val socket = DatagramSocket(10453)
    var isRunning = false

    override fun run() {
        isRunning = true
        while (isRunning) {
                val buf = ByteArray(socket.receiveBufferSize)
                val datagramPacket = DatagramPacket(buf, buf.size)
                socket.receive(datagramPacket)
                val byteIn = ByteArrayInputStream(datagramPacket.data)
                val inputStream = FSTObjectInput(byteIn)
                val receivedView = inputStream.readObject() as ArrayList<PeerInfo>
                inputStream.close()
            synchronized(pssLock) {
                //TODO maybe remove oneself
                val toSend = pss.selectToSend()
                pss.selectToKeep(receivedView)

                core.sendPss(toSend, datagramPacket.address)
            }
        }
    }

    fun stop() {
        isRunning = false
    }
}