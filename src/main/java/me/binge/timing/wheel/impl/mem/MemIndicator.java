package me.binge.timing.wheel.impl.mem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.entry.Entry;

public class MemIndicator<E extends Entry> implements Indicator<E> {

    private Map<E, Slot<E>> entrySlots = new ConcurrentHashMap<E, Slot<E>>();

    @Override
    public void put(E e, Slot<E> slot) {
        entrySlots.put(e, slot);
    }

    @Override
    public Slot<E> get(E e) {
        return entrySlots.get(e);
    }

    @Override
    public void remove(E e) {
        entrySlots.remove(e);
    }

}
