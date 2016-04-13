package epto;

import epto.utilities.Event;

/**
 * Created by jocelyn on 12.04.16.
 */
public class StabilityOracle {

    int logicalClock = 0;

    public boolean isDeliverable(Event event) {
        return event.getTtl() > DisseminationComponent.getTTL();
    }

    public int incrementAndGetClock() {
        logicalClock++;
        return logicalClock;
    }

    public void updateClock(int ts) {
        if (ts > logicalClock)
            logicalClock = ts;
    }
}
