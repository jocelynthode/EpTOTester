package epto.udp

import epto.libs.Delegates.logger
import epto.pss.PeerSamplingService.PeerInfo
import epto.utilities.Application
import epto.utilities.Event
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * Created by jocelyn on 29.09.16.
 */
class Gossip(val core: Core, val K: Int = 15) {

    val logger by logger()

    val MAX_SIZE = 5000
    //260bits (for an event)
    val MAX_EVENTS = (MAX_SIZE * 8) / 260

    fun relay(nextBall: List<Event>) {
        if (core.pss.view.size < K) throw ViewSizeException("View is smaller than fanout K")

        val kView = selectKFromView()
        val ballsToSend = Math.ceil(nextBall.size / MAX_EVENTS.toDouble()).toInt()

        if (ballsToSend > 1) {
            relaySplitted(nextBall, ballsToSend, kView)
        } else {
            sendRelay(nextBall, kView)
        }
    }

    private fun sendRelay(nextBall: List<Event>, kView: ArrayList<PeerInfo>) {
        val byteOut = ByteArrayOutputStream()
        val gzipOut = GZIPOutputStream(byteOut, 8192)
        val out = Application.conf.getObjectOutput(gzipOut)
        try {
            out.writeInt(nextBall.size)
            nextBall.forEach { it.serialize(out) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending next ball", e)
        } finally {
            out.close()
        }

        logger.debug("Ball size in Events: ${nextBall.size}")
        logger.debug("Ball size in Bytes: ${byteOut.size()}")
        if (byteOut.size() > MAX_SIZE) {
            logger.warn("Ball size is too big !")
        }
        kView.forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
        logger.debug("Sent Ball")
    }

    private fun relaySplitted(values: List<Event>, ballsToSend: Int, kView: ArrayList<PeerInfo>) {
        var ballsToSend = ballsToSend
        var i = 0
        logger.debug("total Ball size: ${values.size}")
        logger.debug("ballsToSend: $ballsToSend")
        while (ballsToSend > 0) {
            if (ballsToSend > 1) {
                sendRelay(values.subList(i, i + MAX_EVENTS), kView)
            } else {
                sendRelay(values.subList(i, values.size), kView)
            }
            ballsToSend--
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

    class ViewSizeException(s: String) : Throwable() {}
}


