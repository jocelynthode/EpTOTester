package epto.pss

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
 * Created by jocelyn on 29.09.16.
 */
class PeerSamplingService(val gossipInterval: Int, val core: Core) {

    val view = ArrayList<PeerInfo>()
    val c = 30
    val exch = 14
    val s = 10
    val h = 3
    val pssLock = Object()
    val passiveThread = PassiveThread(pssLock, this, core)
    private val rand = Random()
    private val scheduler = Executors.newScheduledThreadPool(1)
    private val activeThread = Runnable {
        if (view.size < 2) {
            println("Not enough peers to shuffle")
            return@Runnable
        }
        synchronized(pssLock) {
            val partner = selectPartner()
            view.remove(partner)
            val toSend = selectToSend()
            core.sendPss(toSend, partner.address)
            view.forEach { it.age++ }
        }

    }
    private var activeThreadFuture: ScheduledFuture<*>? = null


    fun start() = {
        Thread(passiveThread).start()
        activeThreadFuture = scheduler.scheduleWithFixedDelay(activeThread, 0, gossipInterval.toLong()
                , TimeUnit.MILLISECONDS)
    }

    fun selectToSend(): ByteArray {
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
        //System.out.println("toSend size: "+toSend.size());
        val byteOut = ByteArrayOutputStream()
        val out = FSTObjectOutput(byteOut)
        try {
            out.writeObject(toSend)
            out.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            out.close()
        }
        return byteOut.toByteArray()
    }

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

    private fun removeDuplicate(info: PeerInfo): Boolean =
            if (view.contains(info) && view[view.indexOf(info)].age <= info.age)
                true
            else if (view.contains(info)) {
                view[view.indexOf(info)].age = info.age
                true
            } else {
                false
            }

    fun selectPartner() = view[rand.nextInt(view.size)]

    data class PeerInfo(val address: InetAddress, var age: Int = 0) : Serializable
}


