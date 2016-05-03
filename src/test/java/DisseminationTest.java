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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jocelyn on 03.05.16.
 */
public class DisseminationTest {

    private Event event;
    private Event event1;
    private Event event2;
    private Event event3;
    private Event event4;
    private Event event5;

    private TestApp app;
    private TestApp app1;
    private MulticastChannel neem;
    private MulticastChannel neem1;

    @Before
    public void setup() throws IOException {
        event = new Event(new UUID(23333,233123),1,51,new UUID(1111,2222));
        event1 = new Event(new UUID(44444,324645),2,51,new UUID(4444,2222));
        event2 = new Event(new UUID(847392,848123),1,51,new UUID(4444,2222));
        event3 = new Event(new UUID(45775,233123),1,51,new UUID(2221,2222));
        event4 = new Event(new UUID(9823498,3409834),1,51,new UUID(11,32344));
        event5 = new Event(new UUID(439495775,34034),1,51,new UUID(22,34048488));

        neem = new MulticastChannel(Addresses.parse("localhost:8000", true));
        neem1 = new MulticastChannel(Addresses.parse("localhost:8001", true));

        neem.connect(Addresses.parse("localhost:8001", false));
        neem1.connect(Addresses.parse("localhost:8000", false));

        app = new TestApp(neem);
        app1 = new TestApp(neem1);

        app.start();
        app1.start();
    }

    @After
    public void kill() {
        neem.close();
        neem1.close();
    }


    @Test
    public void testDissemination() throws InterruptedException {
        app.broadcast(new Event(new UUID(1111,2222),0,0,null));
        app1.broadcast(new Event(new UUID(1111,2222),0,0,null));
        app1.broadcast(new Event(new UUID(1111,2222),0,0,null));
        app.broadcast(new Event(new UUID(1111,2222),0,0,null));
        app1.broadcast(new Event(new UUID(1111,2222),0,0,null));
        app.broadcast(new Event(new UUID(1111,2222),0,0,null));

        System.out.println("test");

    }


    private class TestApp extends App {

        public ArrayList<Event> events = new ArrayList<>();

        public TestApp(MulticastChannel neem) {
            super(neem);
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
