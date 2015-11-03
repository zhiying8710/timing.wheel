package me.binge.timing.wheel.impl.zookeeper;

import org.I0Itec.zkclient.ZkClient;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.Wheel;
import me.binge.timing.wheel.entry.Entry;

public class ZookeeperIndicator<E extends Entry> implements Indicator<E> {

    private ZkClient zkClient;
    private Wheel<E> wheel;

    public ZookeeperIndicator(ZkClient zkClient, Wheel<E> wheel) {
        this.zkClient = zkClient;
        this.wheel = wheel;
    }

    @Override
    public void put(E e, Slot<E> slot) {
    }

    @Override
    public Slot<E> get(E e) {
        return null;
    }

    @Override
    public void remove(E e) {

    }

}
