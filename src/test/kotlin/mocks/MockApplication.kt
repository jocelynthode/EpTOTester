package mocks

import epto.utilities.Event
import net.sf.neem.impl.Application
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jocelyn on 05.05.16.
 */
class MockApplication : Application {

    var events: MutableList<UUID> = Collections.synchronizedList(ArrayList<UUID>())

    override fun deliver(byteBuffers: Array<ByteBuffer>) {
        for (byteBuffer in byteBuffers) {
            val content = byteBuffer.array()
            val byteIn = ByteArrayInputStream(content)
            try {
                val `in` = FSTObjectInput(byteIn)
                val event = `in`.readObject() as Event
                `in`.close()
                events.add(event.id)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }

        }
    }
}

