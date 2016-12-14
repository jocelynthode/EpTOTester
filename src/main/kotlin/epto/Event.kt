package epto

import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import java.io.IOException
import java.io.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of the events. This class implements the structure of an event as described in EpTO.
 *
 * @constructor initializes an event with a random UUID
 * @property id the id of the event
 * @property timestamp the timestamp of the event
 * @property ttl the number of rounds the event has been in the system
 * @property sourceId the id of the peer that broadcasted this event
 */
data class Event(val id: UUID = UUID.randomUUID()) : Comparable<Event>, Serializable {

    var timestamp: Int = 0
    var ttl: AtomicInteger = AtomicInteger(0)
    var sourceId: UUID? = null

    /**
     * Initializes an event
     *
     * @param id        The id of the event
     * @param timeStamp the time stamp of the event
     * @param ttl       the time to live of the vent
     * @param sourceId  the id of the peer sending this event
     */
    constructor(id: UUID, timeStamp: Int, ttl: Int, sourceId: UUID) : this(id) {
        this.timestamp = timeStamp
        this.ttl.set(ttl)
        this.sourceId = sourceId
    }

    /**
     * Increments the event's ttl
     */
    fun incrementTtl() {
        this.ttl.incrementAndGet()
    }

    /**
     * compareTo method for Event class
     *
     * @param other a non-null Event
     *
     * @return int (1 if before, -1 if after)
     */
    override fun compareTo(other: Event): Int {
        val compare = timestamp.compareTo(other.timestamp)

        return if (compare == 0) {
            //in case of tie
            sourceId?.compareTo(other.sourceId) ?: 0
        } else {
            compare
        }
    }

    override fun toString(): String {
        return "Event{id=$id, timestamp=$timestamp, ttl=${ttl.get()}, sourceId=$sourceId}"
    }

    internal fun toIdentifier(): String {
        return "[$sourceId, $timestamp]"
    }

    fun serialize(out: FSTObjectOutput) {
        out.writeLong(this.id.mostSignificantBits)
        out.writeLong(this.id.leastSignificantBits)
        out.writeInt(this.timestamp)
        out.writeInt(this.ttl.get())
        out.writeLong(this.sourceId!!.mostSignificantBits)
        out.writeLong(this.sourceId!!.leastSignificantBits)
    }

    companion object {
        /**
         * Unserialize an event from an input stream
         *
         * @throws EventUnserializeException the event could not be serialized
         *
         * @param inputStream the input stream from which to unserialize
         *
         * @return the unserialized event
         */
        fun unserialize(inputStream: FSTObjectInput): Event {
            try {
                val id = UUID(inputStream.readLong(), inputStream.readLong())
                val timeStamp = inputStream.readInt()
                val ttl = inputStream.readInt()
                val sourceId = UUID(inputStream.readLong(), inputStream.readLong())
                return Event(id, timeStamp, ttl, sourceId)
            } catch (e: IOException) {
                throw EventUnserializeException("Error unserializing the event values")
            }
        }
    }

    class EventUnserializeException(s: String) : Throwable(s) {}
}
