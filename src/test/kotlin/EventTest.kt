import epto.utilities.Event
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*

/**
 * Class testing the events methods
 */
class EventTest {

    private lateinit var event: Event
    private lateinit var event1: Event
    private lateinit var event2: Event
    private lateinit var event3: Event

    @Before
    @Throws(IOException::class)
    fun setup() {
        event = Event(UUID(23333, 233123), 1, 5, UUID(1111, 2222))
        event1 = Event(UUID(44444, 324645), 2, 5, UUID(4444, 2222))
        event2 = Event(UUID(847392, 848123), 1, 5, UUID(4444, 2222))
        event3 = Event(UUID(23333, 233123), 1, 5, UUID(1111, 2222))
    }

    @Test
    fun testCompareTo() {
        Assert.assertTrue(event.compareTo(event3) == 0)
        Assert.assertTrue(event.compareTo(event1) == -1)
        Assert.assertTrue(event.compareTo(event2) == -1)
        Assert.assertTrue(event1.compareTo(event2) == 1)
    }

    @Test
    fun testEquals() {
        Assert.assertTrue(event == event3)
        Assert.assertTrue(event == event)
        Assert.assertFalse(event == event1)
    }
}
