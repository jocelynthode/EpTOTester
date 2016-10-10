package epto.pss

import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService.PeerInfo
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
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
 */
class PassiveThread(val pssLock: Any, val pss: PeerSamplingService) : Runnable {

    val logger by logger()
    var messagesReceived = 0
        private set
    private var isRunning = false

    override fun run() {
        isRunning = true
        while (isRunning) {
            try {
                val buf = ByteArray(pss.core.pssChannel.socket().receiveBufferSize)
                val bb = ByteBuffer.wrap(buf)
                val address = pss.core.pssChannel.receive(bb)
                if (address != null) {
                    try {
                        val (isPull, receivedView) = unserialize(buf)
                        synchronized(pssLock) {
                            logger.debug("isPull : $isPull")
                            if (!isPull) {
                                val isRemoved = pss.view.removeIf { it.address == (address as InetSocketAddress).address }
                                logger.debug("address isRemoved: $isRemoved")
                                val toSend = pss.selectToSend(true)
                                logger.debug("Address received : ${(address as InetSocketAddress).address.hostAddress}")
                                pss.core.sendPss(toSend, address.address)
                            }
                            pss.selectToKeep(receivedView)
                        }
                        messagesReceived++
                    } catch (e: IOException) {
                        logger.error("Error unserializing view", e)
                    }
                }
            } catch (e: IOException) {
                isRunning = false
            }
        }
    }

    private data class Result(val isPull: Boolean, val receivedView: ArrayList<PeerInfo>)

    private fun unserialize(buf: ByteArray): Result {
        val byteIn = ByteArrayInputStream(buf)
        val inputStream = FSTObjectInput(byteIn)

        val isPull = inputStream.readBoolean()
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
        return Result(isPull, receivedView)

    }

    fun stop() {
        isRunning = false
    }
}