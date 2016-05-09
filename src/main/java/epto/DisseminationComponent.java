package epto;

import epto.utilities.Event;
import net.sf.neem.MulticastChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 */
public class DisseminationComponent {

    private static final Object nextBallLock = new Object(); //for synchronization of nextBall
    //private ArrayList<Peer> view = new ArrayList<>(); //TODO for now don't use it
    public final int K; //for 20 processes
    private final ScheduledExecutorService scheduler;
    private final StabilityOracle oracle;
    private final Peer peer;
    private HashMap<UUID, Event> nextBall;

    private final Runnable periodicDissemination;
    private ScheduledFuture<?> periodicDisseminationFuture;


    /**
     * Creates a new instance of DisseminationComponent
     *
     * @param oracle            StabilityOracle for the clock
     * @param peer              parent Peer
     * @param neem              MultiCastChannel to gossip
     * @param orderingComponent OrderingComponent to order events
     */
    public DisseminationComponent(StabilityOracle oracle, Peer peer, MulticastChannel neem,
                                  OrderingComponent orderingComponent, int K) {
        this.peer = peer;
        this.oracle = oracle;
        this.nextBall = new HashMap<>();
        this.K = K;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.periodicDissemination = () -> {
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
            synchronized (nextBallLock) {
                orderingComponent.orderEvents(nextBall);
                nextBall.clear();
            }
        };
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
     * Starts the periodic dissemination
     */
    public void start() {
        periodicDisseminationFuture = scheduler.scheduleWithFixedDelay(periodicDissemination, 0, Peer.DELTA, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the periodic dissemination
     */
    public void stop() {
        //Don't interrupt if running
        periodicDisseminationFuture.cancel(false);
    }
}
