package me.binge.timing.wheel.impl.mem;

import java.util.concurrent.TimeUnit;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;

public class MemTimingWheel<E extends Entry> extends TimingWheel<E> {

    private MemIndicator<E> indicator;

    private volatile int currentTickIndex = 0;

    private volatile int currentCycle = 0;

    @SafeVarargs
    public MemTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName, Expiration<E>... expirations) {

        super(tickDuration, ticksPerWheel, timeUnit, wheelName, null, expirations);

        this.indicator = new MemIndicator<E>();
    }

    @Override
    public Indicator<E> getIndicator() {
        return indicator;
    }

    @Override
    public Slot<E> workSlot(long cycle, int id) {
        return new MemSlot<E>(cycle, id);
    }

    @Override
    protected int getCurrentTickIndex() {
        return currentTickIndex;
    }

    @Override
    protected int setCurrentTickIndex(int currentTickIndex) {
        if (currentTickIndex == this.ticksPerWheel) {
            currentTickIndex = 0;
        }
        this.currentTickIndex = currentTickIndex;
        return this.currentTickIndex;
    }

    @Override
    protected long getCurrentCycle() {
        return currentCycle;
    }

    @Override
    protected void incrCurrentCycle() {
        currentCycle ++;
    }

}
