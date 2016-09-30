package epto.pss

import epto.pss.PeerSamplingService.PeerInfo
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementation of a PSS passive thread receiving views from other peers
 *
 * @param pssLock the lock used for the active and passive threads
 *
 * @param pss The Peer Sampling Service on which this passive thread depends
 */
class PassiveThread(val pssLock: Any, val pss: PeerSamplingService) : Runnable {

    var isRunning = false

    override fun run() {
        isRunning = true
        while (isRunning) {
            val buf = ByteArray(pss.core.pssChannel.socket().receiveBufferSize)
            println("Passive size : ${pss.core.pssChannel.socket().receiveBufferSize}")
            val bb = ByteBuffer.wrap(buf)
            println("Passive before message")
            val address = pss.core.pssChannel.receive(bb)
            if (address != null) {
                println("Passive  RECEIVED message")
                val byteIn = ByteArrayInputStream(bb.array())
                val inputStream = FSTObjectInput(byteIn)
                val receivedView = inputStream.readObject() as ArrayList<PeerInfo>
                inputStream.close()
                synchronized(pssLock) {
                    //TODO maybe remove oneself
                    val toSend = pss.selectToSend()
                    pss.selectToKeep(receivedView)
                    println("Passive test : ${(address as InetSocketAddress).address.hostAddress}")
                    pss.core.sendPss(toSend, address.address)
                }
            }
            println("Passive not received message")

        }
    }

    fun stop() {
        isRunning = false
    }
}