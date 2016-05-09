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
     * @param id The id of the event
     * @param timeStamp the time stamp of the event
     * @param ttl the time to live of the vent
     * @param sourceId the id of the peer sending this event
     */
    public Event(UUID id, long timeStamp, int ttl, UUID sourceId) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.ttl = ttl;
        this.sourceId = sourceId;
    }

    /**
     * Initializes an event
     */
    public Event() {
        this.id = UUID.randomUUID();
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
     * sets the source UUID
     *
     * @param sourceId
     */
    public synchronized void setSourceId(UUID sourceId) {
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
    public synchronized void setTimeStamp(long timeStamp) {
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
     * @param ttl the new ttl of th event
     */
    public synchronized void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
     * Increments the event's ttl
     */
    public synchronized void incrementTtl() {
        this.ttl++;
    }

    /**
     * compareTo method for Event class
     *
     * @param event a non-null Event
     * @return int (1 if before, -1 if after)
     */
    public int compareTo(Event event) {

        int compare = Long.compare(timeStamp, event.getTimeStamp());

        if (compare == 0) {
            //in case of tie
            return sourceId.compareTo(event.getSourceId());
        } else {
            return compare;
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        return id != null ? id.equals(event.id) : event.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
