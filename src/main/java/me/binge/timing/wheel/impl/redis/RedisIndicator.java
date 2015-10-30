package me.binge.timing.wheel.impl.redis;

import static me.binge.timing.wheel.utils.RedisConstant.entriesSlotKey;
import me.binge.redis.exec.RedisExecutor;
import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.Wheel;
import me.binge.timing.wheel.entry.Entry;

import org.apache.commons.lang3.StringUtils;

public class RedisIndicator<E extends Entry> implements Indicator<E> {

    private RedisExecutor<?> redisExecutor;
    private Wheel<E> wheel;

    public RedisIndicator(RedisExecutor<?> redisExecutor, Wheel<E> wheel) {
        this.redisExecutor = redisExecutor;
        this.wheel = wheel;
    }

    @Override
    public void put(E e, Slot<E> slot) {
        this.redisExecutor.hset(entriesSlotKey(), e.getKey(), slot.getId() + "");
    }

    @Override
    public Slot<E> get(E e) {
        String ssid = this.redisExecutor.hget(entriesSlotKey(), e.getKey());
        if (StringUtils.isBlank(ssid)) {
            return null;
        }
        return wheel.get(Integer.valueOf(ssid));
    }

    @Override
    public void remove(E e) {
        this.redisExecutor.hdel(entriesSlotKey(), e.getKey());
    }

}
