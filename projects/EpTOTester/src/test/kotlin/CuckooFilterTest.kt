import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Funnel
import com.google.common.hash.PrimitiveSink
import epto.Application
import epto.Event
import net.jpountz.lz4.LZ4BlockOutputStream
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import net.jpountz.lz4.LZ4Factory





/**
 * Created by jocelyn on 11.01.17.
 */
class CuckooFilterTest {
    private val uuids = ArrayList<Event>()
    private val MAX_KEYS = 500
    private val FPP = Math.pow(10.0, -3.0)//1.0 / (Math.pow(1000.0, 4.0))
    private object EventFunnel : Funnel<Event> {
        override fun funnel(from: Event, into: PrimitiveSink) {
            //We only use the identifier
            into.putLong(from.sourceId!!.mostSignificantBits)
                    .putLong(from.sourceId!!.leastSignificantBits)
                    .putInt(from.timestamp)
        }
    }
    private lateinit var cuckooFilter: CuckooFilter<Event>


    @Before
    @Throws(IOException::class)
    fun setup() {
        val sourceUUIDS = Array(200, {EventCreator()})
        val rand = Random()
        (1..MAX_KEYS).forEach {
            val creator = sourceUUIDS[rand.nextInt(sourceUUIDS.size)]
            uuids.add(Event(UUID.randomUUID(), creator.incrementAndGet(),
                    (Math.random()*100).toInt(), creator.sourceID))
        }
        cuckooFilter = CuckooFilter.Builder(EventFunnel, MAX_KEYS)
                .withFalsePositiveRate(FPP).build()
    }

    @Test
    fun testCuckooSize() {
        println("Cuckoo storage size: ${cuckooFilter.storageSize / 8.0 } MB")
        println("Cuckoo max elems: ${cuckooFilter.actualCapacity}")

        uuids.forEach {
            Assert.assertTrue("Cuckoo could not insert an event", cuckooFilter.put(it))
        }

        //TODO maybe use other compression algorithm
        val byteOut = ByteArrayOutputStream()
        val lz4Out = LZ4BlockOutputStream(byteOut)
        val out = Application.conf.getObjectOutput(lz4Out)
        try {
            out.writeObject(cuckooFilter)
            out.flush()
        } catch (e: IOException) {
            throw e
        } finally {
            out.close()
        }
        println("Cuckoo size: ${byteOut.size()/ 1000000.0} MB")
    }

    @Test
    fun testCuckooInsertAll() {
        uuids.forEach {
            Assert.assertTrue("Cuckoo could not insert an event", cuckooFilter.put(it))
        }
    }

    class EventCreator(val sourceID: UUID = UUID.randomUUID(), var timestamp: Int = 0) {
        fun incrementAndGet(): Int {
            timestamp++
            return timestamp
        }
    }

}