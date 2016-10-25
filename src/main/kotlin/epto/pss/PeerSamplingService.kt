package epto.pss

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import epto.Application
import epto.Event
import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService.PeerInfo
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
 * @property trackerURL the base URL of the tracker
 *
 * @throws PSSInitializationException If (h + s) > exch
 *
 * @see Core
 * @see PassiveThread
 *
 * @author Jocelyn Thode
 */
class PeerSamplingService(var gossipInterval: Int, val core: Core, val c: Int = 20, val exch: Int = 10,
                          val s: Int = 8, val h: Int = 2, trackerURL: String) {

    private val logger by logger()

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
            val toSend = selectToSend(true)
            core.sendPss(toSend, partner.address)
        }
    }
    private var activeThreadFuture: ScheduledFuture<*>? = null

    init {
        if (h + s > exch) throw PSSInitializationException("(H + S) must not be higher than exch !")
        var result: String?
        var tmp_view: MutableList<String> = ArrayList()
        FuelManager.instance.basePath = trackerURL
        var retry = 0
        do {
            try {
                Thread.sleep(10000)
                result = "/REST/v1/admin/get_view".httpGet().timeout(20000).timeoutRead(60000).responseString().third.get()
                tmp_view = result.split('|').toMutableList()
                logger.debug(result)
            } catch (e: Exception) {
                logger.error("Error while trying to get a view from the tracker", e)
                retry++
                if (retry > 25) {
                    logger.error("Too many retries, Aborting...")
                    System.exit(1)
                }
            }
        } while (tmp_view.size < c)

        //Add seeds to the PSS view
        view.addAll(tmp_view.distinct().map { PeerSamplingService.PeerInfo(InetAddress.getByName(it)) })
    }

    /**
     * Start the Peer Sampling Service
     */
    fun start() {
        Thread(passiveThread).start()
        //Run the PSS 4 times
        for (i in 1..4) {
            logger.debug("Running init PSS-{}", i)
            activeThread.run()
            Thread.sleep(1000)
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
     * @param isPush If the view is sent unsolicited
     *
     * @return subview
     */
    fun selectToSend(isPush: Boolean = true): ByteArray {
        val toSend = ArrayList<PeerInfo>()

        Collections.shuffle(view)

        //Move oldest H items to the end (from view)
        if (view.size > h) {
            for (i in 0..h - 1) {
                val oldest = view.subList(0, view.size - i).maxBy { it.age }
                Collections.swap(view, view.indexOf(oldest), view.size - (i + 1))
            }
        }

        //Append the exch-1 element from view to toSend
        if (view.size > exch - 1) {
            for (i in 0..(exch - 2)) {
                val peer = view[i]
                toSend.add(peer)
            }
        }
        toSend.add(PeerInfo(core.myIp))
        return asByteArray(isPush, toSend)
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
    private fun asByteArray(isPush: Boolean, toSend: ArrayList<PeerInfo>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)

        try {
            out.writeBoolean(isPush)
            out.writeInt(toSend.size)
            toSend.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Error serializing the PeerInfos", e)
        } finally {
            out.close()
        }
        //logger.debug("toSendView size in Bytes: {}", byteOut.toByteArray().size)
        return byteOut.toByteArray()
    }

    /**
     * Select which peer to keep from the view received
     *
     * @param receivedView  the received view
     */
    fun selectToKeep(receivedView: ArrayList<PeerInfo>,  isPush: Boolean) {
        //merge view and received
        view.addAll(receivedView)
        //remove duplicates from view
        removeDuplicates()

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
        if (!isPush) view.forEach { it.age++ }
        //logger.debug("View after selectToKeep: {}", view)
    }

    /**
     * We need to remove in this way to make sure we keep the same order, as when we remove nodes
     * we don't want to remove the node that sent us a message otherwise they would slowly kill all their indegrees
     */
    private fun removeDuplicates() {
        var i = 0
        while (i < view.size - 1) {
            for (j in (i + 1)..(view.size - 1)) {
                if (view[i].address == view[j].address) {
                    if (view[i].age >= view[j].age) {
                        view.remove(view[i])
                    } else {
                        view.remove(view[j])
                    }
                    i-- // if there are more than one duplicate
                    break
                }
            }
            i++
        }
    }

    /**
     * Select a partner randomly from the view
     *
     * @return a randomly selected PeerInfo
     */
    fun selectPartner() = view[rand.nextInt(view.size)]


    private fun debug() {
        if (logger.isDebugEnabled) {
            val sj = StringJoiner(" ", "PSS View: ", "")
            sj.add(core.myIp.hostAddress)
            view.forEach {
                sj.add(it.address.hostAddress)
                if (it.address == core.myIp) logger.error("View contains our own IP!")
            }
            logger.debug(sj.toString())
            logger.debug("View size : ${view.size}")
            logger.debug("Distinct view size: ${view.distinctBy { it.address }.size}")
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
    private class PSSInitializationException(s: String) : Throwable(s) {}
}


