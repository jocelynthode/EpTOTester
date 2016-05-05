import epto.utilities.Event;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Class testing the events methods
 */
public class EventTest {

    private Event event;
    private Event event1;
    private Event event2;
    private Event event3;

    @Before
    public void setup() throws IOException {
        event = new Event(new UUID(23333, 233123), 1, 5, new UUID(1111, 2222));
        event1 = new Event(new UUID(44444, 324645), 2, 5, new UUID(4444, 2222));
        event2 = new Event(new UUID(847392, 848123), 1, 5, new UUID(4444, 2222));
        event3 = new Event(new UUID(23333, 233123), 1, 5, new UUID(1111, 2222));
    }

    @Test
    public void testCompareTo() {
        Assert.assertTrue(event.compareTo(event3) == 0);
        Assert.assertTrue(event.compareTo(event1) == -1);
        Assert.assertTrue(event.compareTo(event2) == -1);
        Assert.assertTrue(event1.compareTo(event2) == 1);
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(event.equals(event3));
        Assert.assertTrue(event.equals(event));
        Assert.assertFalse(event.equals(event1));
    }
}
