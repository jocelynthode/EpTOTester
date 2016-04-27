package epto.utilities;


import epto.Peer;
import net.sf.neem.MulticastChannel;
import net.sf.neem.apps.Addresses;
import net.sf.neem.impl.Application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * Implementation of an Application
 * TODO broadcast ?
 */
public class App implements Application {

    private Peer peer;
    private MulticastChannel neem;

    public App(MulticastChannel neem) {
        this.neem = neem;
        this.peer = new Peer(neem, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliver(ByteBuffer[] byteBuffers) {
        for (ByteBuffer byteBuffer : byteBuffers) {
            byte[] content = byteBuffer.array();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
            try {
                ObjectInputStream in = new ObjectInputStream(byteIn);
                Event event = (Event) in.readObject();
                System.out.println(event.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() {
        new Thread(peer).start();
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


            App app = new App(neem);

            for (int i = 1; i < args.length; i++)
                neem.connect(Addresses.parse(args[i], false));

            app.start();

            neem.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
