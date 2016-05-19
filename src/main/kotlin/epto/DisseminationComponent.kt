package epto

import epto.utilities.Event
import net.sf.neem.MulticastChannel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 *
 * Creates a new instance of DisseminationComponent
 *
 * @param oracle            StabilityOracle for the clock
 * @param peer              parent Peer
 * @param neem              MultiCastChannel to gossip
 * @param orderingComponent OrderingComponent to order events
 */
class DisseminationComponent(private val oracle: StabilityOracle, private val peer: Peer, neem: MulticastChannel,
                             orderingComponent: OrderingComponent, val K: Int) {
    private val scheduler: ScheduledExecutorService
    private val periodicDissemination: Runnable
    private val nextBall = HashMap<UUID, Event>()
    private var periodicDisseminationFuture: ScheduledFuture<*>? = null


    init {
        this.scheduler = Executors.newScheduledThreadPool(1)
        this.periodicDissemination = Runnable {
            nextBall.forEach { id, event -> event.incrementTtl() }
            if (!nextBall.isEmpty()) {
                //TODO for now write assuming entire membership
                val byteOut = ByteArrayOutputStream()
                try {
                    val out = ObjectOutputStream(byteOut)
                    out.writeObject(nextBall)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                try {
                    neem.write(ByteBuffer.wrap(byteOut.toByteArray()))
                } catch (e: ClosedChannelException) {
                    e.printStackTrace()
                }
            }
            synchronized (nextBallLock) {
                orderingComponent.orderEvents(nextBall)
                nextBall.clear()
            }
        }
    }

    /**
     * Add the event to nextBall

     * @param event The new event
     */
    fun broadcast(event: Event) {
        event.timeStamp = oracle.incrementAndGetClock()
        event.ttl = 0
        event.sourceId = peer.uuid
        nextBall.put(event.id, event)
    }

    /**
     * Updates nextBall events with the new ball events only if the TTL is smaller and finally updates
     * the clock.

     * @param ball The received ball
     */
    internal fun receive(ball: HashMap<UUID, Event>) {
        for ((eventId, event) in ball) {
            if (event.ttl < oracle.TTL) {
                synchronized (nextBallLock, fun(): Unit {
                    val nextBallEvent = nextBall[eventId]
                    if (nextBallEvent != null) {
                        if (nextBallEvent.ttl < event.ttl) {
                            nextBallEvent.ttl = event.ttl
                        }
                    } else {
                        nextBall.put(eventId, event)
                    }
                })
            }
            oracle.updateClock(event.timeStamp) //only needed with logical time
        }
    }

    /**
     * Starts the periodic dissemination
     */
    fun start() {
        periodicDisseminationFuture = scheduler.scheduleWithFixedDelay(periodicDissemination, 0,
                Peer.DELTA, TimeUnit.MILLISECONDS)
    }

    /**
     * Stops the periodic dissemination
     */
    fun stop() {
        //Don't interrupt if running
        periodicDisseminationFuture?.cancel(false)
    }

    companion object {

        private val nextBallLock = Object() //for synchronization of nextBall
    }
}
