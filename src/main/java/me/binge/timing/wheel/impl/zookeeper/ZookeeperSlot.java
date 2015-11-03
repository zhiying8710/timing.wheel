package me.binge.timing.wheel.impl.zookeeper;

import java.util.Set;

import org.I0Itec.zkclient.ZkClient;

import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.entry.Entry;

public class ZookeeperSlot<E extends Entry> extends Slot<E> {

    private ZkClient zkClient;

    protected ZookeeperSlot(long cycle, int id, ZkClient zkClient) {
        super(cycle, id);
        this.zkClient = zkClient;
    }

    @Override
    public void add(E e) {

    }

    @Override
    public E remove(E e) {
        return null;
    }

    @Override
    public Set<E> elements() {
        return null;
    }

}
