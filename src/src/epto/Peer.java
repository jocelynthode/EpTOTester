package epto;

import net.sf.neem.impl.Connection;
import net.sf.neem.impl.Transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * Created by jocelyn on 13.04.16.
 */
public class Peer extends Connection{

    private final int DELTA = 5;
    StabilityOracle oracle = new StabilityOracle();
    OrderingComponent orderingComponent = new OrderingComponent();
    DisseminationComponent disseminationComponent;

    Peer(Transport trans, InetSocketAddress bind, InetSocketAddress remote) throws IOException {
        super(trans, bind, remote);
        new DisseminationComponent(new Random(), trans, DELTA, oracle);

    }

    Peer(Transport trans, SocketChannel sock) throws IOException {
        super(trans, sock);
        new DisseminationComponent(new Random(), trans, DELTA, oracle);
    }
}
