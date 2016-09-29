package epto.udp

import epto.utilities.Event
import org.nustaq.serialization.FSTObjectOutput
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by jocelyn on 29.09.16.
 */
class Gossip(val core: Core, val K: Int = 15){

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

        //TODO use K and select randomly
        core.pss.view.forEach { peerInfo ->
            core.send(byteOut.toByteArray(), peerInfo.address)
        }
    }
}