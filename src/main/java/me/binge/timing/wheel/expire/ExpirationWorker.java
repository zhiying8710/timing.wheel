package me.binge.timing.wheel.expire;

import me.binge.timing.wheel.entry.Entry;

public class ExpirationWorker<E extends Entry> implements Runnable {

    private Expiration<E> expiration;
    private E e;

    public ExpirationWorker(Expiration<E> expiration, E entry) {
        this.expiration = expiration;
        this.e = entry;
    }

    public void run() {
        if (expiration != null) {
            expiration.expired(e);
        }
    }



}
