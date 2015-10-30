package me.binge.timing.wheel;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    protected final Expiration<E> expiration;

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

    public abstract Slot<E> workSlot(int id);

    /**
     * Construct a timing wheel.
     *
     * @param tickDuration
     *            tick duration with specified time unit.
     * @param ticksPerWheel
     * @param timeUnit
     * @param wheelName a name for this wheel instance
     */
    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, String wheelName, Expiration<E> expiration, TickCondition tickCondition) {
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

        this.expiration = expiration;
        this.tickCondition = tickCondition;
        if (wheelName == null) {
            wheelName = "Timing-Wheel";
        }

        this.wheelName = wheelName;
        this.wheel = new Wheel<E>();

        workerThread = new Thread(new TickWorker(), this.wheelName);
        ShutdownHookUtils.hook(EXPIRATION_EXECUTOR, true);
    }

    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, Expiration<E> expiration, TickCondition notifyExpireCondition) {
        this(tickDuration, ticksPerWheel, timeUnit, null, expiration, notifyExpireCondition);
    }

    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit, TickCondition notifyExpireCondition) {
        this(tickDuration, ticksPerWheel, timeUnit, null, null, notifyExpireCondition);
    }

    public TimingWheel(int tickDuration, int ticksPerWheel, TimeUnit timeUnit) {
        this(tickDuration, ticksPerWheel, timeUnit, null, null, null);
    }


    // ~ -------------------------------------------------------------------------------------------------------------

    public void start() {
        if (shutdown.get()) {
            throw new IllegalStateException("Cannot be started once stopped");
        }

        for (int i = 0; i < this.ticksPerWheel; i++) {
            wheel.put(workSlot(i));
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
     * @param a appoint listener, when e is expired, a thread will be start, appointExpirationListener.expired(e) will be invoked.
     * @return remain time to be expired in millisecond.
     */
    public long add(E e) {
        synchronized(e) {
            checkAdd(e);
            int previousTickIndex = getPreviousTickIndex();
            Slot<E> slot = this.wheel.get(previousTickIndex);
            e.setTime(System.currentTimeMillis());
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
    protected abstract void setCurrentTickIndex(int currentTickIndex);

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

    private void notifyExpired(int idx) {
        Slot<E> slot = this.wheel.get(idx);
        Set<E> elements = slot.elements();
        for (E e : elements) {
            slot.remove(e);
            synchronized (e) {
                Slot<E> latestSlot = getIndicator().get(e);
                if (latestSlot.equals(slot)) {
                    getIndicator().remove(e);
                }
            }
            if (expiration != null) {
                EXPIRATION_EXECUTOR.submit(new ExpirationWorker<E>(expiration, e));
            }
        }
    }

    // ~ -------------------------------------------------------------------------------------------------------------

    protected class TickWorker implements Runnable {

        private long startTime;
        private long tick;

        @Override
        public void run() {
            if (tickCondition != null) {
                tickCondition.tick();
            }
            startTime = System.currentTimeMillis();
            tick = 1;

            for (int i = 0; !shutdown.get(); i++) {
                if (i == wheel.size()) {
                    i = 0;
                }
                lock.writeLock().lock();
                int currentTickIndex = 0;
                try {
                    currentTickIndex  = i;
                    setCurrentTickIndex(currentTickIndex);
                } finally {
                    lock.writeLock().unlock();
                }
                notifyExpired(currentTickIndex);
                waitForNextTick();
            }
        }

        private void waitForNextTick() {
            for (;;) {
                long currentTime = System.currentTimeMillis();
                long sleepTime = tickDuration * tick - (currentTime - startTime);

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
