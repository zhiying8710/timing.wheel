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

    public ZookeeperTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName, Expiration<E> expiration,
            TickCondition tickCondition, ZkClient zkClient) {
        super(tickDuration, ticksPerWheel, timeUnit, wheelName, expiration,
                tickCondition);

        this.zkClient = zkClient;
        this.indicator = new ZookeeperIndicator<E>(zkClient, this.wheel);
    }


    @Override
    public Indicator<E> getIndicator() {
        return this.indicator;
    }

    @Override
    public Slot<E> workSlot(int id) {
        return new ZookeeperSlot<E>(id);
    }

    @Override
    protected int getCurrentTickIndex() {
        return 0;
    }

    @Override
    protected void setCurrentTickIndex(int currentTickIndex) {

    }

}
