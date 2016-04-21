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
public class Peer extends Connection{

    public final static int DELTA = 5;
    private StabilityOracle oracle = new StabilityOracle();
    private OrderingComponent orderingComponent = new OrderingComponent(oracle);
    private DisseminationComponent disseminationComponent;
    private final MulticastChannel neem;


    /**
     * Initializes a peer
     * @param trans
     * @param bind
     * @param remote
     * @throws IOException
     */
    Peer(Transport trans, InetSocketAddress bind, InetSocketAddress remote, MulticastChannel neem) throws IOException {
        super(trans, bind, remote);
        this.neem = neem;
        new DisseminationComponent(new Random(), trans, oracle, this, neem, orderingComponent);
    }

    /**
     * Initializes a peer
     *
     * @param trans
     * @param sock
     * @throws IOException
     */
    Peer(Transport trans, SocketChannel sock, MulticastChannel neem) throws IOException {
        super(trans, sock);
        this.neem = neem;
        new DisseminationComponent(new Random(), trans, oracle, this, neem, orderingComponent);
    }
}
