package epto.udp

import epto.libs.Delegates.logger
import epto.pss.PeerSamplingService.PeerInfo
import epto.utilities.Event
import org.nustaq.serialization.FSTObjectOutput
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

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
        val out = FSTObjectOutput(byteOut)
        try {
            out.writeObject(nextBall)
            out.flush()
        } catch (e: IOException) {
            logger.error("Exception while sending next ball", e)
            e.printStackTrace()
        } finally {
            out.close()
        }

        logger.debug("Ball size in Bytes: ${byteOut.size()}")
        selectKFromView().forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
    }

    private fun selectKFromView(): ArrayList<PeerInfo> {
        val tmpList = ArrayList<PeerInfo>(core.pss.view)
        Collections.shuffle(tmpList)
        tmpList.removeIf { tmpList.indexOf(it) > (K - 1) }
        return tmpList
    }
}