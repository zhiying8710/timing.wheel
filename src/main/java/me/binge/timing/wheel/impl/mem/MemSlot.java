package me.binge.timing.wheel.impl.mem;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.entry.Entry;

public class MemSlot<E extends Entry> extends Slot<E> {

    private Map<E, E> elements = new ConcurrentHashMap<E, E>();

    protected MemSlot(int id) {
        super(id);
    }

    @Override
    public void add(E e) {
        elements.put(e, e);
    }

    @Override
    public E remove(E e) {
        return elements.remove(e);
    }

    @Override
    public Set<E> elements() {
        return elements.keySet();
    }

}
