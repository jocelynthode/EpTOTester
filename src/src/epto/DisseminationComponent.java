package epto;

import epto.utilities.Event;
import net.sf.neem.MulticastChannel;
import net.sf.neem.impl.Overlay;
import net.sf.neem.impl.Periodic;
import net.sf.neem.impl.Transport;

import java.util.*;

/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 */
public class DisseminationComponent extends Periodic {

    private final MulticastChannel neem;
    private final OrderingComponent orderingComponent;
    private ArrayList<Peer> view = new ArrayList<>(); //todo use overlay to get all peers ?
    public final static int TTL = 3;
    public final static int K = 5;
    private HashMap<UUID, Event> nextBall = new HashMap<>();
    private final StabilityOracle  oracle;
    private final Peer peer;


    /**
     * Creates a new instance of DisseminationComponent
     *
     * @param rand The random number generator
     * @param trans The Transport object
     * @param oracle The Stability oracle
     * @param peer The peer that owns this component
     * @param overlay The overlay it is part of
     */
    public DisseminationComponent(Random rand, Transport trans, StabilityOracle oracle, Peer peer, MulticastChannel neem,
                                  OrderingComponent orderingComponent) {
        super(rand, trans, Peer.DELTA);
        this.peer = peer;
        this.oracle = oracle;
        this.neem = neem;
        this.orderingComponent = orderingComponent;
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
    public void receive(HashMap<UUID, Event> ball) { //TODO upon receive
        for (HashMap.Entry<UUID, Event> entry : ball.entrySet()) {
            UUID eventId = entry.getKey();
            Event event = entry.getValue();
            if (event.getTtl() < TTL) {
                if (nextBall.containsKey(eventId)) {
                    if (nextBall.get(eventId).getTtl() < event.getTtl()) {
                        nextBall.get(eventId).setTtl(event.getTtl());
                        // update TTL todo TTL or event.ttl ?
                    }
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
            //create peers + peers <- Random(view, K) We have a perfect view (getPeers ?) connect ?
            //peers.foreach
                //  send using neem.write(nextBall) TODO convert to ByteBuffer + Import MultiCastChannel instead of Overlay
        }
        orderingComponent.orderEvents(nextBall);
        nextBall.clear();
    }
}
