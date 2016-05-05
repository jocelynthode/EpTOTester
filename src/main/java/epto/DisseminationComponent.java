package epto;

import epto.utilities.Event;
import net.sf.neem.MulticastChannel;
import net.sf.neem.impl.Periodic;
import net.sf.neem.impl.Transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 */
public class DisseminationComponent extends Periodic {

    private static final Object nextBallLock = new Object(); //for synchronization of nextBall
    //private ArrayList<Peer> view = new ArrayList<>(); //TODO for now don't use it
    public final int K; //for 20 processes
    private final MulticastChannel neem;
    private final OrderingComponent orderingComponent;
    private final StabilityOracle oracle;
    private final Peer peer;
    private HashMap<UUID, Event> nextBall;


    /**
     * Creates a new instance of DisseminationComponent
     *
     * @param rand              Random instance used for periods
     * @param trans             Transport component to gossip
     * @param oracle            StabilityOracle for the clock
     * @param peer              parent Peer
     * @param neem              MultiCastChannel to gossip
     * @param orderingComponent OrderingComponent to order events
     */
    public DisseminationComponent(Random rand, Transport trans, StabilityOracle oracle, Peer peer, MulticastChannel neem,
                                  OrderingComponent orderingComponent, int K) {
        super(rand, trans, Peer.DELTA);
        this.peer = peer;
        this.oracle = oracle;
        this.neem = neem;
        this.orderingComponent = orderingComponent;
        this.nextBall = new HashMap<>();
        this.K = K;
    }

    /**
     * Add the event to nextBall
     *
     * @param event The new event
     */
    public void broadcast(Event event) {
        event.setTimeStamp(oracle.incrementAndGetClock());
        event.setTtl(0);
        event.setSourceId(peer.getUuid());
        nextBall.put(event.getId(), event);
    }

    /**
     * Updates nextBall events with the new ball events only if the TTL is smaller and finally updates
     * the clock.
     *
     * @param ball The received ball
     */
    void receive(HashMap<UUID, Event> ball) {
        for (HashMap.Entry<UUID, Event> entry : ball.entrySet()) {
            UUID eventId = entry.getKey();
            Event event = entry.getValue();
            if (event.getTtl() < oracle.TTL) {
                synchronized (nextBallLock) {
                    if (nextBall.containsKey(eventId)) {
                        if (nextBall.get(eventId).getTtl() < event.getTtl()) {
                            nextBall.get(eventId).setTtl(event.getTtl());
                        }
                    } else {
                        nextBall.put(eventId, event);
                    }
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
            //TODO for now write assuming entire membership
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try {
                ObjectOutputStream out = new ObjectOutputStream(byteOut);
                out.writeObject(nextBall);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                neem.write(ByteBuffer.wrap(byteOut.toByteArray()));
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
        orderingComponent.orderEvents(nextBall);
        synchronized (nextBallLock) {
            nextBall.clear();
        }
    }
}
