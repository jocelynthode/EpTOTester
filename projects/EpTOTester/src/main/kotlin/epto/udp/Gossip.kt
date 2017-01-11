package epto.udp

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import epto.Application
import epto.Event
import epto.libs.Utilities.logger
import epto.pss.PeerSamplingService.PeerInfo
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.util.*

/**
 * Class gossiping the EpTO messages
 *
 * @property core the Core
 *
 * @property K the gossip fanout
 * @property P the gossip fanin
 *
 * @see Core
 */
class Gossip(val core: Core, val K: Int, val P: Int) {

    private val logger by logger()


    //Ethernet MTU: 1500Bytes - 60Bytes for the max IP header - 8Bytes for the UDP header = 1432
    //An other acceptable size would be 508 (576Bytes - 68Bytes)
    // 576 comes from the minimum maximum reassembly buffer size
    internal val maxSize = 1432
    //an event is 40 Bytes max (id : 16Bytes, ts: 4Bytes, ttl: 4Bytes, srcId: 16Bytes)
    //We substract 4Bytes for the ball length
    //We substract 4Bytes for the messageType
    private val maxEvents = (maxSize - 8) / 40
    internal var totalSplits = 0

    /**
     * Relay a ball of event to other EpTO peers
     *
     * @param nextBall the ball of events to send
     */
    fun sendGossip(nextBall: List<Event>) {
        val kView = selectNFromView(K)
        val ballsToSend = Math.ceil(nextBall.size / maxEvents.toDouble()).toInt()

        logger.debug("Total Ball size in Events: {}", nextBall.size)
        logger.debug("Max Events: {}", maxEvents)
        logger.debug("KView size: {}, KView:  [{}]", kView.size, kView.joinToString())
        if (ballsToSend > 1) {
            relaySplitted(nextBall, ballsToSend, kView)
        } else {
            sendRelay(nextBall, kView)
        }
    }

    /**
     * Send a PullRequest to P EpTO peers
     *
     * @param timestamp the minimum even timestamp in which we are interested
     * @param filter the filter to be send to determine event we do not have
     */
    fun sendPullRequest(timestamp: Int, filter: CuckooFilter<Event>) {
        val pView = selectNFromView(P)
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)
        try {
            out.writeInt(MessageType.PULL_REQUEST.ordinal)
            out.writeInt(timestamp)
            out.writeObject(filter)
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while encoding pull request", e)
        } finally {
            out.close()
        }

        if (byteOut.size() > maxSize) {
            logger.warn("Packet size is too big !")
        }
        pView.forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
        core.pullRequestSent++
    }

    /**
     * Send the events requested by the EpTO peer
     *
     * @param eventsToSend the ball to send
     * @param target the requester address
     */
    fun  sendPullReply(eventsToSend: ArrayList<Event>, target: InetSocketAddress) {
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)
        try {
            out.writeInt(MessageType.PULL_REPLY.ordinal)
            out.writeInt(eventsToSend.size)
            eventsToSend.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending pull reply", e)
        } finally {
            out.close()
        }
        if (byteOut.size() > maxSize) {
            logger.warn("Ball size is too big !")
        }
        core.send(byteOut.toByteArray(), target.address)
        core.pullReplySent++
    }

    private fun sendRelay(nextBall: List<Event>, kView: ArrayList<PeerInfo>) {
        logger.debug("Relay Ball size in Events: {}", nextBall.size)
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)
        try {
            out.writeInt(MessageType.GOSSIP.ordinal)
            out.writeInt(nextBall.size)
            nextBall.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending next ball", e)
        } finally {
            out.close()
        }

        logger.debug("Ball size in Bytes: {}", byteOut.size())
        if (byteOut.size() > maxSize) {
            logger.warn("Ball size is too big !")
        }
        kView.forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
        core.gossipMessagesSent++
        logger.debug("Sent Ball")
    }

    private fun relaySplitted(values: List<Event>, ballsToSend: Int, kView: ArrayList<PeerInfo>) {
        var ballsNumber = ballsToSend
        totalSplits += ballsToSend
        var i = 0

        while (ballsNumber > 0) {
            logger.debug("ballsToSend: {}", ballsNumber)
            if (ballsNumber > 1) {
                sendRelay(values.subList(i, i + maxEvents), kView)
            } else {
                sendRelay(values.subList(i, values.size), kView)
            }
            ballsNumber--
            i += maxEvents
        }
    }

    private fun selectNFromView(n: Int): ArrayList<PeerInfo> {
        var nView = ArrayList<PeerInfo>()
        // We don't want the view to be modified during this time
        synchronized(core.pss.pssLock) {
            nView = ArrayList(core.pss.view)
        }
        if (nView.size < n) {
            logger.warn("View is smaller than size K ({})", core.pss.view.size)
            return nView
        }
        // As lists are small, this is no big deal
        Collections.shuffle(nView)
        return ArrayList(nView.subList(0,n))
    }


    companion object {
        enum class MessageType {
            GOSSIP,
            PULL_REQUEST,
            PULL_REPLY
        }
    }
}


