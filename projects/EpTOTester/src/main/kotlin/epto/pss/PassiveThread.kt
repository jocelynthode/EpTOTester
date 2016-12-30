package epto.pss

import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService.PeerInfo
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementation of a PSS passive thread receiving views from other peers
 *
 * @property pssLock the lock used for the active and passive threads
 *
 * @property pss The Peer Sampling Service on which this passive thread depends
 *
 * @see PeerSamplingService
 *
 * @author Jocelyn Thode
 */
class PassiveThread(val pssLock: Any, val pss: PeerSamplingService) : Runnable {

    private val logger by logger()
    private var isRunning = false

    /**
     * Run by the Thread
     */
    override fun run() {
        isRunning = true
        while (isRunning) {
            try {
                val buf: ByteArray = ByteArray(pss.core.gossip.maxSize)
                val bb = ByteBuffer.wrap(buf)
                val address = pss.core.pssChannel.receive(bb)
                if (address != null) {
                    try {
                        val (isPush, receivedView) = unserialize(buf)
                        synchronized(pssLock) {
                            logger.debug("isPush : {}", isPush)
                            if (isPush) {
                                val senderPeerInfo = pss.view.find { it.address ==  (address as InetSocketAddress).address }
                                val toSend = pss.selectToSend(false, senderPeerInfo)
                                pss.core.sendPss(toSend, (address as InetSocketAddress).address)
                            }
                            logger.debug("sender Address : {}", (address as InetSocketAddress).address.hostAddress)
                            pss.selectToKeep(receivedView, isPush)
                        }
                        pss.core.pssMessagesReceived++
                    } catch (e: SocketException) {
                        logger.error("Error while sending a view")
                    } catch (e: IOException) {
                        logger.error("Error unserializing view", e)
                    }
                }
            } catch (e: IOException) {
                isRunning = false
                logger.error(e)
            }
        }
    }

    private data class Result(val isPush: Boolean, val receivedView: ArrayList<PeerInfo>)

    private fun unserialize(buf: ByteArray): Result {
        val byteIn = ByteArrayInputStream(buf)
        val inputStream = FSTObjectInput(byteIn)

        val isPush = inputStream.readBoolean()
        var len = inputStream.readInt()
        val receivedView = ArrayList<PeerInfo>()
        while (len > 0) {
            val byteArray = ByteArray(4)
            inputStream.read(byteArray)
            val address = InetAddress.getByAddress(byteArray)
            val age = inputStream.readInt()
            val peerInfo = PeerInfo(address, age)
            receivedView.add(peerInfo)
            len--
        }
        return Result(isPush, receivedView)

    }

    /**
     * Stops the PassiveThread
     */
    fun stop() {
        isRunning = false
    }
}