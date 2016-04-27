package epto.utilities;

import java.io.Serializable;
import java.util.UUID;

/**
 * Implementation of the events. This class implements the structure of an event as described in EpTO.
 */
    public class Event implements Comparable<Event>, Serializable {
    private UUID id;
    private long timeStamp;
    private int ttl;
    private UUID sourceId;

    /**
     * Initializes an event
     *
     * @param id
     * @param timeStamp
     * @param ttl
     * @param sourceId
     */
    public Event(UUID id, long timeStamp, int ttl, UUID sourceId) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.ttl = ttl;
        this.sourceId = sourceId;
    }

    /**
     * gets the event's UUID
     *
     * @return the source UUID
     */
    public UUID getSourceId() {
        return sourceId;
    }

    /**
     *  sets the source UUID
     *
     * @param sourceId
     */
    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * gets the event's UUID
     *
     * @returns the event's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * gets the event's timestamp
     *
     * @return The event's timestamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * sets the timestamp to the specified value
     *
     * @param timeStamp
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * returns the event's ttl
     *
     * @return The event's ttl
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * sets the ttl to the specified value
     *
     * @param ttl
     */
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
     * Increments the event's ttl
     */
    public void incrementTtl() {
        this.ttl++;
    }

    /**
     * compareTo method for Event class
     *
     * @param event a not-null Event
     * @return int (1 if before, -1 if after)
     */
    public int compareTo(Event event) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.timeStamp > event.getTimeStamp()) return AFTER;
        if (this.timeStamp < event.getTimeStamp()) return BEFORE;

        //in case of tie
        if (this.sourceId.compareTo(event.getSourceId()) == 1) return AFTER;
        if (this.sourceId.compareTo(event.getSourceId()) == -1) return BEFORE;

        return EQUAL;

    }

    /**
     * @return A string representing the Event object
     */
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", ttl=" + ttl +
                ", sourceId=" + sourceId +
                '}';
    }
}
