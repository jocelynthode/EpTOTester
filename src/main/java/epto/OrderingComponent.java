package epto;


import epto.utilities.Event;
import net.sf.neem.impl.Application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 */
public class OrderingComponent {

    Application app;
    private HashMap<UUID, Event> received;
    private HashMap<UUID, Event> delivered;
    private StabilityOracle oracle;
    private long lastDeliveredTs;

    /**
     * Initialize order component.
     */
    public OrderingComponent(StabilityOracle oracle, Application app) {
        received = new HashMap<>();
        delivered = new HashMap<>();
        this.oracle = oracle;
        this.app = app;
        lastDeliveredTs = 0;
    }

    /**
     * Update the received hash map TTL values and either add the new events to received or
     * update their ttl
     *
     * @param ball the received ball
     */
    private void updateReceived(HashMap<UUID, Event> ball) {
        // update TTL of received events
        received.values().forEach(Event::incrementTtl);

        // update set of received events with events in the ball
        ball.values().stream()
                .filter(event -> !delivered.containsKey(event.getId()) && event.getTimeStamp() >= lastDeliveredTs)
                .forEach(event -> {
                    if (received.containsKey(event.getId())) {
                        if (received.get(event.getId()).getTtl() < event.getTtl()) {
                            received.get(event.getId()).setTtl(event.getTtl());
                        }
                    } else {
                        received.put(event.getId(), event);
                    }
                });
    }

    /**
     * Deliver events mature enough that haven't been yet delivered to the application
     *
     * @param deliverableEvents events mature enough to be delivered
     */
    private void deliver(List<Event> deliverableEvents) {
        for (Event event : deliverableEvents) {
            delivered.put(event.getId(), event);

            lastDeliveredTs = event.getTimeStamp();

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try {
                ObjectOutputStream out = new ObjectOutputStream(byteOut);
                out.writeObject(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //delivering the event
            ByteBuffer[] byteOutArray = new ByteBuffer[1];
            byteOutArray[0] = ByteBuffer.wrap(byteOut.toByteArray());
            app.deliver(byteOutArray);
        }
    }


    /**
     * this is the main function, OrderEvents procedure. Dissemination component will invoke this method periodically.
     *
     * @param ball
     */
    public synchronized void orderEvents(HashMap<UUID, Event> ball) {

        updateReceived(ball);

        // collect deliverable events and determine smallest
        // timestamp of non deliverable events
        long minQueuedTs = Long.MAX_VALUE;
        List<Event> deliverableEvents = new ArrayList<>();

        for (Event event : received.values()) {
            if (oracle.isDeliverable(event)) {
                deliverableEvents.add(event);
            } else if (minQueuedTs > event.getTimeStamp()) {
                minQueuedTs = event.getTimeStamp();
            }
        }

        List<Event> eventsToRemove = new ArrayList<>();

        for (Event event : deliverableEvents) {
            if (event.getTimeStamp() > minQueuedTs) {
                // ignore deliverable events with timestamp greater than all non-deliverable events
                eventsToRemove.add(event);
            } else {
                // event can be delivered, remove from received events
                received.remove(event.getId());
            }
        }
        deliverableEvents.removeAll(eventsToRemove);

        //sort deliverable Events by Ts and ID, ascending
        deliverableEvents.sort(null);

        deliver(deliverableEvents);
    }

    public HashMap<UUID, Event> getReceived() {
        return received;
    }
}
