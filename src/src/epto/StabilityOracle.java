package epto;

import epto.utilities.Event;

/**
 * Implementation of the stability oracle. This class implements the logical clock.
 */
public class StabilityOracle {

    long logicalClock = 0;

    /**
     * This function tells us if the event is ready to be delivered or not according to the TTL.
     *
     * @param event
     * @return wether the event is deliverable or not
     */
    public boolean isDeliverable(Event event) {
        return event.getTtl() > DisseminationComponent.TTL;
    }

    /**
     * Increment and then return the clock.
     *
     * @return the incremented clock
     */
    public synchronized long incrementAndGetClock() {
        logicalClock++;
        return logicalClock;
    }

    /**
     * Update the clock with the new timestamp to synchronize it.
     *
     * @param ts
     */
    public synchronized void updateClock(long ts) {
        if (ts > logicalClock)
            logicalClock = ts;
    }
}
