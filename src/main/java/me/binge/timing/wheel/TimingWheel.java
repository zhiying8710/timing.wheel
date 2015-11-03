package me.binge.timing.wheel;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import me.binge.timing.wheel.Wheel.SlotGenerator;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.expire.ExpirationWorker;
import me.binge.timing.wheel.tick.TickCondition;
import me.binge.timing.wheel.utils.ShutdownHookUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A timing-wheel optimized for approximated I/O timeout scheduling.<br>
 * {@link TimingWheel} creates a new thread whenever it is instantiated and started, so don't create many instances.
 * <p>
 * <b>The classic usage as follows:</b><br>
 * <li>using timing-wheel manage any object timeout</li>
 * <pre>
 *    // Create a timing-wheel with 60 ticks, and every tick is 1 second.
 *    private static final TimingWheel<CometChannel> TIMING_WHEEL = new TimingWheel<CometChannel>(1, 60, TimeUnit.SECONDS);
 *
 *    // Add expiration listener and start the timing-wheel.
 *    static {
 *    	TIMING_WHEEL.addExpirationListener(new YourExpirationListener());
 *    	TIMING_WHEEL.start();
 *    }
 *
 *    // Add one element to be timeout approximated after 60 seconds
 *    TIMING_WHEEL.add(e);
 *
 *    // Anytime you can cancel count down timer for element e like this
 *    TIMING_WHEEL.remove(e);
 * </pre>
 *
 * After expiration occurs, the {@link Expiration} interface will be invoked and the expired object will be
 * the argument for callback method {@link Expiration#expired(Object)}
 * <p>
 * {@link TimingWheel} is based on <a href="http://cseweb.ucsd.edu/users/varghese/">George Varghese</a> and Tony Lauck's paper,
 * <a href="http://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">'Hashed and Hierarchical Timing Wheels: data structures
 * to efficiently implement a timer facility'</a>.  More comprehensive slides are located <a href="http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt">here</a>.
 *
 * @author mindwind
 * @version 1.0, Sep 20, 2012
 */
public abstract class TimingWheel<E extends Entry> {


    private final static Log log = LogFactory.getLog(TimingWheel.class);

    private final static ExecutorService EXPIRATION_EXECUTOR = Executors.newCachedThreadPool();

    protected final long tickDuration;
    protected final int ticksPerWheel;

    protected final Expiration<E>[] expirations;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Thread workerThread;

    private String wheelName;

    protected Wheel<E> wheel;

    private TickCondition tickCondition;

    private volatile boolean running = false;

    // ~ -------------------------------------------------------------------------------------------------------------

    public long getTickDuration() {
        return tickDuration;
    }

    public abstract Indicator<E> getIndicator();

    public abstract Slot<E> workSlot(long cycle, int id);

    /**
     * Construct a timing wheel.
     *
     * @param tickDuration
     *            tick duration with specified time unit.
     * @param ticksPerWheel
     * @param timeUnit
     * @param wheelName a name for this wheel instance
     */
    @SafeVarargs
    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, String wheelName, TickCondition tickCondition, Expiration<E>... expirations) {
        if (timeUnit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        this.tickDuration = TimeUnit.MILLISECONDS.convert(tickDuration, timeUnit);
        this.ticksPerWheel = ticksPerWheel + 1;

        this.expirations = expirations;
        this.tickCondition = tickCondition;
        if (wheelName == null) {
            wheelName = "Timing-Wheel";
        }

        this.wheelName = wheelName;
        this.wheel = new Wheel<E>(new SlotGenerator<E>() {

            @Override
            public Slot<E> gene(long cycle, int id) {
                return workSlot(cycle, id);
            }
        });
        workerThread = new Thread(new TickWorker(), this.wheelName);
        ShutdownHookUtils.hook(EXPIRATION_EXECUTOR, true);
    }

    @SafeVarargs
    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, TickCondition notifyExpireCondition, Expiration<E>... expirations) {
        this(tickDuration, ticksPerWheel, timeUnit, null, notifyExpireCondition, expirations);
    }

    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, TickCondition notifyExpireCondition) {
        this(tickDuration, ticksPerWheel, timeUnit, null, notifyExpireCondition);
    }

    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit) {
        this(tickDuration, ticksPerWheel, timeUnit, null);
    }


    // ~ -------------------------------------------------------------------------------------------------------------

    public void start() {
        if (shutdown.get()) {
            throw new IllegalStateException("Cannot be started once stopped");
        }

        for (int i = 0; i < this.ticksPerWheel; i++) {
            wheel.put(workSlot(getCurrentCycle(), i));
        }

        if (!workerThread.isAlive()) {
            workerThread.start();
            log.info(wheelName + " is running");
            running = true;
        }

    }

    public boolean stop() {
        if (!shutdown.compareAndSet(false, true)) {
            return false;
        }

        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        running = false;
        return true;
    }

    public boolean running() {
        return running;
    }

    /**
     * Add a element to {@link TimingWheel} and start to count down its life-time.
     *
     * @param e
     * @return remain time to be expired in millisecond.
     */
    public long add(E e) {
        synchronized(e) {
            checkAdd(e);
            Slot<E> slot = this.wheel.get(getCurrentCycle(), getPreviousTickIndex());
            e.init(slot.getCycle(), slot.getId());
            slot.add(e);
            getIndicator().put(e, slot);
            return (ticksPerWheel - 1) * tickDuration;
        }
    }

    private void checkAdd(E e) {
        Slot<E> slot = getIndicator().get(e);
        if (slot != null) {
            slot.remove(e);
        }
    }


    public boolean exist(E e) {
        return getIndicator().get(e) != null;
    }

    protected abstract int getCurrentTickIndex();
    protected abstract int setCurrentTickIndex(int currentTickIndex);
    protected abstract long getCurrentCycle();
    protected abstract void incrCurrentCycle();

    protected int getPreviousTickIndex() {
        lock.readLock().lock();
        try {
            int cti = getCurrentTickIndex();
            if (cti == 0) {
                return ticksPerWheel - 1;
            }

            return cti - 1;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes the specified element from timing wheel.
     *
     * @param e
     * @return <tt>true</tt> if this timing wheel contained the specified
     *         element
     */
    public boolean remove(E e) {
        synchronized (e) {
            Slot<E> slot = getIndicator().get(e);
            if (slot == null) {
                return false;
            }

            getIndicator().remove(e);
            return slot.remove(e) != null;
        }
    }

    public static class ElementExpireHandler<E extends Entry> implements Callable<E> {

        private Indicator<E> indicator;
        private Slot<E> slot;
        private E e;

        public ElementExpireHandler(Indicator<E> indicator, Slot<E> slot, E e) {
            this.indicator = indicator;
            this.slot = slot;
            this.e = e;
        }

        @Override
        public E call() throws Exception {
            slot.remove(e);
            synchronized (e) {
                Slot<E> latestSlot = indicator.get(e);
                if (latestSlot.equals(slot)) {
                    indicator.remove(e);
                }
            }
            return e;
        }

    }

    private void notifyExpired(long cycle, int idx) {
        for (long i = cycle; i > -1; i--) {
            Slot<E> slot = wheel.get(i, idx);
            if (slot == null) {
                break;
            }
            Set<E> elements = slot.elements();
            for (E e : elements) {
                EXPIRATION_EXECUTOR.submit(new ExpirationWorker<E>(new ElementExpireHandler<E>(getIndicator(), slot, e), e, expirations));
            }
            wheel.clear(i, idx);
        }

    }

    // ~ -------------------------------------------------------------------------------------------------------------

    protected class TickWorker implements Runnable {

        private long startTime;
        private long tick;

        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            tick = 1;
            for (; !shutdown.get();) {
                int currentTickIndex = getCurrentTickIndex();
                long currentCycle = getCurrentCycle();
                notifyExpired(currentCycle, currentTickIndex);
                if (tickCondition == null || tickCondition.tick()) {
                    try {
                        int newCurrentTickIdx = setCurrentTickIndex(currentTickIndex + 1);
                        if (newCurrentTickIdx == 0) {
                            incrCurrentCycle();
                        }
                    } catch (Exception e) { // if occur a exception, release tick condition and retry.
                        log.error("set current tick idx and incr current cycle error: " + e.getMessage(), e);
                        if (tickCondition != null) {
                            tickCondition.untick();
                        }
                        continue;
                    }
                }
                waitForNextTick();
            }
        }

        private void waitForNextTick() {
            for (;;) {
                long sleepTime = tickDuration * tick - (System.currentTimeMillis() - startTime);
                if (sleepTime <= 0) {
                    break;
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    return;
                }
            }
            tick++;
        }
    }
}
