import epto.DisseminationComponent;
import epto.OrderingComponent;
import epto.StabilityOracle;
import epto.utilities.App;
import epto.utilities.Event;
import net.sf.neem.MulticastChannel;
import net.sf.neem.apps.Addresses;
import net.sf.neem.impl.Application;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.plaf.multi.MultiInternalFrameUI;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class testing the dissemination component
 */
public class DisseminationTest {

    private TestApp app;
    private TestApp app1;
    private TestApp app2;
    private MulticastChannel neem;
    private MulticastChannel neem1;
    private MulticastChannel neem2;

    @Before
    public void setUp() throws IOException {

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

        app = new TestApp(neem, 5, 2);
        app1 = new TestApp(neem1, 5, 2);
        app2 = new TestApp(neem2, 5, 2);

        app.start();
        app1.start();
        app2.start();

    }

    @After
    public void tearDown() {
        neem.close();
        neem1.close();
        neem2.close();
    }


    //TODO maybe waiting a time is not good enough
    @Test
    public void testDissemination() throws Exception {

        app.broadcast(new Event(new UUID(11911,22292),0,0,null));
        app1.broadcast(new Event(new UUID(112511,255222),0,0,null));
        app1.broadcast(new Event(new UUID(11151,225345222),0,0,null));
        app.broadcast(new Event(new UUID(1152311,22522),0,0,null));
        app2.broadcast(new Event(new UUID(19111,225422),0,0,null));
        app1.broadcast(new Event(new UUID(115511,22292),0,0,null));
        app.broadcast(new Event(new UUID(11234511,2222),0,0,null));
        app2.broadcast(new Event(new UUID(11,22252),0,0,null));

        Thread.sleep(10000);

        Assert.assertTrue(app.events.size() == 8);
        Assert.assertTrue(app1.events.size() == 8);
        Assert.assertTrue(app2.events.size() == 8);

    }


    private class TestApp extends App {

        public ArrayList<Event> events = new ArrayList<>();

        public TestApp(MulticastChannel neem, int TTL, int K) {
            super(neem, TTL, K);
        }

        @Override
        public void deliver(ByteBuffer[] byteBuffers) {
            for (ByteBuffer byteBuffer : byteBuffers) {
                byte[] content = byteBuffer.array();
                ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
                try {
                    ObjectInputStream in = new ObjectInputStream(byteIn);
                    Event event = (Event) in.readObject();
                    events.add(event);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
