package epto.udp

import epto.pss.PeerSamplingService
import epto.utilities.Event
import org.nustaq.serialization.FSTObjectOutput
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by jocelyn on 29.09.16.
 */
class Gossip(val core: Core, val K: Int = 15) {

    fun relay(nextBall: HashMap<UUID, Event>) {
        val byteOut = ByteArrayOutputStream()
        val out = FSTObjectOutput(byteOut)
        try {
            out.writeObject(nextBall)
            out.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            out.close()
        }

        println("Ball size in Bytes: ${byteOut.size()}")

        selectKFromView().forEach {
            core.send(byteOut.toByteArray(), it.address)
        }
    }

    private fun selectKFromView(): ArrayList<PeerSamplingService.PeerInfo> {
        val tmpList = ArrayList<PeerSamplingService.PeerInfo>(core.pss.view)
        Collections.shuffle(tmpList)
        tmpList.removeIf { tmpList.indexOf(it) > (K - 1) }
        return tmpList
    }
}