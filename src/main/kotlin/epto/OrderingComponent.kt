package epto


import epto.libs.Utilities.logger
import java.util.*

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 *
 * @property oracle the stability oracle
 * @property application the application
 * @property received an HashMap of received but not yet delivered events
 * @property delivered an HashMap of delivered events
 * @property lastDeliveredTs Minimum timestamp for an event to be deliverable
 *
 * @see StabilityOracle
 * @see Application
 *
 * @author Jocelyn Thode
 */
class OrderingComponent(private val oracle: StabilityOracle, internal var application: Application) {

    private val logger by logger()

    internal val received = HashMap<UUID, Event>()
    private val delivered = HashMap<UUID, Event>()
    private var lastDeliveredTs: Int = 0

    /**
     * Update the received hash map TTL values and either add the new events to received or
     * update their ttl
     *
     * @param ball the received ball
     */
    private fun updateReceived(ball: HashMap<UUID, Event>) {
        // update TTL of received events
        received.values.forEach(Event::incrementTtl)

        // update set of received events with events in the ball
        ball.values.filter { event -> !delivered.containsKey(event.id) && event.timestamp >= lastDeliveredTs }
                .forEach { event ->
                    val receivedEvent = received[event.id]
                    if (receivedEvent != null) {
                        if (receivedEvent.ttl < event.ttl) {
                            receivedEvent.ttl = event.ttl
                        }
                    } else {
                        received.put(event.id, event)
                    }
                }
        logger.debug("Received size: {}", received.size)
        logger.debug("Min TTL: {}, Max TTL: {}",
                received.values.minBy { it.ttl }?.ttl,
                received.values.maxBy { it.ttl }?.ttl)
    }

    /**
     * Deliver events mature enough that haven't been yet delivered to the application
     *
     * @param deliverableEvents events mature enough to be delivered
     */
    private fun deliver(deliverableEvents: List<Event>) {
        for (event in deliverableEvents) {
            delivered.put(event.id, event)
            lastDeliveredTs = event.timestamp
            application.deliver(event)
        }
    }


    /**
     * this is the main function, OrderEvents procedure. Dissemination component will invoke this method periodically.
     *
     * @param ball the ball containing the received events
     */
    fun orderEvents(ball: HashMap<UUID, Event>) {

        updateReceived(ball)

        // collect deliverable events and determine smallest
        // timestamp of non deliverable events
        var minQueuedTs = Int.MAX_VALUE
        val deliverableEvents = ArrayList<Event>()

        for (event in received.values) {
            if (oracle.isDeliverable(event)) {
                deliverableEvents.add(event)
            } else if (minQueuedTs > event.timestamp) {
                minQueuedTs = event.timestamp
            }
        }

        val eventsToRemove = ArrayList<Event>()
        logger.debug("Deliverable events: {}", deliverableEvents.size)
        logger.debug("minQueuedTs: {}", minQueuedTs)
        for (event in deliverableEvents) {
            if (event.timestamp >= minQueuedTs) {
                // ignore deliverable events with timestamp greater or equal than all non-deliverable events
                eventsToRemove.add(event)
            } else {
                // event can be delivered, remove from received events
                received.remove(event.id)
            }
        }
        deliverableEvents.removeAll(eventsToRemove)
        logger.debug("Final Deliverable events: {}", deliverableEvents.size)

        //sort deliverable Events by Ts and ID, ascending
        deliverableEvents.sort(null)

        deliver(deliverableEvents)
    }
}
