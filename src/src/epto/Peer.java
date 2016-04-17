package epto;

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
    private final Overlay overlay;


    /**
     * Initializes a peer
     * @param trans
     * @param bind
     * @param remote
     * @throws IOException
     */
    Peer(Transport trans, InetSocketAddress bind, InetSocketAddress remote, Overlay overlay) throws IOException {
        super(trans, bind, remote);
        this.overlay = overlay;
        new DisseminationComponent(new Random(), trans, oracle, this, overlay, orderingComponent);
    }

    /**
     * Initializes a peer
     *
     * @param trans
     * @param sock
     * @throws IOException
     */
    Peer(Transport trans, SocketChannel sock, Overlay overlay) throws IOException {
        super(trans, sock);
        this.overlay = overlay;
        new DisseminationComponent(new Random(), trans, oracle, this, overlay, orderingComponent);
    }
}
