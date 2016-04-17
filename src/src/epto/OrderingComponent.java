package epto;

import epto.utilities.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 *
 */
public class OrderingComponent {

    private Map<UUID, Event> received; //TODO change it to queue of known ?
    private Map<UUID, Event> delivered; //TODO change it to queue of known ?
    private StabilityOracle oracle;
    private long lastDeliveredTs;

    /**
     * Initialize order component.
     */
    public OrderingComponent(StabilityOracle oracle){
        received = new HashMap<>();
        delivered = new HashMap<>();
        this.oracle = oracle;
        lastDeliveredTs = 0;
    }

    /**
     * this is the main function, OrderEvents procedure. Dissemination component will invoke this method periodically.
     *
     * @param ball
     */
    public void OrderEvents(HashMap<UUID, Event> ball) {
        // update TTL of received events
        for (UUID key : received.keySet()){
            Event event = received.get(key);
            event.setTtl(event.getTtl()+1);
        }

        // update set of received events with events in the ball
        for (UUID key : ball.keySet()){
            Event event = received.get(key);
            if(!delivered.containsKey(key) && event.getTimeStamp() > lastDeliveredTs){
                if (received.containsKey(key)){
                    if(received.get(key).getTtl() < event.getTtl()){ //TODO check later
                        received.get(key).setTtl(event.getTtl());
                    }
                }
                else {
                    received.put(key, event);
                }
            }
        }
        // collect deliverable events and determine smallest
        // timestamp of non deliverable events

        long minQueuedTs  = Integer.MAX_VALUE;
        Map<UUID, Event> deliverableEvents = new HashMap<>();

        for (UUID key : received.keySet()){
            Event event = received.get(key);
            if (oracle.isDeliverable(event)){
                if(!deliverableEvents.containsKey(key))
                    deliverableEvents.put(key, event);
            }
            else if (minQueuedTs > event.getTimeStamp()){
                minQueuedTs = event.getTimeStamp();
            }
        }
        for (UUID key : deliverableEvents.keySet()){
            Event event = received.get(key);
            if (event.getTimeStamp() > minQueuedTs) {
                // ignore deliverable events with timestamp greater than all non-deliverable events
                deliverableEvents.remove(key); //TODO check later
            }
            else {
                // event can be delivered, remove from received events
                received.remove(key);
            }
        }
        //TODO sort deliverablesEvents by Ts and ID
        for (UUID key : deliverableEvents.keySet()){
            Event event = received.get(key);
            if(!delivered.containsKey(key))
                delivered.put(key, event);
            lastDeliveredTs = event.getTimeStamp();
            //TODO deliver??
            //Deliver(event);

        }

    }
}
