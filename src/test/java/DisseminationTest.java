import epto.utilities.Event;
import mocks.MockApp;
import net.sf.neem.MulticastChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Class testing the dissemination component
 */
public class DisseminationTest {

    private MockApp app;
    private MockApp app1;
    private MockApp app2;
    private MockApp app3;
    private MockApp app4;
    private MockApp app5;
    private MulticastChannel neem;
    private MulticastChannel neem1;
    private MulticastChannel neem2;
    private MulticastChannel neem3;
    private MulticastChannel neem4;
    private MulticastChannel neem5;

    @Before
    public void setUp() throws IOException {

        final int ttl = 30;
        final int k = 4;

        InetSocketAddress address10000 = new InetSocketAddress("localhost", 10000);
        InetSocketAddress address10001 = new InetSocketAddress("localhost", 10001);
        InetSocketAddress address10002 = new InetSocketAddress("localhost", 10002);
        InetSocketAddress address10003 = new InetSocketAddress("localhost", 10003);
        InetSocketAddress address10004 = new InetSocketAddress("localhost", 10004);
        InetSocketAddress address10005 = new InetSocketAddress("localhost", 10005);

        InetSocketAddress[] addresses0 = new InetSocketAddress[] {address10001, address10002, address10003,
                address10004, address10005};
        InetSocketAddress[] addresses1 = new InetSocketAddress[] {address10000, address10002, address10003,
                address10004, address10005};
        InetSocketAddress[] addresses2 = new InetSocketAddress[] {address10000, address10001, address10003,
                address10004, address10005};
        InetSocketAddress[] addresses3 = new InetSocketAddress[] {address10000, address10001, address10002,
                address10004, address10005};
        InetSocketAddress[] addresses4 = new InetSocketAddress[] {address10000, address10001, address10002, address10003,
                address10005};
        InetSocketAddress[] addresses5 = new InetSocketAddress[] {address10000, address10001, address10002, address10003,
                address10004};

        neem = new MulticastChannel(address10000);
        neem1 = new MulticastChannel(address10001);
        neem2 = new MulticastChannel(address10002);
        neem3 = new MulticastChannel(address10003);
        neem4 = new MulticastChannel(address10004);
        neem5 = new MulticastChannel(address10005);

        for (InetSocketAddress address: addresses0) {
            neem.connect(address);
        }

        for (InetSocketAddress address: addresses1) {
            neem1.connect(address);
        }

        for (InetSocketAddress address: addresses2) {
            neem2.connect(address);
        }

        for (InetSocketAddress address: addresses3) {
            neem3.connect(address);
        }

        for (InetSocketAddress address: addresses4) {
            neem4.connect(address);
        }

        for (InetSocketAddress address: addresses5) {
            neem5.connect(address);
        }


        app = new MockApp(neem, ttl, k);
        app1 = new MockApp(neem1, ttl, k);
        app2 = new MockApp(neem2, ttl, k);
        app3 = new MockApp(neem3, ttl, k);
        app4 = new MockApp(neem4, ttl, k);
        app5 = new MockApp(neem5, ttl, k);

        app.start();
        app1.start();
        app2.start();
        app3.start();
        app4.start();
        app5.start();

    }


    //TODO maybe waiting a time is not good enough
    @Test
    public void testDissemination() throws Exception {

        Event event = new Event(new UUID(23333, 233123), 0, 0, null);
        Event event1 = new Event(new UUID(44444, 324645), 0, 0, null);
        Event event2 = new Event(new UUID(847392, 848123), 0, 0, null);
        Event event3 = new Event(new UUID(45775, 233123), 0, 0, null);
        Event event4 = new Event(new UUID(9823498, 3409834), 0, 0, null);
        Event event5 = new Event(new UUID(439495775, 34034), 0, 0, null);
        Event event6 = new Event(new UUID(543234, 23794), 0, 0, null);
        Event event7 = new Event(new UUID(63567, 874), 0, 0, null);
        Event event8 = new Event(new UUID(23457, 34470734), 0, 0, null);
        Event event9 = new Event(new UUID(562, 57), 0, 0, null);
        Event event10 = new Event(new UUID(23456, 340735534), 0, 0, null);

        app.broadcast(event);
        app1.broadcast(event1);
        app.broadcast(event2);
        app2.broadcast(event3);
        app1.broadcast(event4);
        app2.broadcast(event5);
        app3.broadcast(event6);
        app4.broadcast(event7);
        app5.broadcast(event8);
        app4.broadcast(event9);
        app3.broadcast(event10);

        Thread.sleep(10000);

        Assert.assertTrue(app.getPeer().getOrderingComponent().getReceived().size() == 11);
        Assert.assertTrue(app1.getPeer().getOrderingComponent().getReceived().size() == 11);
        Assert.assertTrue(app2.getPeer().getOrderingComponent().getReceived().size() == 11);
    }
}
