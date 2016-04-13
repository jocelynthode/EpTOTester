package epto;

import epto.utilities.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jocelyn on 12.04.16.
 */
public class OrderingComponent {

    private Map<Integer, Event> received = new HashMap<>();
    private Map<Integer, Event> delivered = new HashMap<>();
    private long lastDeliveredTs = 0;
}
