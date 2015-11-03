package me.binge.timing.wheel.impl.redis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import me.binge.redis.exec.RedisExecutor;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import static me.binge.timing.wheel.utils.RedisConstant.*;

public class RedisSlot<E extends Entry> extends Slot<E> {

    private final static Log log = LogFactory.getLog(TimingWheel.class);

    private String allEntriesKey;
    private String slotKey;

    private RedisExecutor<?> redisExecutor;
    private RedisEntryCodecer<E> entryCodecer = new RedisEntryCodecer<E>();

    public RedisSlot(long cycle, int id, RedisExecutor<?> redisExecutor) {
        super(cycle, id);
        this.redisExecutor = redisExecutor;
        this.slotKey = slotKeyPrefix() + this.getCycle() + "_" + this.getId();
        this.allEntriesKey = slotKey + "_vals";
    }

    @Override
    public void add(E e) {
        String val = null;
        try {
            val = entryCodecer.encode(e);
            this.redisExecutor.hset(slotKey, e.getKey(), val);
        } catch (Exception ex) {
            log.error("add " + val + " to slot " + this.getId() + " failed:" + ex.getMessage(), ex);
        }
    }

    @Override
    public E remove(E e) {
        try {
            this.redisExecutor.hdel(slotKey, e.getKey());
        } catch (Exception ex) {
            log.error("remove " + e + " from slot " + this.getCycle() + "_" + this.getId() + " failed:" + ex.getMessage(), ex);
        }
        return e;
    }

    private boolean storeAllEntries() {
        this.redisExecutor.del(allEntriesKey);
        List<String> vals = this.redisExecutor.hvals(slotKey);
        if (vals == null || vals.isEmpty()) {
            return false;
        }
        this.redisExecutor.sadd(allEntriesKey, vals.toArray(new String[]{}));
        return true;
    }

    @Override
    public Set<E> elements() {
        Set<E> entries = new HashSet<E>();
        try {
            if (!storeAllEntries()) {
                return entries;
            }
            Set<String> vals = new HashSet<String>();
            String v = null;
            while ((v = this.redisExecutor.spop(allEntriesKey)) != null) {
                vals.add(v);
            }
            for (String val : vals) {
                entries.add(this.entryCodecer.decode(val));
            }
        } catch (Exception e) {
            log.error("get entries from slot " + this.getId() + " error:" + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public String toString() {
        return "RedisSlot [allEntriesKey=" + allEntriesKey + ", slotKey="
                + slotKey + ", cycle=" + cycle + ", id=" + id + "]";
    }

}
