import epto.utilities.Event;
import mocks.MockApp;
import net.sf.neem.MulticastChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Class testing the whole protocol
 */
public class ProtocolTest {

    private MockApp app;
    private MockApp app1;
    private MockApp app2;
    private MulticastChannel neem;
    private MulticastChannel neem1;
    private MulticastChannel neem2;

    @Before
    public void setUp() throws Exception {

        final int ttl = 20;
        final int k = 10;

        InetSocketAddress address10000 = new InetSocketAddress("localhost", 10000);
        InetSocketAddress address10001 = new InetSocketAddress("localhost", 10001);
        InetSocketAddress address10002 = new InetSocketAddress("localhost", 10002);

        neem = new MulticastChannel(address10000);
        neem1 = new MulticastChannel(address10001);
        neem2 = new MulticastChannel(address10002);

        neem.connect(address10001);
        neem.connect(address10002);
        neem1.connect(address10000);
        neem1.connect(address10002);
        neem2.connect(address10000);
        neem2.connect(address10001);

        app = new MockApp(neem, ttl, k);
        app1 = new MockApp(neem1, ttl, k);
        app2 = new MockApp(neem2, ttl, k);

        app.start();
        app1.start();
        app2.start();

    }

    @After
    public void tearDown() throws Exception {
        neem.close();
        neem1.close();
        neem2.close();
    }

    //TODO for now compare if order is the same everywhere maybe make it so we can compare order too
    @Test
    public void testProtocol() throws Exception {
        Event event = new Event(new UUID(23333, 233123), 0, 0, null);
        Event event1 = new Event(new UUID(44444, 324645), 0, 0, null);
        Event event2 = new Event(new UUID(847392, 848123), 0, 0, null);
        Event event3 = new Event(new UUID(45775, 233123), 0, 0, null);
        Event event4 = new Event(new UUID(9823498, 3409834), 0, 49, null);
        Event event5 = new Event(new UUID(439495775, 34034), 0, 49, null);
        Event event6 = new Event(new UUID(423441, 340734), 0, 0, null);
        Event event7 = new Event(new UUID(15546, 98734732), 0, 0, null);
        Event event8 = new Event(new UUID(8384834, 34343l), 0, 0, null);

        app.broadcast(event);
        app1.broadcast(event1);
        app.broadcast(event2);
        app2.broadcast(event3);
        app1.broadcast(event4);
        app2.broadcast(event5);
        app1.broadcast(event6);
        app.broadcast(event7);
        app.broadcast(event8);

        int retry = 0;
        while ((retry != 6) && ((app.events.size() != 9)
                || (app1.events.size() != 9)
                || (app2.events.size() != 9))) {
            Thread.sleep(5000);
            retry++;
        }
        Assert.assertTrue(((app.events.size() == 9)
                && (app1.events.size() == 9)
                && (app2.events.size() == 9)));
        Assert.assertArrayEquals(app.events.toArray(), app1.events.toArray());
        Assert.assertArrayEquals(app1.events.toArray(), app2.events.toArray());

    }
}
