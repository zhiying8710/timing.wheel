package me.binge.timing.wheel.impl.redis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.binge.redis.exec.RedisExecutor;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.entry.Entry;
import static me.binge.timing.wheel.utils.RedisConstant.*;

public class RedisSlot<E extends Entry> extends Slot<E> {

    private RedisExecutor<?> redisExecutor;
    private RedisEntryCodecer<E> entryCodecer = new RedisEntryCodecer<E>();


    public RedisSlot(int id, RedisExecutor<?> redisExecutor) {
        super(id);
        this.redisExecutor = redisExecutor;
    }

    @Override
    public void add(E e) {
        this.redisExecutor.hset(slotKeyPrefix() + this.getId(), e.getKey(), entryCodecer.encode(e));
    }

    @Override
    public E remove(E e) {
        this.redisExecutor.hdel(slotKeyPrefix() + this.getId(), e.getKey());
        return e;
    }

    @Override
    public Set<E> elements() {
        List<String> vals = this.redisExecutor.hvals(slotKeyPrefix() + this.getId());
        Set<E> entries = new HashSet<E>();
        for (String val : vals) {
            entries.add(this.entryCodecer.decode(val));
        }
        return entries;
    }

}
