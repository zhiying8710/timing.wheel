package me.binge.timing.wheel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.binge.timing.wheel.entry.Entry;


public class Wheel<E extends Entry> {

    private Map<Long, Map<Integer, Slot<E>>> cycleSlots = new ConcurrentHashMap<Long, Map<Integer,Slot<E>>>();

    private SlotGenerator<E> slotGenerator;

    public Wheel(SlotGenerator<E> slotGenerator) {
        this.slotGenerator = slotGenerator;
    }

    public void put(Slot<E> slot) {
        Map<Integer, Slot<E>> slots = cycleSlots.get(slot.getCycle());
        if (slots == null) {
            slots = new ConcurrentHashMap<Integer, Slot<E>>();
        }
        slots.put(slot.getId(), slot);
        cycleSlots.put(slot.getCycle(), slots);
    }

    public int size(long cycle) {
        Map<Integer, Slot<E>> slots = cycleSlots.get(cycle);
        if (slots == null) {
            return 0;
        }
        return slots.size();
    }

    public Slot<E> get(long cycle, int id) {
        Map<Integer, Slot<E>> slots = cycleSlots.get(cycle);
        if (slots == null) {
            slots = new ConcurrentHashMap<Integer, Slot<E>>();
            cycleSlots.put(cycle, slots);
        }
        Slot<E> slot = slots.get(id);
        if (slot == null) {
            slot = slotGenerator.gene(cycle, id);
            slots.put(id, slot);
        }
        return slot;
    }

    public void clear(long cycle, int idx) {
        Map<Integer, Slot<E>> slots = cycleSlots.get(cycle);
        if (slots != null) {
            slots.remove(idx);
        }
    }

    public Collection<Slot<E>> slots(long cycle) {
        return cycleSlots.get(cycle).values();
    }

    public static interface SlotGenerator<E extends Entry> {

        public Slot<E> gene(long cycle, int id);

    }
}
