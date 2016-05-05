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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 *
 */
public class OrderingComponent {

    private HashMap<UUID, Event> received;
    private HashMap<UUID, Event> delivered;
    private StabilityOracle oracle;
    Application app;
    private long lastDeliveredTs;

    /**
     * Initialize order component.
     */
    public OrderingComponent(StabilityOracle oracle, Application app){
        received = new HashMap<>();
        delivered = new HashMap<>();
        this.oracle = oracle;
        this.app = app;
        lastDeliveredTs = 0;
    }

    /**
     * this is the main function, OrderEvents procedure. Dissemination component will invoke this method periodically.
     *
     * @param ball
     */
    public synchronized void orderEvents(HashMap<UUID, Event> ball) {
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
        // collect deliverable events and determine smallest
        // timestamp of non deliverable events

        long minQueuedTs  = Long.MAX_VALUE;
        List<Event> deliverableEvents = new ArrayList<>();

        for (Event event : received.values()){
            if (oracle.isDeliverable(event)){
                deliverableEvents.add(event);
            }
            else if (minQueuedTs > event.getTimeStamp()){
                minQueuedTs = event.getTimeStamp();
            }
        }

        List<Event> eventsToRemove = new ArrayList<>();

        for (Event event : deliverableEvents){
            if (event.getTimeStamp() > minQueuedTs) {
                //TODO test in unit tests
                // ignore deliverable events with timestamp greater than all non-deliverable events
                eventsToRemove.add(event);
            }
            else {
                // event can be delivered, remove from received events
                received.remove(event.getId());
            }
        }
        deliverableEvents.removeAll(eventsToRemove);
        //sort deliverables Events by Ts and ID, ascending
        deliverableEvents.sort(null);

        for (Event event : deliverableEvents){
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
}
