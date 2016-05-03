import epto.OrderingComponent;
import epto.StabilityOracle;
import epto.utilities.Event;
import net.sf.neem.impl.Application;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class testing the ordering component
 */
public class OrderingTest {

    private Event event;
    private Event event1;
    private Event event2;
    private Event event3;
    private Event event4;
    private Event event5;
    private Event event6;
    private Event event7;

    private TestApp app;
    private OrderingComponent orderingComponent;

    @Before
    public void setUp() throws IOException {
        event = new Event(new UUID(23333,233123),1,51,new UUID(1111,2222));
        event1 = new Event(new UUID(44444,324645),2,51,new UUID(4444,2222));
        event2 = new Event(new UUID(847392,848123),1,51,new UUID(4444,2222));
        event3 = new Event(new UUID(45775,233123),1,51,new UUID(2221,2222));
        event4 = new Event(new UUID(9823498,3409834),1,51,new UUID(11,32344));
        event5 = new Event(new UUID(439495775,34034),1,51,new UUID(22,34048488));
        event6 = new Event(new UUID(423441,340734),4,30,new UUID(2223345,34048488));
        event7 = new Event(new UUID(15546,98734732),2,30,new UUID(2223345,34048488));
        app = new TestApp();
        orderingComponent = new OrderingComponent(new StabilityOracle(50), app);
    }

    @Test
    public void testOrderEvents() {
        ConcurrentHashMap<UUID, Event> map = new ConcurrentHashMap<UUID, Event>(){{
            put(event.getId(), event);
            put(event1.getId(), event1);
            put(event2.getId(), event2);
            put(event3.getId(), event3);
            put(event4.getId(), event4);
            put(event5.getId(), event5);
            put(event6.getId(), event6);
        }};

        orderingComponent.orderEvents(map);

        Assert.assertTrue(app.events.size() == 6);
        Assert.assertEquals(event4.getId(), app.events.get(0));
        Assert.assertEquals(event5.getId(), app.events.get(1));
        Assert.assertEquals(event.getId(), app.events.get(2));
        Assert.assertEquals(event3.getId(), app.events.get(3));
        Assert.assertEquals(event2.getId(), app.events.get(4));
        Assert.assertEquals(event1.getId(), app.events.get(5));
        Assert.assertFalse(app.events.contains(event6.getId()));
        Assert.assertFalse(app.events.contains(event7.getId()));
    }

    private class TestApp implements Application {

        public ArrayList<UUID> events = new ArrayList<>();

        @Override
        public void deliver(ByteBuffer[] byteBuffers) {
            for (ByteBuffer byteBuffer : byteBuffers) {
                byte[] content = byteBuffer.array();
                ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
                try {
                    ObjectInputStream in = new ObjectInputStream(byteIn);
                    Event event = (Event) in.readObject();
                    events.add(event.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
