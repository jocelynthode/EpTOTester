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

    fun relay(nextBall: HashMap<UUID, Event>) {
        if (core.pss.view.size < K) {
            logger.error("View should be at least equal to K")
            return
        }
        val byteOut = ByteArrayOutputStream()
        val gzipOut = GZIPOutputStream(byteOut)
        val out = Application.conf.getObjectOutput(gzipOut)
        try {
            out.writeInt(nextBall.size)
            nextBall.forEach { uuid, event -> out.writeObject(event, Event::class.java) }
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending next ball", e)
            e.printStackTrace()
        } finally {
            out.close()
        }

        logger.debug("Ball size in Bytes: ${byteOut.size()}")
        if (byteOut.size() >= 65507) {
            logger.debug("Ball size too big !")
        }
        selectKFromView().forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
    }

    private fun selectKFromView(): ArrayList<PeerInfo> {
        val tmpList = ArrayList<PeerInfo>(core.pss.view)
        Collections.shuffle(tmpList)
        tmpList.removeIf { tmpList.indexOf(it) > (K - 1) }
        logger.debug("KList size: ${tmpList.size}")
        return tmpList
    }
}