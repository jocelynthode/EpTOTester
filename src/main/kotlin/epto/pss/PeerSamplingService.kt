package epto.pss

import epto.Application
import epto.libs.Utilities.logger
import epto.udp.Core
import org.nustaq.serialization.FSTObjectOutput
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.Serializable
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Implementation of a Peer Sampling Service
 *
 * @property gossipInterval the interval at which to perform the active thread of the PSS
 * @property core the Class responsible for the datagram channels
 * @property c the ideal view size
 * @property exch the number of peers to exch
 * @property s the swapping parameter
 * @property h the healing parameter
 * @property view the current view
 * @property passiveThread the thread in charge of receiving messages
 *
 * @see Core
 * @see PassiveThread
 */
class PeerSamplingService(var gossipInterval: Int, val core: Core, val c: Int = 30, val exch: Int = 14,
                          val s: Int = 8, val h: Int = 1) {

    val logger by logger()

    val view = ArrayList<PeerInfo>()
    private val pssLock = Any()
    val passiveThread = PassiveThread(pssLock, this)
    private val rand = Random()
    private val scheduler = Executors.newScheduledThreadPool(1)
    private val activeThread = Runnable {
        synchronized(pssLock) {
            debug()
            if (view.size < 2) {
                logger.info("Not enough peers to shuffle")
                return@Runnable
            }

            val partner = selectPartner()
            view.remove(partner)
            val toSend = selectToSend(false)
            core.sendPss(toSend, partner.address)
            view.forEach { it.age++ }
        }
    }
    private var activeThreadFuture: ScheduledFuture<*>? = null

    /**
     * Start the Peer Sampling Service
     */
    fun start() {
        Thread(passiveThread).start()
        //Run the PSS 3 times
        for (i in 1..3) {
            logger.debug("Running init PSS-$i")
            activeThread.run()
        }
        activeThreadFuture = scheduler.scheduleAtFixedRate(activeThread, 0, gossipInterval.toLong(),
                TimeUnit.MILLISECONDS)
    }

    /**
     * Stop the Peer Sampling Service
     */
    fun stop() {
        passiveThread.stop()
        activeThreadFuture?.cancel(true)

    }

    /**
     * Selects a sublist of the view to send to the partner
     *
     * @param isPull If the view was solicited by a peer
     *
     * @return subview
     */
    fun selectToSend(isPull: Boolean = false): ByteArray {
        val toSend = ArrayList<PeerInfo>()
        toSend.add(PeerInfo(core.myIp))

        Collections.shuffle(view)

        //Move oldest H items to the end (from view)
        if (view.size > h) {
            for (i in 0..h - 1) {
                val oldest = view.subList(0, view.size - i).maxBy { it.age }
                Collections.swap(view, view.indexOf(oldest), view.size - (i + 1))
            }
        }

        //Append the  exch-1 element from view to toSend
        if (view.size > exch - 1) {
            for (i in 0..exch - 1 - 1) {
                val peer = view[i]
                toSend.add(peer)
            }
        } else {
            toSend.addAll(view)
        }
        logger.debug("toSend size in address: ${toSend.size}")
        return asByteArray(isPull, toSend)
    }

    /**
     * We never should have to split the view. As a PeerInfo is 8Bytes maximum,
     * A boolean is usually 2Bytes and the size is 4Bytes maximum this gives us
     *
     * 6Bytes + 8Bytes per PeerInfo (If we had a view of 100 (exch = 50) and a max Packet size of
     * 512Bytes => Give 12Bytes for the Boolean and size
     *
     * 500 Bytes / 8Bytes = 62.5 PeerInfos
     *
     * For a total Datagram Packet Size of 512Bytes Payload + IPV4 Header (20Bytes) + UDP Header (8Bytes)
     * => 540 Bytes which is under 576Bytes
     *
     * Source : http://stackoverflow.com/questions/900697/how-to-find-the-largest-udp-packet-i-can-send-without-fragmenting
     */
    private fun asByteArray(isPull: Boolean, toSend: ArrayList<PeerInfo>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)

        try {
            out.writeBoolean(isPull)
            out.writeInt(toSend.size)
            toSend.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Error serializing the PeerInfos", e)
        } finally {
            out.close()
        }
        logger.debug("toSendView size in Bytes: ${byteOut.toByteArray().size}")
        return byteOut.toByteArray()
    }

    /**
     * Select which peer to keep from the view received
     *
     * @param receivedView  the received view
     */
    fun selectToKeep(receivedView: ArrayList<PeerInfo>) {
        //remove duplicates from view
        val toRemove = ArrayList<PeerInfo>()
        for (peer in receivedView) {
            val isDuplicate = removeDuplicate(peer)
            if (isDuplicate) toRemove.add(peer)
        }
        receivedView.removeAll(toRemove)
        //merge view and received in an arrayList
        view.addAll(receivedView)
        //remove min(H, #view-c) oldest items
        var minimum = Math.min(h, view.size - c)
        while (minimum > 0 && view.size > 0) {
            val oldestPeer = view.maxBy { it.age }
            view.remove(oldestPeer)
            minimum--
        }

        //remove the min(S, #view -c) head items from view
        minimum = Math.min(s, view.size - c)
        while (minimum > 0 && view.size > 0) {
            view.removeAt(0)
            minimum--
        }

        while (view.size > c) {
            view.removeAt(rand.nextInt(view.size))
        }
    }

    private fun removeDuplicate(info: PeerInfo) =
            if (view.contains(info) && view[view.indexOf(info)].age <= info.age)
                true
            else if (view.contains(info)) {
                view[view.indexOf(info)].age = info.age
                true
            } else {
                false
            }

    /**
     * Select a partner randomly from the view
     */
    fun selectPartner() = view[rand.nextInt(view.size)]


    private fun debug() {
        if (logger.isDebugEnabled) {
            val sj = StringJoiner(" ", "PSS View: ", "")
            sj.add(core.myIp.hostAddress)
            view.forEach { sj.add(it.address.hostAddress) }
            logger.debug(sj.toString())
            logger.debug("View size : ${view.size}")
        }
    }

    /**
     * Data class used to represent a peer
     *
     * @property address the address of the peer
     * @property age the age of the peer
     */
    data class PeerInfo(val address: InetAddress, var age: Int = 0) : Serializable {

        /**
         * Serialize a peer to the provided output stream
         *
         * @param out an FSTObjectOutput stream
         */
        fun serialize(out: FSTObjectOutput): Unit {
            out.write(address.address)
            out.writeInt(age)
        }
    }
}


