package epto

import epto.libs.Utilities.logger
import epto.udp.Gossip
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.Executors
import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Funnel
import java.net.InetSocketAddress
import java.util.ArrayList
import com.google.common.hash.PrimitiveSink



/**
 * Implementation of the Dissemination Component  of EpTO. This class is in charge of
 * sending and collecting events to/from other peers.
 *
 * Creates a new instance of DisseminationComponent
 *
 * @property oracle            StabilityOracle for the clock
 * @property peer              the Peer
 * @property gossip    the gossip
 * @property orderingComponent OrderingComponent to order events
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
class DisseminationComponent(private val oracle: StabilityOracle, private val peer: Peer, private val gossip: Gossip,
                             private val orderingComponent: OrderingComponent, val K: Int, val delta: Long) {

    private val logger by logger()

    private val scheduler: ScheduledExecutorService
    private val nextBall = HashMap<String, Event>()
    private val periodicDissemination: Runnable
    private var periodicDisseminationFuture: ScheduledFuture<*>? = null
    private val myExecutor = Executors.newCachedThreadPool()
    private val MAX_KEYS = 300
    private val FPP = Math.pow(10.0, -3.0) //TODO use the c from EpTO
    private object EventFunnel : Funnel<Event> {
        override fun funnel(from: Event, into: PrimitiveSink) {
            //We only use the identifier
            into.putLong(from.sourceId!!.mostSignificantBits)
                    .putLong(from.sourceId!!.leastSignificantBits)
                    .putInt(from.timestamp)
        }
    }

    private val nonPushedEvents = HashMap<String, Event>()


    init {
        this.scheduler = Executors.newScheduledThreadPool(1)
        this.periodicDissemination = Runnable {
            try {
                logger.debug("nextBall size: {}", nextBall.size)
                synchronized(nextBall) {
                    nextBall.values.forEach(Event::incrementTtl)
                    if (!nextBall.isEmpty()) {
                        val events = nextBall.values.toList()
                        gossip.sendGossip(events)
                    }
                    orderingComponent.orderEvents()
                    nextBall.clear()
                    synchronized(nonPushedEvents) {
                        nonPushedEvents.clear()
                    }
                    val timestamp = orderingComponent.lastDeliveredTs
                    //TODO Maybe do earlier in a thread
                    val filter = CuckooFilter.Builder(EventFunnel, MAX_KEYS).withFalsePositiveRate(FPP).build()
                    orderingComponent.delivered.values.filter { it.timestamp == timestamp }
                            .forEach {filter.put(it)}
                    orderingComponent.received.values.forEach {filter.put(it)}

                    gossip.sendPullRequest(timestamp, filter)
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
     * @param ball The received ball
     */
    internal fun receiveGossip(ball: HashMap<String, Event>) {
        myExecutor.execute {
            logger.debug("Receiving a new ball of size: {}", ball.size)
            logger.debug("Ball will sendGossip {} events", ball.filter { it.value.ttl.get() < oracle.TTL }.size)
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

    /**
     * Find out which event the sender needs with the filter and send them
     *
     * @param timestamp the filter timestamp
     * @param filter the probabilistic filter
     * @param address the sender address
     */
    fun  receivePullRequest(timestamp: Int, filter: CuckooFilter<Event>, address: InetSocketAddress) {
        //TODO need to be more efficient
        val allKnownEvents = ArrayList<Event>()
        allKnownEvents.addAll(orderingComponent.received.values)
        allKnownEvents.addAll(orderingComponent.delivered.values)

        val eventsToSend = allKnownEvents.filter { it.timestamp >= timestamp && !filter.mightContain(it) }
        gossip.sendPullReply(eventsToSend as ArrayList<Event>, address)
    }

    /**
     * Pass the received events to the ordering component
     *
     * @param ball The received ball
     */
    fun  receivePullReply(ball: HashMap<String, Event>) {
        orderingComponent.receiveEvents(ball)
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
