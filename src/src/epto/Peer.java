package epto;

import net.sf.neem.MulticastChannel;
import net.sf.neem.impl.Connection;
import net.sf.neem.impl.Overlay;
import net.sf.neem.impl.Transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 */
public class Peer extends Thread{

    public final static int DELTA = 5;
    private StabilityOracle oracle = new StabilityOracle();
    private OrderingComponent orderingComponent = new OrderingComponent(oracle);
    private DisseminationComponent disseminationComponent;
    private final MulticastChannel neem;

    /**
     * Initializes a peer
     * @param trans
     */
    Peer(Transport trans, MulticastChannel neem){
        this.neem = neem;
        this.disseminationComponent = new DisseminationComponent(new Random(), trans, oracle, this, neem, orderingComponent);
    }

    @Override
    public void run() {
        //TODO maybe dissemination and ordering should be thread instead of this
    }
}
