package epto.pss

import epto.libs.Delegates.logger
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

    val logger by logger()
    var isRunning = false

    override fun run() {
        isRunning = true
        while (isRunning) {
            try {
                val buf = ByteArray(pss.core.pssChannel.socket().receiveBufferSize)
                val bb = ByteBuffer.wrap(buf)
                val address = pss.core.pssChannel.receive(bb)
                if (address != null) {
                    val byteIn = ByteArrayInputStream(bb.array())
                    val inputStream = FSTObjectInput(byteIn)
                    val (isPull, receivedView) = inputStream.readObject() as Pair<Boolean, ArrayList<PeerInfo>>
                    inputStream.close()
                    synchronized(pssLock) {
                        //TODO maybe remove oneself
                        logger.debug("isPull : $isPull")
                        if (!isPull) {
                            val toSend = pss.selectToSend(true)
                            logger.debug("Address received : ${(address as InetSocketAddress).address.hostAddress}")
                            pss.core.sendPss(toSend, (address as InetSocketAddress).address)
                        }
                        pss.selectToKeep(receivedView)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error receiving a packet", e)
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        isRunning = false
    }
}