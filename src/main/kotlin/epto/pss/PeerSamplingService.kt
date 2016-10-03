package epto.pss

import epto.libs.Delegates.logger
import epto.udp.Core
import epto.utilities.Application
import java.io.Serializable
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Implementation of a Peer Sampling Service
 *
 * @param gossipInterval the interval at which to perform the active thread of the PSS
 *
 * @param core the Class responsible for the datagram channels
 *
 * @param c the ideal view size
 *
 * @param exch the number of peers to exch
 *
 * @param s the swapping parameter
 *
 * @param h the healing parameter
 */
class PeerSamplingService(var gossipInterval: Int, val core: Core, val c: Int = 30, val exch: Int = 14,
                          val s: Int = 6, val h: Int = 1) {

    val logger by logger()

    val view = ArrayList<PeerInfo>()
    val pssLock = Any()
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
        activeThreadFuture = scheduler.scheduleWithFixedDelay(activeThread, 0, gossipInterval.toLong(),
                TimeUnit.MILLISECONDS)
    }

    /**
     * Stop the PSS
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
                val oldest = Collections.max(view.subList(0, view.size - i)) { o1, o2 -> o1.age - o2.age }
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
        return Application.conf.asByteArray(Pair(isPull, toSend))
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
            val oldestPeer = Collections.max(view) { o1, o2 -> o1.age - o2.age }
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
            view.forEach { sj.add(it.address.hostAddress) }
            logger.debug(sj.toString())
            logger.debug("View size : ${view.size}")
        }
    }

    /**
     * Data class used to represent a peer
     */
    data class PeerInfo(val address: InetAddress, var age: Int = 0) : Serializable
}


