package me.binge.timing.wheel.expire;

import java.util.concurrent.Callable;

import me.binge.timing.wheel.entry.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExpirationWorker<E extends Entry> implements Callable<E> {

    private final static Log log = LogFactory.getLog(ExpirationWorker.class);

    private Expiration<E>[] expirations;
    private E e;
    private Callable<E> entryExpireHandler;

    @SafeVarargs
    public ExpirationWorker(Callable<E> entryExpireHandler, E e,
            Expiration<E>... expirations) {
        this.expirations = expirations;
        this.e = e;
        this.entryExpireHandler = entryExpireHandler;
    }

    public E call() throws Exception {
        if (entryExpireHandler != null) {
            entryExpireHandler.call();
        }
        if (expirations != null) {
            for (Expiration<E> expiration : expirations) {
                if (expiration != null) {
                    try {
                        expiration.expired(e);
                    } catch (Exception ex) {
                        log.error("use " + expiration + " expiration " + e + " failed:" + ex.getMessage(), ex);
                    }
                }
            }
        }
        return e;
    }

}
