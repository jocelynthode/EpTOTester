package epto;

import epto.utilities.Event;
import net.sf.neem.impl.Periodic;
import net.sf.neem.impl.Transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by jocelyn on 12.04.16.
 */
public class DisseminationComponent extends Periodic {

    private ArrayList<Peer> view = new ArrayList<>(); //todo use overlay to get all peers ?
    private static int TTL = 3;
    private int K = 5;
    private Map<Integer, Event> nextBall = new HashMap<>();
    private StabilityOracle  oracle;

    public DisseminationComponent(Random rand, Transport trans, int interval, StabilityOracle oracle) {
        super(rand, trans, interval);
        this.oracle = oracle;
    }

    public void broadcast(Event event) {
        event.setTimeStamp(oracle.incrementAndGetClock());
        event.setTtl(0);
        //event.setSourceId() todo Peer Id
        nextBall.put(event.getId(), event);
    }

    @Override
    public void run() {
        nextBall.forEach((k, v) -> v.incrementTtl());
        if (!nextBall.isEmpty()) {
            //create peers + peers <- Random(view, K)
            //peers.foreach send todo peer == process ?
        }
        //OrderEvents(nextBall); todo static ?
        nextBall.clear();
    }

    public static int getTTL() {
        return TTL;
    }
}
