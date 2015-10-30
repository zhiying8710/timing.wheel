package me.binge.timing.wheel.expire;

import me.binge.timing.wheel.entry.Entry;

public abstract class Expiration<E extends Entry> {

    /**
     * Invoking when a expired event occurs.
     *
     * @param entry
     */
    public abstract void expired(E entry);

}
