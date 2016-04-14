package epto;

import net.sf.neem.impl.Connection;
import net.sf.neem.impl.Transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 */
public class Peer extends Connection{

    private final int DELTA = 5;
    private StabilityOracle oracle = new StabilityOracle();
    private OrderingComponent orderingComponent = new OrderingComponent();
    private DisseminationComponent disseminationComponent;

    /**
     * Initializes a peer
     * @param trans
     * @param bind
     * @param remote
     * @throws IOException
     */
    Peer(Transport trans, InetSocketAddress bind, InetSocketAddress remote) throws IOException {
        super(trans, bind, remote);
        new DisseminationComponent(new Random(), trans, DELTA, oracle, this);
    }

    /**
     * Initializes a peer
     *
     * @param trans
     * @param sock
     * @throws IOException
     */
    Peer(Transport trans, SocketChannel sock) throws IOException {
        super(trans, sock);
        new DisseminationComponent(new Random(), trans, DELTA, oracle, this);
    }
}
