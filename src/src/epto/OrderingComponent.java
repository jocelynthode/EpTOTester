package epto;

import epto.utilities.Event;
import epto.utilities.App;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 *
 */
public class OrderingComponent {

    private HashMap<UUID, Event> received; //TODO change it to queue of known ?
    private HashMap<UUID, Event> delivered; //TODO change it to queue of known ?
    private StabilityOracle oracle;
    App app;
    private long lastDeliveredTs;

    /**
     * Initialize order component.
     */
    public OrderingComponent(StabilityOracle oracle, App app){
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
    public void orderEvents(HashMap<UUID, Event> ball) {
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
        HashMap<UUID, Event> deliverableEvents = new HashMap<>();

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
        //sort deliverablesEvents by Ts and ID, descending
        deliverableEvents.entrySet()
                .stream()
                .sorted(HashMap.Entry.<UUID, Event>comparingByValue().reversed());

        for (UUID key : deliverableEvents.keySet()){
            Event event = received.get(key);
            if(!delivered.containsKey(key))
                delivered.put(key, event);
            lastDeliveredTs = event.getTimeStamp();
            //TODO it should deliver the msg inside the event, not the event itself. Right?
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try {
                ObjectOutputStream out = new ObjectOutputStream(byteOut);
                out.writeObject(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //delivering the event
            ByteBuffer[] byteoutArray = new ByteBuffer[1];
            byteoutArray[0] = ByteBuffer.wrap(byteOut.toByteArray());
            app.deliver(byteoutArray);

        }

    }
}
