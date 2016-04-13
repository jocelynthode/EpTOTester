package epto;

import net.sf.neem.impl.Connection;
import net.sf.neem.impl.Transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Created by jocelyn on 13.04.16.
 */
public class Peer extends Connection{

    StabilityOracle oracle = new StabilityOracle();
    OrderingComponent orderingComponent = new OrderingComponent();
    DisseminationComponent disseminationComponent = new DisseminationComponent();

    Peer(Transport trans, InetSocketAddress bind, InetSocketAddress remote) throws IOException {
        super(trans, bind, remote);
    }

    Peer(Transport trans, SocketChannel sock) throws IOException {
        super(trans, sock);
    }
}
