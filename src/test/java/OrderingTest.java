import epto.OrderingComponent;
import epto.StabilityOracle;
import epto.utilities.Event;
import mocks.MockApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

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
    private Event event8;
    private Event event9;

    private MockApplication app;
    private OrderingComponent orderingComponent;

    @Before
    public void setUp() throws IOException {

        final int ttl = 50;

        event = new Event(new UUID(23333, 233123), 1, 37, new UUID(1111, 2222));
        event1 = new Event(new UUID(44444, 324645), 2, 49, new UUID(4444, 2222));
        event2 = new Event(new UUID(847392, 848123), 1, 49, new UUID(4444, 2222));
        event3 = new Event(new UUID(45775, 233123), 1, 49, new UUID(2221, 2222));
        event4 = new Event(new UUID(9823498, 3409834), 1, 49, new UUID(11, 32344));
        event5 = new Event(new UUID(439495775, 34034), 1, 49, new UUID(22, 34048488));

        event6 = new Event(new UUID(423441, 340734), 4, 30, new UUID(2223345, 3426356));
        event7 = new Event(new UUID(15546, 98734732), 2, 30, new UUID(2223345, 34566998));
        event8 = new Event(new UUID(8384834, 34343), 5, 49, new UUID(22, 34048488));
        event9 = new Event(new UUID(23333, 233123), 1, 49, new UUID(1111, 2222));

        app = new MockApplication();
        orderingComponent = new OrderingComponent(new StabilityOracle(ttl), app);
    }

    @Test
    public void testOrderEvents() {
        HashMap<UUID, Event> map = new HashMap<UUID, Event>() {{
            put(event.getId(), event);
            put(event1.getId(), event1);
            put(event2.getId(), event2);
            put(event3.getId(), event3);
            put(event4.getId(), event4);
            put(event5.getId(), event5);
        }};

        HashMap<UUID, Event> map1 = new HashMap<UUID, Event>() {{
            put(event9.getId(), event9);
            put(event6.getId(), event6);
            put(event7.getId(), event7);
            put(event8.getId(), event8);
        }};

        orderingComponent.orderEvents(map);
        orderingComponent.orderEvents(map1);
        orderingComponent.orderEvents(new HashMap<>());
        orderingComponent.orderEvents(new HashMap<>());

        Assert.assertTrue(app.events.size() == 5);
        Assert.assertEquals(event4.getId(), app.events.get(0));
        Assert.assertEquals(event5.getId(), app.events.get(1));
        Assert.assertEquals(event9.getId(), app.events.get(2));
        Assert.assertEquals(event3.getId(), app.events.get(3));
        Assert.assertEquals(event2.getId(), app.events.get(4));
//TODO fix later
/*        orderingComponent.orderEvents(new HashMap<>());
        Assert.assertTrue(app.events.size() == 6);
        Assert.assertEquals(event9.getId(), app.events.get(4));
        Assert.assertEquals(event1.getId(), app.events.get(5));
        orderingComponent.orderEvents(new HashMap<>());
        Assert.assertTrue(app.events.size() == 6);*/

    }
}
