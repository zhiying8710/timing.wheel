package me.binge.timing.wheel;

import java.util.Set;

import me.binge.timing.wheel.entry.Entry;

public abstract class Slot<E extends Entry> {
    protected long cycle;
    protected int id;

    protected Slot(long cycle, int id) {
        this.cycle = cycle;
        this.id = id;
    }

    public abstract void add(E e);

    public abstract E remove(E e);

    public abstract Set<E> elements();

    public int getId() {
        return id;
    }

    public long getCycle() {
        return cycle;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (cycle ^ (cycle >>> 32));
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        Slot<E> other = (Slot<E>) obj;
        if (cycle != other.cycle)
            return false;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Slot [cycle=" + cycle + ", id=" + id + "]";
    }

}
