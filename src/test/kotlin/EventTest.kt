import epto.utilities.Application
import epto.utilities.Event
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.nustaq.serialization.FSTObjectInput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.GZIPInputStream
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

        event4 = Event(UUID(2951, 29942), 12523452345, 87, UUID(67754, 252453))
        event5 = Event(UUID(444344, 324645), 94845423418, 143, UUID(3456745, 453568))
        event6 = Event(UUID(847392, 848123), 2345, 9, UUID(2662, 57245))
        event7 = Event(UUID(255333, 233123), 59595, 45, UUID(61856, 8454))
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
    fun testEventOverhead()  {
        val byteOut = ByteArrayOutputStream()
        val gzipOut = GZIPOutputStream(byteOut, 65536)
        val out = Application.conf.getObjectOutput(byteOut)


        out.writeObject(event4, Event::class.java)
        out.flush()
        println(byteOut.toByteArray().size)
        out.writeObject(event5, Event::class.java)
        out.flush()
        println(byteOut.toByteArray().size)
        out.writeObject(event6, Event::class.java)
        out.flush()
        println(byteOut.toByteArray().size)
        out.writeObject(event7, Event::class.java)
        out.flush()
        println(byteOut.toByteArray().size)

        println("---------------")

        val byteOut1 = ByteArrayOutputStream()
        val gzipOut1 = GZIPOutputStream(byteOut1, 65536)
        val out1 = Application.conf.getObjectOutput(byteOut1)

        out1.writeLong(event4.id.mostSignificantBits)
        out1.writeLong(event4.id.leastSignificantBits)
        out1.writeLong(event4.timestamp)
        out1.writeInt(event4.ttl)
        out1.writeLong(event4.sourceId!!.mostSignificantBits)
        out1.writeLong(event4.sourceId!!.leastSignificantBits)
        out1.flush()
        println(byteOut1.toByteArray().size)
        out1.writeLong(event5.id.mostSignificantBits)
        out1.writeLong(event5.id.leastSignificantBits)
        out1.writeLong(event5.timestamp)
        out1.writeInt(event5.ttl)
        out1.writeLong(event5.sourceId!!.mostSignificantBits)
        out1.writeLong(event5.sourceId!!.leastSignificantBits)
        out1.flush()
        println(byteOut1.toByteArray().size)
        out1.writeLong(event6.id.mostSignificantBits)
        out1.writeLong(event6.id.leastSignificantBits)
        out1.writeLong(event6.timestamp)
        out1.writeInt(event6.ttl)
        out1.writeLong(event6.sourceId!!.mostSignificantBits)
        out1.writeLong(event6.sourceId!!.leastSignificantBits)
        out1.flush()
        println(byteOut1.toByteArray().size)
        out1.writeLong(event7.id.mostSignificantBits)
        out1.writeLong(event7.id.leastSignificantBits)
        out1.writeLong(event7.timestamp)
        out1.writeInt(event7.ttl)
        out1.writeLong(event7.sourceId!!.mostSignificantBits)
        out1.writeLong(event7.sourceId!!.leastSignificantBits)
        out1.flush()
        println(byteOut1.toByteArray().size)

        println("---------------")

        val byteOut2 = ByteArrayOutputStream()
        val gzipOut2 = GZIPOutputStream(byteOut2, 65536)
        val out2 = Application.conf.getObjectOutput(byteOut2)

        out2.writeObject(event4.id,  UUID::class.java)
        out2.writeLong(event4.timestamp)
        out2.writeInt(event4.ttl)
        out2.writeObject(event4.sourceId,  UUID::class.java)
        out2.flush()
        println(byteOut2.toByteArray().size)
        out2.writeObject(event5.id,  UUID::class.java)
        out2.writeLong(event5.timestamp)
        out2.writeInt(event5.ttl)
        out2.writeObject(event5.sourceId,  UUID::class.java)
        out2.flush()
        println(byteOut2.toByteArray().size)
        out2.writeObject(event6.id,  UUID::class.java)
        out2.writeLong(event6.timestamp)
        out2.writeInt(event6.ttl)
        out2.writeObject(event6.sourceId,  UUID::class.java)
        out2.flush()
        println(byteOut2.toByteArray().size)
        out2.writeObject(event7.id,  UUID::class.java)
        out2.writeLong(event7.timestamp)
        out2.writeInt(event7.ttl)
        out2.writeObject(event7.sourceId,  UUID::class.java)
        out2.flush()
        println(byteOut2.toByteArray().size)

    }
}
