package epto

import epto.libs.Utilities.logger
import epto.udp.Gossip
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.Executors



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
                             private val orderingComponent: OrderingComponent, val K: Int, val delta: Long) {

    private val logger by logger()

    private val scheduler: ScheduledExecutorService
    private val nextBall = HashMap<String, Event>()
    private val periodicDissemination: Runnable
    private var periodicDisseminationFuture: ScheduledFuture<*>? = null
    private val myExecutor = Executors.newCachedThreadPool()

    private val nonPushedEvents = ConcurrentHashMap<String, Event>()


    init {
        this.scheduler = Executors.newScheduledThreadPool(1)
        this.periodicDissemination = Runnable {
            try {
                logger.debug("nextBall size: {}", nextBall.size)
                synchronized(nextBall) {
                    nextBall.values.forEach(Event::incrementTtl)
                    if (!nextBall.isEmpty()) {
                        val events = nextBall.values.toList()
                        gossip.relay(events)
                    }
                    orderingComponent.orderEvents(nextBall)
                    val timestamp = orderingComponent.lastDeliveredTs
                    //TODO val filter = new Filter()
                    orderingComponent.delivered.forEach { s, event ->
                        if (event.timestamp == timestamp) {
                            //TODO filter.add(event)
                        }
                    }
                    //TODO filter.addAll(orderingComponent.received)
                    //TODO gossip.sendPull(timestamp, filter)
                    //TODO receive here ?
                    //TODO Receive on a third port or same?
                    //TODO where to receive
                    nextBall.clear()
                    synchronized(nonPushedEvents) {
                        nonPushedEvents.clear()
                    }
                }
            } catch (e: Exception) {
                logger.error(e.message, e)
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
        synchronized(nextBall) {
            nextBall.put(event.toIdentifier(), event)
        }
    }

    /**
     * Updates nextBall events with the new ball events only if the TTL is smaller and finally updates
     * the clock.
     *
     * This method is non-blocking
     *
     * @param ball The received ball
     */
    internal fun receive(ball: HashMap<String, Event>) {
        myExecutor.execute {
            logger.debug("Receiving a new ball of size: {}", ball.size)
            logger.debug("Ball will relay {} events", ball.filter { it.value.ttl.get() < oracle.TTL }.size)
            //TODO refactor to be in synchronized block probably
            orderingComponent.receiveEvents(ball)
            ball.forEach { eventIdentifier, event ->
                if (isPush(event)) {
                    synchronized(nextBall, fun(): Unit {
                        val nextBallEvent = nextBall[eventIdentifier]
                        if (nextBallEvent != null) {
                            if (nextBallEvent.ttl.get() < event.ttl.get()) {
                                nextBallEvent.ttl.set(event.ttl.get())
                            }
                        } else {
                            nextBall.put(eventIdentifier, event)
                        }
                    })
                } else {
                    debug(event)
                    //TODO even though nonPushed is multi thread I don't think we need to synchronize
                    //Nothing bad could happen I think
                    synchronized(nextBall) {
                        nextBall.remove(eventIdentifier)
                    }
                    synchronized(nonPushedEvents) {
                        nonPushedEvents.put(eventIdentifier, event)
                    }
                }
                oracle.updateClock(event.timestamp) //only needed with logical time
            }
        }
    }

    private fun isPush(event: Event): Boolean {
        synchronized(nonPushedEvents) {
            return event.timestamp  < oracle.TTL && !nonPushedEvents.contains(event.toIdentifier())
        }
    }

    private fun debug(event: Event) {
        if (logger.isDebugEnabled) {
            if (!peer.orderingComponent.received.containsKey(event.toIdentifier()) &&
                    !peer.orderingComponent.delivered.containsKey(event.toIdentifier())) {
                logger.debug("Event received too late: {}, TTL: {}", event.toIdentifier(), event.ttl.get())
            }
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
