import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.github.mgunlogson.cuckoofilter4j.Utils
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
    private val MAX_KEYS = 100
    private val PEER_NUMBER = 1000.0
    private val FPP = 1.0 / (Math.pow(PEER_NUMBER, 4.0))
    private enum class EventFunnel : Funnel<Event> {
        INSTANCE;
        override fun funnel(from: Event, into: PrimitiveSink) {
            //We only use the identifier
            into.putLong(from.sourceId!!.mostSignificantBits)
                    .putLong(from.sourceId!!.leastSignificantBits)
                    .putInt(from.timestamp)
        }
    }
    private lateinit var cuckooFilter: CuckooFilter<Event>
    private lateinit var cuckooFilterDelta: CuckooFilter<Event>


    @Before
    @Throws(IOException::class)
    fun setup() {
        val sourceUUIDS = Array(PEER_NUMBER.toInt(), {EventCreator()})
        val rand = Random()
        (1..MAX_KEYS).forEach {
            val creator = sourceUUIDS[rand.nextInt(sourceUUIDS.size)]
            uuids.add(Event(UUID.randomUUID(), creator.incrementAndGet(),
                    (Math.random()*100).toInt(), creator.sourceID))
        }
        cuckooFilter = CuckooFilter.Builder(EventFunnel.INSTANCE, MAX_KEYS)
                .withFalsePositiveRate(FPP)
                .withExpectedConcurrency(1).build()
        cuckooFilterDelta = CuckooFilter.Builder(EventFunnel.INSTANCE, MAX_KEYS + 10)
                .withFalsePositiveRate(FPP)
                .withExpectedConcurrency(1).build()
    }

    @Test
    fun testCuckooInsertAll() {
        uuids.forEach {
            Assert.assertTrue("Cuckoo could not insert an event",
                    cuckooFilter.put(it))
        }
    }

    @Test
    fun testCuckooSize() {
        //println("Cuckoo storage size: ${cuckooFilter.storageSize / 8.0 } B")
        //println("Cuckoo max elems: ${cuckooFilter.actualCapacity}")

        uuids.forEach {
            Assert.assertTrue("Cuckoo could not insert an event",
                    cuckooFilter.put(it))
            Assert.assertTrue("CuckooDelta could not insert an event",
                    cuckooFilterDelta.put(it))
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
        println("Cuckoo size: ${byteOut.size()} B")

        val byteOut1 = ByteArrayOutputStream()
        val lz4Out1 = LZ4BlockOutputStream(byteOut1)
        val out1 = Application.conf.getObjectOutput(lz4Out1)
        try {

            out1.writeObject(cuckooFilterDelta)
            out1.flush()
        } catch (e: IOException) {
            throw e
        } finally {
            out1.close()
        }
        println("CuckooDelta size: ${byteOut1.size()} B")
    }

    @Test
    fun testNormalSize() {
        val byteOut = ByteArrayOutputStream()
        val lz4Out = LZ4BlockOutputStream(byteOut)
        val out = Application.conf.getObjectOutput(lz4Out)
        try {
            out.writeInt(uuids.size)
            uuids.forEach {
                out.writeLong(it.sourceId!!.mostSignificantBits)
                out.writeLong(it.sourceId!!.leastSignificantBits)
                out.writeInt(it.timestamp)
            }
            out.flush()
        } catch (e: IOException) {
            throw e
        } finally {
            out.close()
        }
        println("Normal size: ${byteOut.size()} B")
    }

    class EventCreator(val sourceID: UUID = UUID.randomUUID(), var timestamp: Int = 0) {
        fun incrementAndGet(): Int {
            timestamp++
            return timestamp
        }
    }

}