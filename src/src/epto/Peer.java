package epto;

import epto.utilities.Event;
import net.sf.neem.MulticastChannel;
import net.sf.neem.apps.Addresses;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Implementation of a peer as described in EpTO. This class implements the structure of a peer.
 */
public class Peer implements Runnable{

    public final static int DELTA = 5;
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
    Peer(MulticastChannel neem){
        this.neem = neem;
        this.oracle = new StabilityOracle();
        this.orderingComponent = new OrderingComponent(oracle);
        this.uuid = neem.getProtocolMBean().getLocalId();
        this.disseminationComponent = new DisseminationComponent(new Random(), neem.getNet(), oracle, this, neem, orderingComponent);
        neem.getProtocolMBean().setGossipFanout(DisseminationComponent.K);
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void run() {
        disseminationComponent.start();
        try {
            //TODO recheck this oart
            while (true) {
                byte[] buf = new byte[1000];
                ByteBuffer bb = ByteBuffer.wrap(buf);

                neem.read(bb);
                ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray()); //TODO fix this
                ObjectInputStream in = new ObjectInputStream(byteIn);
                disseminationComponent.receive((HashMap<UUID, Event>) in.readObject());
            }
        } catch (AsynchronousCloseException ace) {
            // Exiting.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: apps.Chat local peer1 ... peerN");
            System.exit(1);
        }

        try {

            MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true));

            System.out.println("Started: " + neem.getLocalSocketAddress());

            if (neem.getLocalSocketAddress().getAddress().isLoopbackAddress())
                System.out.println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.");

            //Todo should use start ?
            Peer peer = new Peer(neem);
            new Thread(peer).start();

            for (int i = 1; i < args.length; i++)
                neem.connect(Addresses.parse(args[i], false));

            neem.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
