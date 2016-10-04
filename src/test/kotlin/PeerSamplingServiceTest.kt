import epto.pss.PeerSamplingService
import epto.udp.Core
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

/**
 * Class testing the events methods
 */
class PeerSamplingServiceTest {

    private lateinit var core1: Core
    private lateinit var core2: Core
    private lateinit var core3: Core
    private lateinit var core4: Core
    private lateinit var core5: Core


    @Before
    @Throws(IOException::class)
    fun setup() {
        core1 = Core(InetAddress.getByName("localhost"), 1)
        core2 = Core(InetAddress.getByName("localhost"), 1, 11353, 11453)
        core3 = Core(InetAddress.getByName("localhost"), 1, 12353, 12453)
        core4 = Core(InetAddress.getByName("localhost"), 1, 13353, 13453)
        core5 = Core(InetAddress.getByName("localhost"), 1, 14353, 14453)

        core1.pss.gossipInterval = 500
        core2.pss.gossipInterval = 500
        core3.pss.gossipInterval = 500
        core4.pss.gossipInterval = 500
        core5.pss.gossipInterval = 500


        core1.pss.view.addAll(arrayListOf(
                PeerSamplingService.PeerInfo(core2.myIp),
                PeerSamplingService.PeerInfo(core3.myIp),
                PeerSamplingService.PeerInfo(core4.myIp),
                PeerSamplingService.PeerInfo(core5.myIp)
        ))
        core2.pss.view.addAll(arrayListOf(
                PeerSamplingService.PeerInfo(core1.myIp),
                PeerSamplingService.PeerInfo(core3.myIp),
                PeerSamplingService.PeerInfo(core4.myIp),
                PeerSamplingService.PeerInfo(core5.myIp)
        ))
        core3.pss.view.addAll(arrayListOf(
                PeerSamplingService.PeerInfo(core2.myIp),
                PeerSamplingService.PeerInfo(core1.myIp),
                PeerSamplingService.PeerInfo(core4.myIp),
                PeerSamplingService.PeerInfo(core5.myIp)
        ))
        core4.pss.view.addAll(arrayListOf(
                PeerSamplingService.PeerInfo(core2.myIp),
                PeerSamplingService.PeerInfo(core3.myIp),
                PeerSamplingService.PeerInfo(core1.myIp),
                PeerSamplingService.PeerInfo(core5.myIp)
        ))
        core5.pss.view.addAll(arrayListOf(
                PeerSamplingService.PeerInfo(core2.myIp),
                PeerSamplingService.PeerInfo(core3.myIp),
                PeerSamplingService.PeerInfo(core4.myIp),
                PeerSamplingService.PeerInfo(core1.myIp)
        ))

        core1.startPss()
        //core2.startPss()
        //core3.startPss()
        //core4.startPss()
    }

    @Test
    fun testPSS() {
        Thread.sleep(5000)
        Thread.sleep(10000)
    }
}
