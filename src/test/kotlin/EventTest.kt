import epto.utilities.Application
import epto.utilities.Event
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * Class testing the events methods
 */
class EventTest {

    private lateinit var event: Event
    private lateinit var event1: Event
    private lateinit var event2: Event
    private lateinit var event3: Event
    private lateinit var event4: Event
    private lateinit var event5: Event
    private lateinit var event6: Event
    private lateinit var event7: Event

    @Before
    @Throws(IOException::class)
    fun setup() {
        event = Event(UUID(23333, 233123), 1, 5, UUID(1111, 2222))
        event1 = Event(UUID(44444, 324645), 2, 5, UUID(4444, 2222))
        event2 = Event(UUID(847392, 848123), 1, 5, UUID(4444, 2222))
        event3 = Event(UUID(23333, 233123), 1, 5, UUID(1111, 2222))
    }

    @Test
    fun testCompareTo() {
        Assert.assertTrue(event.compareTo(event3) == 0)
        Assert.assertTrue(event.compareTo(event1) == -1)
        Assert.assertTrue(event.compareTo(event2) == -1)
        Assert.assertTrue(event1.compareTo(event2) == 1)
    }

    @Test
    fun testEquals() {
        Assert.assertTrue(event == event3)
        Assert.assertTrue(event == event)
        Assert.assertFalse(event == event1)
    }

    @Test
    fun testSerializeUnserialize() {
        val byteOut = ByteArrayOutputStream()
        val out = Application.conf.getObjectOutput(byteOut)

        event.serialize(out)
        out.close()

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val inputStream = FSTObjectInput(byteIn)

        val test = Event.unserialize(inputStream)
        Assert.assertEquals(event, test)
    }

    @Test
    fun testEventOverhead() {
        val EVENTS = 20
        val rand = Random()
        val byteOut = ByteArrayOutputStream()
        val gzipOut = GZIPOutputStream(byteOut)
        val out = Application.conf.getObjectOutput(byteOut)

        for (i in 1..EVENTS) {
            val event = Event(UUID.randomUUID(), rand.nextInt(10), rand.nextInt(30), UUID.randomUUID())
            out.writeObject(event, Event::class.java)
        }
        out.close()
        println("Average event size: ${byteOut.toByteArray().size.toDouble() / EVENTS}")

        println("---------------")

        val byteOut1 = ByteArrayOutputStream()
        val gzipOut1 = GZIPOutputStream(byteOut1)
        val out1 = Application.conf.getObjectOutput(byteOut1)
        for (i in 1..EVENTS) {
            val id = UUID.randomUUID()
            val timestamp = rand.nextInt(10)
            val ttl = rand.nextInt(30)
            val sourceId = UUID.randomUUID()
            out1.writeLong(id.mostSignificantBits)
            out1.writeLong(id.leastSignificantBits)
            out1.writeInt(timestamp)
            out1.writeInt(ttl)
            out1.writeLong(sourceId.mostSignificantBits)
            out1.writeLong(sourceId.leastSignificantBits)
        }
        out1.close()
        println("Average event size: ${byteOut1.toByteArray().size.toDouble() / EVENTS}")


        println("---------------")

        val byteOut2 = ByteArrayOutputStream()
        val gzipOut2 = GZIPOutputStream(byteOut2)
        val out2 = Application.conf.getObjectOutput(byteOut2)
        for (i in 1..EVENTS) {
            val id = UUID.randomUUID()
            val timestamp = rand.nextInt(10)
            val ttl = rand.nextInt(30)
            val sourceId = UUID.randomUUID()
            out2.writeObject(id, UUID::class.java)
            out2.writeInt(timestamp)
            out2.writeInt(ttl)
            out2.writeObject(sourceId, UUID::class.java)
        }
        out2.close()
        println("Average event size: ${byteOut2.toByteArray().size.toDouble() / EVENTS}")
    }
}
