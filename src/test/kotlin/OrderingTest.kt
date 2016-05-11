import epto.OrderingComponent
import epto.StabilityOracle
import epto.utilities.Event
import mocks.MockApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*

/**
 * Class testing the ordering component
 */
class OrderingTest {

    private lateinit var event: Event
    private lateinit var event1: Event
    private lateinit var event2: Event
    private lateinit var event3: Event
    private lateinit var event4: Event
    private lateinit var event5: Event
    private lateinit var event6: Event
    private lateinit var event7: Event
    private lateinit var event8: Event
    private lateinit var event9: Event

    private lateinit var app: MockApplication
    private lateinit var orderingComponent: OrderingComponent

    @Before
    @Throws(IOException::class)
    fun setUp() {

        val ttl = 50

        event = Event(UUID(23333, 233123), 1, 37, UUID(1111, 2222))
        event1 = Event(UUID(44444, 324645), 2, 49, UUID(4444, 2222))
        event2 = Event(UUID(847392, 848123), 1, 49, UUID(4444, 2222))
        event3 = Event(UUID(45775, 233123), 1, 49, UUID(2221, 2222))
        event4 = Event(UUID(9823498, 3409834), 1, 49, UUID(11, 32344))
        event5 = Event(UUID(439495775, 34034), 1, 49, UUID(22, 34048488))

        event6 = Event(UUID(423441, 340734), 4, 30, UUID(2223345, 3426356))
        event7 = Event(UUID(15546, 98734732), 2, 30, UUID(2223345, 34566998))
        event8 = Event(UUID(8384834, 34343), 5, 49, UUID(22, 34048488))
        event9 = Event(UUID(23333, 233123), 1, 49, UUID(1111, 2222))

        app = MockApplication()
        orderingComponent = OrderingComponent(StabilityOracle(ttl), app)
    }

    @Test
    fun testOrderEvents() {
        val map = object : HashMap<UUID, Event>() {
            init {
                put(event.id, event)
                put(event1.id, event1)
                put(event2.id, event2)
                put(event3.id, event3)
                put(event4.id, event4)
                put(event5.id, event5)
            }
        }

        val map1 = object : HashMap<UUID, Event>() {
            init {
                put(event9.id, event9)
                put(event6.id, event6)
                put(event7.id, event7)
                put(event8.id, event8)
            }
        }

        orderingComponent.orderEvents(map)
        orderingComponent.orderEvents(map1)
        orderingComponent.orderEvents(HashMap<UUID, Event>())
        orderingComponent.orderEvents(HashMap<UUID, Event>())

        Assert.assertTrue(app.events.size == 5)
        Assert.assertEquals(event4.id, app.events[0])
        Assert.assertEquals(event5.id, app.events[1])
        Assert.assertEquals(event9.id, app.events[2])
        Assert.assertEquals(event3.id, app.events[3])
        Assert.assertEquals(event2.id, app.events[4])
        //TODO fix later
        /*        orderingComponent.orderEvents(new HashMap<>());
        Assert.assertTrue(app.events.size() == 6);
        Assert.assertEquals(event9.getId(), app.events.get(4));
        Assert.assertEquals(event1.getId(), app.events.get(5));
        orderingComponent.orderEvents(new HashMap<>());
        Assert.assertTrue(app.events.size() == 6);*/

    }
}
