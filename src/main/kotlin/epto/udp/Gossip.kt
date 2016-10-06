package epto.udp

import epto.libs.Delegates.logger
import epto.pss.PeerSamplingService.PeerInfo
import epto.utilities.Application
import epto.utilities.Event
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by jocelyn on 29.09.16.
 */
class Gossip(val core: Core, val K: Int = 15) {

    val logger by logger()


    //an event is 40 Bytes max (id : 16Bytes, ts: 4Bytes, ttl: 4Bytes, srcId: 16Bytes)
    //We substract 4Bytes for the ball size
    val MAX_EVENTS = (core.MAX_SIZE - 4) / 40

    fun relay(nextBall: List<Event>) {
        if (core.pss.view.size < K) throw ViewSizeException("View is smaller than fanout K")

        val kView = selectKFromView()
        val ballsToSend = Math.ceil(nextBall.size / MAX_EVENTS.toDouble()).toInt()

        logger.debug("Total Ball size in Events: ${nextBall.size}")
        if (ballsToSend > 1) {
            relaySplitted(nextBall, ballsToSend, kView)
        } else {
            sendRelay(nextBall, kView)
        }
    }

    private fun sendRelay(nextBall: List<Event>, kView: ArrayList<PeerInfo>) {
        logger.debug("Relay Ball size in Events: ${nextBall.size}")
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)
        try {
            out.writeInt(nextBall.size)
            nextBall.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending next ball", e)
        } finally {
            out.close()
        }

        logger.debug("Ball size in Bytes: ${byteOut.size()}")
        if (byteOut.size() > core.MAX_SIZE) {
            logger.warn("Ball size is too big !")
        }
        kView.forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
        logger.debug("Sent Ball")
    }

    private fun relaySplitted(values: List<Event>, ballsToSend: Int, kView: ArrayList<PeerInfo>) {
        var ballsNumber = ballsToSend
        var i = 0

        while (ballsNumber > 0) {
            logger.debug("ballsToSend: $ballsNumber")
            if (ballsNumber > 1) {
                sendRelay(values.subList(i, i + MAX_EVENTS), kView)
            } else {
                sendRelay(values.subList(i, values.size), kView)
            }
            ballsNumber--
            i += MAX_EVENTS
        }
    }

    private fun selectKFromView(): ArrayList<PeerInfo> {
        val tmpList = ArrayList<PeerInfo>(core.pss.view)
        Collections.shuffle(tmpList)
        tmpList.removeIf { tmpList.indexOf(it) > (K - 1) }
        logger.debug("KList size: ${tmpList.size}")
        return tmpList
    }

    class ViewSizeException(s: String) : Throwable(s) {}
}


