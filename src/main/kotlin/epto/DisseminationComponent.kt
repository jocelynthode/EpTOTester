package epto

import epto.libs.Utilities.logger
import epto.udp.Gossip
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
 * @property oracle            StabilityOracle for the clock
 * @property peer              the Peer
 * @param gossip    the gossip
 * @param orderingComponent OrderingComponent to order events
 * @property K the fanout used to send balls
 * @property delta the delay between each execution of EpTO
 *
 * @see StabilityOracle
 * @see Peer
 * @see Gossip
 * @see OrderingComponent
 *
 * @author Jocelyn Thode
 */
class DisseminationComponent(private val oracle: StabilityOracle, private val peer: Peer, gossip: Gossip,
                             orderingComponent: OrderingComponent, val K: Int, val delta: Long) {

    private val logger by logger()

    private val scheduler: ScheduledExecutorService
    private val nextBall = HashMap<UUID, Event>()
    private val nextBallLock = Any()
    private val periodicDissemination: Runnable
    private var periodicDisseminationFuture: ScheduledFuture<*>? = null


    init {
        this.scheduler = Executors.newScheduledThreadPool(1)
        this.periodicDissemination = Runnable {
            logger.debug("nextBall size: {}", nextBall.size)
            synchronized(nextBallLock) {
                nextBall.values.forEach(Event::incrementTtl)
                if (!nextBall.isEmpty()) {
                    val events = nextBall.values.toList()
                    try {
                        gossip.relay(events)
                    } catch (e: Gossip.ViewSizeException) {
                        logger.error(e)
                    }
                }
                orderingComponent.orderEvents(nextBall)
                nextBall.clear()
            }
        }
    }

    /**
     * Add the event to nextBall
     *
     * @param event The new event
     */
    fun broadcast(event: Event) {
        event.timestamp = oracle.incrementAndGetClock()
        event.sourceId = peer.uuid
        synchronized(nextBallLock) {
            nextBall.put(event.id, event)
        }
    }

    /**
     * Updates nextBall events with the new ball events only if the TTL is smaller and finally updates
     * the clock.
     *
     * @param ball The received ball
     */
    internal fun receive(ball: HashMap<UUID, Event>) {
        logger.debug("Receiving a new ball of size: {}", ball.size)
        logger.debug("Ball will relay {} events", ball.filter { it.value.ttl.get() < oracle.TTL }.size)
        ball.forEach { eventId, event ->
            if (event.ttl.get() < oracle.TTL) {
                synchronized(nextBallLock, fun(): Unit {
                    val nextBallEvent = nextBall[eventId]
                    if (nextBallEvent != null) {
                        if (nextBallEvent.ttl.get() < event.ttl.get()) {
                            nextBallEvent.ttl.set(event.ttl.get())
                        }
                    } else {
                        nextBall.put(eventId, event)
                    }
                })
            }
            oracle.updateClock(event.timestamp) //only needed with logical time
        }
    }

    /**
     * Starts the periodic dissemination
     */
    fun start() {
        periodicDisseminationFuture = scheduler.scheduleAtFixedRate(periodicDissemination,
                0, delta, TimeUnit.MILLISECONDS)
    }

    /**
     * Stops the periodic dissemination
     */
    fun stop() = periodicDisseminationFuture?.cancel(true)
}
