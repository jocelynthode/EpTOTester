package epto;

import epto.utilities.Event;

/**
 * Implementation of the stability oracle. This class implements the logical clock.
 */
public class StabilityOracle {

    public final int TTL;
    long logicalClock = 0;

    public StabilityOracle(int TTL) {
        this.TTL = TTL;
    }

    /**
     * This function tells us if the event is ready to be delivered or not according to the TTL.
     *
     * @param event the event to be checked
     * @return wether the event is deliverable or not
     */
    public boolean isDeliverable(Event event) {
        return event.getTtl() > TTL;
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
     * @param ts the new clock value
     */
    public synchronized void updateClock(long ts) {
        if (ts > logicalClock)
            logicalClock = ts;
    }
}
