package mocks;

import epto.utilities.Event;
import net.sf.neem.impl.Application;

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
public class MockApplication implements Application {

    public List<UUID> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void deliver(ByteBuffer[] byteBuffers) {
        for (ByteBuffer byteBuffer : byteBuffers) {
            byte[] content = byteBuffer.array();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
            try {
                ObjectInputStream in = new ObjectInputStream(byteIn);
                Event event = (Event) in.readObject();
                events.add(event.getId());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

