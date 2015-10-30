package me.binge.timing.wheel.impl.zookeeper;

import org.I0Itec.zkclient.ZkClient;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.Wheel;
import me.binge.timing.wheel.entry.Entry;

public class ZookeeperIndicator<E extends Entry> implements Indicator<E> {

    public ZookeeperIndicator(ZkClient zkClient, Wheel<E> wheel) {
    }

    @Override
    public void put(E e, Slot<E> slot) {
        // TODO Auto-generated method stub

    }

    @Override
    public Slot<E> get(E e) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remove(E e) {
        // TODO Auto-generated method stub

    }

}
