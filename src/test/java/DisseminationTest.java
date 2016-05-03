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
 * Created by jocelyn on 03.05.16.
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

        InetSocketAddress address8000 = new InetSocketAddress("localhost", 8000);
        InetSocketAddress address8001 = new InetSocketAddress("localhost", 8001);
        InetSocketAddress address8002 = new InetSocketAddress("localhost", 8002);


        neem = new MulticastChannel(address8000);
        neem1 = new MulticastChannel(address8001);
        neem2 = new MulticastChannel(address8001);

        neem.connect(address8001);
        neem.connect(address8002);
        neem1.connect(address8000);
        neem1.connect(address8002);
        neem2.connect(address8000);
        neem2.connect(address8001);

        app = new TestApp(neem, 5, 1);
        app1 = new TestApp(neem1, 5, 1);
        app2 = new TestApp(neem2, 5, 1);

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

        while (true) {Thread.sleep(1000);}


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
