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
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jocelyn on 03.05.16.
 */
public class OrderingTest {

    private Event event;
    private Event event1;
    private Event event2;
    private Event event3;
    private TestApp app;
    private OrderingComponent orderingComponent;

    @Before
    public void setup() throws IOException {
        event = new Event(new UUID(23333,233123),1,5,new UUID(1111,2222));
        event1 = new Event(new UUID(44444,324645),2,5,new UUID(4444,2222));
        event2 = new Event(new UUID(847392,848123),3,5,new UUID(4444,2222));
        event3 = new Event(new UUID(45775,233123),1,5,new UUID(2221,2222));
        app = new TestApp();
        orderingComponent = new OrderingComponent(new StabilityOracle(), app);

    }

    @Test
    public void testOrderEvents() {
        ConcurrentHashMap<UUID, Event> map = new ConcurrentHashMap<UUID, Event>(){{
            put(event.getId(), event);
            put(event1.getId(), event1);
            put(event2.getId(), event2);
            put(event3.getId(), event3);
        }};

        orderingComponent.orderEvents(map);

        Assert.assertEquals(event, app.events.get(0));
        Assert.assertEquals(event3, app.events.get(1));
        Assert.assertEquals(event1, app.events.get(2));
        Assert.assertEquals(event2, app.events.get(3));
    }

    private class TestApp implements Application {

        public ArrayList<Event> events = new ArrayList<>();

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
