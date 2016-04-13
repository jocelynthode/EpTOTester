package epto.utilities;

/**
 * Created by jocelyn on 12.04.16.
 */
public class Event {
    private int id;
    private long timeStamp;
    private int ttl;
    private int sourceId;

    public Event(int id, long timeStamp, int ttl, int sourceId) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.ttl = ttl;
        this.sourceId = sourceId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getId() {
        return id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public void incrementTtl() {
        this.ttl++;
    }
}
