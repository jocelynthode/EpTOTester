package mocks;

import epto.utilities.App;
import epto.utilities.Event;
import net.sf.neem.MulticastChannel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by jocelyn on 05.05.16.
 */
public class MockApp extends App {

    public List<UUID> events = Collections.synchronizedList(new ArrayList<>());

    public MockApp(MulticastChannel neem, int TTL, int K) {
        super(neem, TTL, K);
    }

    @Override
    public void broadcast(Event event) throws InterruptedException {
        Thread.sleep(200);
        if (event == null) event = new Event(UUID.randomUUID(), 0, 0, null);
        this.getPeer().getDisseminationComponent().broadcast(event);
    }

    @Override
    public void deliver(ByteBuffer[] byteBuffers) {
        for (ByteBuffer byteBuffer : byteBuffers) {
            byte[] content = byteBuffer.array();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
            try {
                ObjectInputStream in = new ObjectInputStream(byteIn);
                Event event = (Event) in.readObject();
                events.add(event.getId());
                System.out.println(event.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

