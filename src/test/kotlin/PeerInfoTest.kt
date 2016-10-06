import epto.pss.PeerSamplingService
import epto.pss.PeerSamplingService.PeerInfo
import epto.utilities.Application
import epto.utilities.Event
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.*
/**
 * Created by jocelyn on 06.10.16.
 */
class PeerInfoTest {

    private lateinit var peer: PeerInfo
    private lateinit var peer1: PeerInfo
    private lateinit var peer2: PeerInfo
    private lateinit var peer3: PeerInfo
    private var bool: Boolean = true
    private var bool1: Boolean = true
    private var bool2: Boolean = false
    private var bool3: Boolean = false

    @Before
    @Throws(IOException::class)
    fun setup() {
        peer = PeerInfo(InetAddress.getByName("192.168.1.201"), 5)
        peer1 = PeerInfo(InetAddress.getByName("8.8.8.8"), 10)
        peer2 = PeerInfo(InetAddress.getByName("localhost"), 2)
        peer3 = PeerInfo(InetAddress.getByName("localhost"), 7)
    }

    @Test
    fun testSerialization() {
        var byteOut = ByteArrayOutputStream()
        var out = Application.conf.getObjectOutput(byteOut)

        out.writeBoolean(bool)
        out.writeInt(50)
        peer.serialize(out)
        out.close()
        println(byteOut.toByteArray().size)

        byteOut = ByteArrayOutputStream()
        out = Application.conf.getObjectOutput(byteOut)
        out.writeBoolean(bool1)
        out.writeInt(100)
        peer1.serialize(out)
        out.close()
        println(byteOut.toByteArray().size)

        byteOut = ByteArrayOutputStream()
        out = Application.conf.getObjectOutput(byteOut)
        out.writeBoolean(bool2)
        out.writeInt(10)
        peer2.serialize(out)
        out.close()
        println(byteOut.toByteArray().size)

        byteOut = ByteArrayOutputStream()
        out = Application.conf.getObjectOutput(byteOut)
        out.writeBoolean(bool3)
        out.writeInt(5)
        peer3.serialize(out)
        out.close()
        println(byteOut.toByteArray().size)
    }
}