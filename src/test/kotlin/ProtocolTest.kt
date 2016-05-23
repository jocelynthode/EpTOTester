import epto.utilities.Event
import mocks.MockApp
import net.sf.neem.MulticastChannel
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.*

/**
 * Class testing the whole protocol
 */
class ProtocolTest {

    private lateinit var app: MockApp
    private lateinit var app1: MockApp
    private lateinit var app2: MockApp
    private lateinit var app3: MockApp
    private lateinit var app4: MockApp
    private lateinit var app5: MockApp
    private lateinit var neem: MulticastChannel
    private lateinit var neem1: MulticastChannel
    private lateinit var neem2: MulticastChannel
    private lateinit var neem3: MulticastChannel
    private lateinit var neem4: MulticastChannel
    private lateinit var neem5: MulticastChannel

    @Before
    @Throws(Exception::class)
    fun setUp() {

        val ttl = 30
        val k = 4

        val address10000 = InetSocketAddress("localhost", 10000)
        val address10001 = InetSocketAddress("localhost", 10001)
        val address10002 = InetSocketAddress("localhost", 10002)
        val address10003 = InetSocketAddress("localhost", 10003)
        val address10004 = InetSocketAddress("localhost", 10004)
        val address10005 = InetSocketAddress("localhost", 10005)

        val addresses0 = arrayOf(address10001, address10002, address10003, address10004, address10005)
        val addresses1 = arrayOf(address10000, address10002, address10003, address10004, address10005)
        val addresses2 = arrayOf(address10000, address10001, address10003, address10004, address10005)
        val addresses3 = arrayOf(address10000, address10001, address10002, address10004, address10005)
        val addresses4 = arrayOf(address10000, address10001, address10002, address10003, address10005)
        val addresses5 = arrayOf(address10000, address10001, address10002, address10003, address10004)

        neem = MulticastChannel(address10000)
        neem1 = MulticastChannel(address10001)
        neem2 = MulticastChannel(address10002)
        neem3 = MulticastChannel(address10003)
        neem4 = MulticastChannel(address10004)
        neem5 = MulticastChannel(address10005)

        for (address in addresses0) {
            neem.connect(address)
        }

        for (address in addresses1) {
            neem1.connect(address)
        }

        for (address in addresses2) {
            neem2.connect(address)
        }

        for (address in addresses3) {
            neem3.connect(address)
        }

        for (address in addresses4) {
            neem4.connect(address)
        }

        for (address in addresses5) {
            neem5.connect(address)
        }


        app = MockApp(neem, ttl, k)
        app1 = MockApp(neem1, ttl, k)
        app2 = MockApp(neem2, ttl, k)
        app3 = MockApp(neem3, ttl, k)
        app4 = MockApp(neem4, ttl, k)
        app5 = MockApp(neem5, ttl, k)

        app.start()
        app1.start()
        app2.start()
        app3.start()
        app4.start()
        app5.start()

    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        neem.close()
        neem1.close()
        neem2.close()
    }

    //TODO for now compare if order is the same everywhere maybe make it so we can compare order too
    @Test
    @Throws(Exception::class)
    fun testProtocol() {

        val event = Event(UUID(23333, 233123))
        val event1 = Event(UUID(44444, 324645))
        val event2 = Event(UUID(847392, 848123))
        val event3 = Event(UUID(45775, 233123))
        val event4 = Event(UUID(9823498, 3409834))
        val event5 = Event(UUID(439495775, 34034))
        val event6 = Event(UUID(543234, 23794))
        val event7 = Event(UUID(63567, 874))
        val event8 = Event(UUID(23457, 34470734))
        val event9 = Event(UUID(562, 57))
        val event10 = Event(UUID(23456, 340735534))

        app.broadcast(event)
        app1.broadcast(event1)
        app.broadcast(event2)
        app2.broadcast(event3)
        app1.broadcast(event4)
        app2.broadcast(event5)
        app3.broadcast(event6)
        app4.broadcast(event7)
        app5.broadcast(event8)
        app4.broadcast(event9)
        app3.broadcast(event10)

        var retry = 0
        while (retry != 10 && (app.events.size != 11
                || app1.events.size != 11
                || app2.events.size != 11)) {
            Thread.sleep(6000)
            retry++
        }
        Assert.assertTrue("Not all events delivered", app.events.size == 11
                && app1.events.size == 11
                && app2.events.size == 11)
        Assert.assertArrayEquals(app.events.toTypedArray(), app1.events.toTypedArray())
        Assert.assertArrayEquals(app1.events.toTypedArray(), app2.events.toTypedArray())

    }
}
