package epto;

import epto.utilities.App;
import epto.utilities.Event;
import net.sf.neem.MulticastChannel;
import net.sf.neem.impl.Application;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 */
public class Peer implements Runnable{

    public final static int DELTA = 5000;
    private StabilityOracle oracle;
    private OrderingComponent orderingComponent;
    private DisseminationComponent disseminationComponent;
    private final MulticastChannel neem;
    private final UUID uuid;

    /**
     * Initializes a peer
     *
     * @param neem MultiCast object
     */
    public Peer(MulticastChannel neem, Application app){
        this.neem = neem;
        this.oracle = new StabilityOracle();
        this.orderingComponent = new OrderingComponent(oracle, app);
        this.uuid = neem.getProtocolMBean().getLocalId();
        this.disseminationComponent = new DisseminationComponent(new Random(), neem.getNet(), oracle, this, neem, orderingComponent);
        neem.getProtocolMBean().setGossipFanout(DisseminationComponent.K);
    }

    public UUID getUuid() {
        return uuid;
    }

    public DisseminationComponent getDisseminationComponent() {
        return disseminationComponent;
    }

    @Override
    public void run() {
        disseminationComponent.start();
        try {
            //TODO recheck this part
            while (true) {
                byte[] buf = new byte[100000];
                ByteBuffer bb = ByteBuffer.wrap(buf);

                neem.read(bb);
                ByteArrayInputStream byteIn = new ByteArrayInputStream(bb.array());
                ObjectInputStream in = new ObjectInputStream(byteIn);
                disseminationComponent.receive((ConcurrentHashMap<UUID, Event>) in.readObject());
            }
        } catch (AsynchronousCloseException ace) {
            // Exiting.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
