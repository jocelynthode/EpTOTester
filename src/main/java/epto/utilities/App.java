package epto.utilities;


import epto.Peer;
import net.sf.neem.MulticastChannel;
import net.sf.neem.apps.Addresses;
import net.sf.neem.impl.Application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Implementation of an Application
 */
public class App implements Application {

    private Peer peer;
    private MulticastChannel neem;

    public App(MulticastChannel neem, int TTL, int K) {
        this.neem = neem;
        this.peer = new Peer(neem, this, TTL, K);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: apps.App local peer1 ... peerN");
            System.exit(1);
        }

        try {

            MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true));

            System.out.println("Started: " + neem.getLocalSocketAddress());

            if (neem.getLocalSocketAddress().getAddress().isLoopbackAddress())
                System.out.println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.");


            App app = new App(neem, 100, 18);
            System.out.format("Peer ID : %s%n", app.peer.getUuid().toString());

            for (int i = 1; i < args.length; i++)
                neem.connect(Addresses.parse(args[i], false));

            app.start();
            app.broadcast(null);
            while (true) {
                Thread.sleep(1000);
            }
            //neem.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void deliver(ByteBuffer[] byteBuffers) {
        for (ByteBuffer byteBuffer : byteBuffers) {
            byte[] content = byteBuffer.array();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
            try {
                ObjectInputStream in = new ObjectInputStream(byteIn);
                Event event = (Event) in.readObject();
                System.out.println("Delivered : " + event.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Peer getPeer() {
        return peer;
    }

    public void start() {
        new Thread(peer).start();
    }

    public void broadcast(Event event) throws InterruptedException {
        Thread.sleep(1000);
        if (event == null) event = new Event(UUID.randomUUID(), 0, 0, null);
        peer.getDisseminationComponent().broadcast(event);
    }

}
