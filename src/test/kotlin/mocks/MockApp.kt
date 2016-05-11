package mocks

import epto.utilities.App
import epto.utilities.Event
import net.sf.neem.MulticastChannel
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jocelyn on 05.05.16.
 */
class MockApp(neem: MulticastChannel, TTL: Int, K: Int) : App(neem, TTL, K) {

    var events: MutableList<UUID> = Collections.synchronizedList(ArrayList<UUID>())

    @Throws(InterruptedException::class)
    override fun broadcast(event: Event?) {
        var event = event
        Thread.sleep(200)
        if (event == null) event = Event(UUID.randomUUID(), 0, 0, null)
        this.peer.disseminationComponent.broadcast(event)
    }

    override fun deliver(byteBuffers: Array<ByteBuffer>) {
        for (byteBuffer in byteBuffers) {
            val content = byteBuffer.array()
            val byteIn = ByteArrayInputStream(content)
            try {
                val `in` = ObjectInputStream(byteIn)
                val event = `in`.readObject() as Event
                events.add(event.id)
                println(event.toString())
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }

        }
    }
}

