package me.binge.timing.wheel.impl.zookeeper;

import java.util.concurrent.TimeUnit;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.tick.TickCondition;

import org.I0Itec.zkclient.ZkClient;

public class ZookeeperTimingWheel<E extends Entry> extends TimingWheel<E> {

    private ZkClient zkClient;
    private Indicator<E> indicator;

    @SafeVarargs
    public ZookeeperTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName,
            TickCondition tickCondition, ZkClient zkClient, Expiration<E>... expirations) {

        super(tickDuration, ticksPerWheel, timeUnit, wheelName,
                tickCondition, expirations);

        this.zkClient = zkClient;
        this.indicator = new ZookeeperIndicator<E>(zkClient, this.wheel);
    }


    @Override
    public Indicator<E> getIndicator() {
        return this.indicator;
    }


    @Override
    public Slot<E> workSlot(long cycle, int id) {
        return new ZookeeperSlot<E>(cycle, id, zkClient);
    }


    @Override
    protected long getCurrentCycle() {
        return 0;
    }


    @Override
    protected void incrCurrentCycle() {

    }


    @Override
    protected int getCurrentTickIndex() {
        return 0;
    }


    @Override
    protected int setCurrentTickIndex(int currentTickIndex) {
        return 0;
    }


}
