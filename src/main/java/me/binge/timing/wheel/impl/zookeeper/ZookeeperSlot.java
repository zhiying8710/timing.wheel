package me.binge.timing.wheel.impl.zookeeper;

import java.util.Set;

import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.entry.Entry;

public class ZookeeperSlot<E extends Entry> extends Slot<E> {

    protected ZookeeperSlot(int id) {
        super(id);
    }

    @Override
    public void add(E e) {
        // TODO Auto-generated method stub

    }

    @Override
    public E remove(E e) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<E> elements() {
        // TODO Auto-generated method stub
        return null;
    }

}
