package me.binge.timing.wheel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.binge.timing.wheel.entry.Entry;


public class Wheel<E extends Entry> {

    private Map<Integer, Slot<E>> slots = new ConcurrentHashMap<Integer, Slot<E>>();

    public void put(Slot<E> slot) {
        slots.put(slot.getId(), slot);
    }

    public int size() {
        return slots.size();
    }

    public Slot<E> get(int id) {
        return slots.get(id);
    }

}
