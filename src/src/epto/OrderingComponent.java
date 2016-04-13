package epto;

import epto.utilities.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jocelyn on 12.04.16.
 */
public class OrderingComponent {

    private Map<Integer, Event> received;
    private Map<Integer, Event> delivered;
    private StabilityOracle so;
    private long lastDeliveredTs;

    public OrderingComponent(){
        received = new HashMap<>();
        delivered = new HashMap<>();
        so = new StabilityOracle();
        lastDeliveredTs = 0;
    }

    public void run(HashMap<Integer, Event> ball){
        // update TTL of received events
        for (Integer key : received.keySet()){
            Event event = received.get(key);
            event.setTtl(event.getTtl()+1);
        }

        // update set of received events with events in the ball
        for (Integer key : ball.keySet()){
            Event event = received.get(key);
            if(!delivered.containsKey(key) && event.getTimeStamp() > lastDeliveredTs){
                if (received.containsKey(key)){
                    if(received.get(key).getTtl() < event.getTtl()){ //TODO check later
                        received.get(key).setTtl(event.getTtl());
                    }
                }
                else {
                    received.put(event.getId(), event);
                }
            }
        }
        // collect deliverable events and determine smallest
        // timestamp of non deliverable events

        long minQueuedTs  = Integer.MAX_VALUE;
        Map<Integer, Event> deliverableEvents = new HashMap<>();

        for (Integer key : received.keySet()){
            Event event = received.get(key);
            if (so.isDeliverable(event)){
                if(!deliverableEvents.containsKey(key))
                    deliverableEvents.put(key, event);
            }
            else if (minQueuedTs > event.getTimeStamp()){
                minQueuedTs = event.getTimeStamp();
            }
        }
        for (Integer key : deliverableEvents.keySet()){
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
        //TODO sort deliverablesEventsby Ts and ID
        for (Integer key : deliverableEvents.keySet()){
            Event event = received.get(key);
            if(!delivered.containsKey(key))
                delivered.put(key, event);
            lastDeliveredTs = event.getTimeStamp();
            //TODO deliver??
            //Deliver(event);

        }

    }
}
