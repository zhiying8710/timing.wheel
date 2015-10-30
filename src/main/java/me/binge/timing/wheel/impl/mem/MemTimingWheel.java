package me.binge.timing.wheel.impl.mem;

import java.util.concurrent.TimeUnit;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.tick.TickCondition;

public class MemTimingWheel<E extends Entry> extends TimingWheel<E> {

    private MemIndicator<E> indicator;

    private volatile int currentTickIndex = 0;

    public MemTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName, Expiration<E> expiration,
            TickCondition notifyExpireCondition) {

        super(tickDuration, ticksPerWheel, timeUnit, wheelName, expiration,
                notifyExpireCondition);

        this.indicator = new MemIndicator<E>();
    }

    @Override
    public Indicator<E> getIndicator() {
        return indicator;
    }

    @Override
    public Slot<E> workSlot(int id) {
        return new MemSlot<E>(id);
    }

    @Override
    protected int getCurrentTickIndex() {
        return currentTickIndex;
    }

    @Override
    protected void setCurrentTickIndex(int currentTickIndex) {
        this.currentTickIndex = currentTickIndex;
    }

}
