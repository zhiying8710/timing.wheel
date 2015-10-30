package me.binge.timing.wheel;

import me.binge.timing.wheel.entry.Entry;

public interface Indicator<E extends Entry> {

    public void put(E e, Slot<E> slot);

    public Slot<E> get(E e);

    public void remove(E e);

}
