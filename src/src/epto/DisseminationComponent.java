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

    private ArrayList<Peer> view = new ArrayList<>();
    private static int TTL = 0;
    private int K = 0;
    private Map<Integer, Event> nextBall = new HashMap<>();
    private StabilityOracle  oracle;

    public DisseminationComponent(Random rand, Transport trans, int interval, StabilityOracle oracle) {
        super(rand, trans, interval);
        this.oracle = oracle;
    }

    public void broadcast(Event event) {
        event.setTimeStamp(oracle.incrementAndGetClock());
        event.setTtl(0);
        //event.setSourceId() todo id
        nextBall.put(event.getId(), event);
    }

    @Override
    public void run() {
        nextBall.forEach((k, v) -> v.incrementTtl());
        if (!nextBall.isEmpty()) {
            //create peers + peers <- Random(view, k)
            //peers.foreach
        }
    }

    public static int getTTL() {
        return TTL;
    }
}
