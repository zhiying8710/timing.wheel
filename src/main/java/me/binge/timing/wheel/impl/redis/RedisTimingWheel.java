package me.binge.timing.wheel.impl.redis;

import static me.binge.timing.wheel.utils.RedisConstant.currIdxKey;

import java.util.concurrent.TimeUnit;

import me.binge.redis.exec.RedisExecutor;
import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.tick.TickCondition;

import org.apache.commons.lang3.StringUtils;

public class RedisTimingWheel<E extends Entry> extends TimingWheel<E> {

    private RedisExecutor<?> redisExecutor;
    private RedisIndicator<E> indicator;

    public RedisTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName, Expiration<E> expiration, RedisExecutor<?> redisExecutor, TickCondition notifyExpireCondition) {
        super(tickDuration, ticksPerWheel, timeUnit, wheelName, expiration, notifyExpireCondition);
        this.redisExecutor = redisExecutor;
        this.indicator = new RedisIndicator<E>(redisExecutor, this.wheel);
    }


    @Override
    public Indicator<E> getIndicator() {
        return indicator;
    }

    @Override
    public Slot<E> workSlot(int id) {
        return new RedisSlot<E>(id, redisExecutor);
    }

    @Override
    protected int getCurrentTickIndex() {
        String sCurrIdx = redisExecutor.get(currIdxKey());
        if (StringUtils.isBlank(sCurrIdx)) {
            return 0;
        }
        return Integer.valueOf(sCurrIdx);
    }

    @Override
    protected void setCurrentTickIndex(int currentTickIndex) {
        redisExecutor.set(currIdxKey(), currentTickIndex + "");
    }

}
