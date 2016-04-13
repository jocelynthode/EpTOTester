package epto;

import epto.utilities.Event;
import net.sf.neem.impl.Periodic;
import net.sf.neem.impl.Transport;

import java.util.*;

/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 */
public class DisseminationComponent extends Periodic {

    private ArrayList<Peer> view = new ArrayList<>(); //todo use overlay to get all peers ?
    private static int TTL = 3;
    private int K = 5;
    private Map<UUID, Event> nextBall = new HashMap<>();
    private StabilityOracle  oracle;
    private Peer peer;


    /**
     * Creates a new instance of DisseminationComponent
     *
     * @param rand The random number generator
     * @param trans The Transport object
     * @param interval The interval to run the periodic function
     * @param oracle The Stability oracle
     * @param peer The peer that owns this component
     */
    public DisseminationComponent(Random rand, Transport trans, int interval, StabilityOracle oracle, Peer peer) {
        super(rand, trans, interval);
        this.peer = peer;
        this.oracle = oracle;
    }

    /**
     * Add the event to nextBall
     *
     * @param event The new event
     */
    public void broadcast(Event event) {
        event.setTimeStamp(oracle.incrementAndGetClock());
        event.setTtl(0);
        event.setSourceId(peer.id);
        nextBall.put(event.getId(), event);
    }

    /**
     * Updates nextBall events with the new ball events only if the TTL is smaller and finally updates
     * the clock.
     *
     * @param ball The received ball
     */
    public void receive(Map<UUID, Event> ball) {
        for (Map.Entry<UUID, Event> entry : ball.entrySet()) {
            UUID eventId = entry.getKey();
            Event event = entry.getValue();
            if (event.getTtl() < TTL) {
                // TODO check if the condition is correct
                if (nextBall.containsKey(eventId) && nextBall.get(eventId).getTtl() < event.getTtl()) {
                    nextBall.get(eventId).setTtl(event.getTtl());
                    // update TTL todo why ?
                } else {
                    nextBall.put(eventId, event);
                }
            }
            oracle.updateClock(event.getTimeStamp()); //only needed with logical time
        }
     }

    /**
     * Periodic functions that sends nextBall to K random peers
     */
    @Override
    public void run() {
        nextBall.forEach((id, event) -> event.incrementTtl());
        if (!nextBall.isEmpty()) {
            //create peers + peers <- Random(view, K)
            //peers.foreach
                //  send using peer.send(nextBall, q.getPeer().getPort())
        }
        //OrderEvents(nextBall); todo static ?
        nextBall.clear();
    }

    /**
     * Returns the TTL constant
     * @return The TTL constant
     */
    public static int getTTL() {
        return TTL;
    }
}
