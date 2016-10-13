package epto

/**
 * Implementation of the stability oracle. This class implements the logical clock.
 *
 * @author Jocelyn Thode
 */
class StabilityOracle(val TTL: Int) {
    internal var logicalClock: Int = 0

    /**
     * This function tells us if the event is ready to be delivered or not according to the TTL.

     * @param event the event to be checked
     * *
     * @return wether the event is deliverable or not
     */
    fun isDeliverable(event: Event): Boolean {
        return event.ttl > TTL
    }

    /**
     * Increment and then return the clock.

     * @return the incremented clock
     */
    @Synchronized fun incrementAndGetClock(): Int {
        logicalClock++
        return logicalClock
    }

    /**
     * Update the clock with the new timestamp to synchronize it.

     * @param ts the new clock value
     */
    @Synchronized fun updateClock(ts: Int) {
        if (ts > logicalClock)
            logicalClock = ts
    }
}
