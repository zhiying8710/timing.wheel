package me.binge.timing.wheel.impl.redis;

import static me.binge.timing.wheel.utils.RedisConstant.currIdxKey;
import static me.binge.timing.wheel.utils.RedisConstant.currCycleKey;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.binge.redis.exec.RedisExecutor;
import me.binge.timing.wheel.Indicator;
import me.binge.timing.wheel.Slot;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.entry.Entry;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.tick.TickCondition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RedisTimingWheel<E extends Entry> extends TimingWheel<E> {

    private final static Log log = LogFactory.getLog(RedisTimingWheel.class);

    private RedisExecutor<?> redisExecutor;
    private RedisIndicator<E> indicator;

    private volatile int currentTickIndex = 0;
    private volatile long tickTime = 0;

    private volatile long currentCycle = 0;
    private volatile long cycleTime = 0;

    @SafeVarargs
    public RedisTimingWheel(int tickDuration, int ticksPerWheel,
            TimeUnit timeUnit, String wheelName, RedisExecutor<?> redisExecutor, TickCondition notifyExpireCondition, Expiration<E>... expirations) {
        super(tickDuration, ticksPerWheel, timeUnit, wheelName, notifyExpireCondition, expirations);
        this.redisExecutor = redisExecutor;
        this.indicator = new RedisIndicator<E>(redisExecutor, this.wheel);
    }


    @Override
    public Indicator<E> getIndicator() {
        return indicator;
    }

    @Override
    public Slot<E> workSlot(long cycle, int id) {
        return new RedisSlot<E>(cycle, id, redisExecutor);
    }

    @Override
    protected int getCurrentTickIndex() {
        try {
            Map<String, String> currIdxInfo = redisExecutor.hgetAll(currIdxKey());
            if (currIdxInfo == null || currIdxInfo.isEmpty()) {
                this.currentTickIndex = 0;
                return 0;
            }
            int oCurrIdx = Integer.valueOf(currIdxInfo.get("idx"));
            if (oCurrIdx != this.currentTickIndex) {
                long oTickTime = Long.valueOf(currIdxInfo.get("tickTime"));
                if (oTickTime > this.tickTime) { // if false, means the idx in redis is over time.
                    this.currentTickIndex = oCurrIdx;
                }
            }
        } catch (Exception e) {
            log.error("get current tick idx error: " + e.getMessage(), e);
        }
        return this.currentTickIndex;
    }

    @Override
    protected int setCurrentTickIndex(int currentTickIndex) {
        if (currentTickIndex == this.ticksPerWheel) {
            currentTickIndex = 0;
        }
        long now = System.currentTimeMillis();
        try {
            Map<String, String> currIdxInfo = new HashMap<String, String>();
            currIdxInfo.put("idx", currentTickIndex + "");
            currIdxInfo.put("tickTime", now + "");
            redisExecutor.hmset(currIdxKey(), currIdxInfo);
        } catch (Exception e) {
            log.error("set current tick idx error: " + e.getMessage(), e);
        }
        this.tickTime = now;
        this.currentTickIndex = currentTickIndex;
        return this.currentTickIndex;
    }


    @Override
    protected long getCurrentCycle() {
        try {
            Map<String, String> cycleInfo = this.redisExecutor.hgetAll(currCycleKey());
            if (cycleInfo == null || cycleInfo.isEmpty()) {
                this.currentCycle = 0;
                return 0;
            }
            long oCycle = Long.valueOf(cycleInfo.get("cycle"));
            if (oCycle != this.currentCycle) {
                long oCycleTime = Long.valueOf(cycleInfo.get("cycleTime"));
                if (oCycleTime > this.cycleTime) { // if false, means the idx in redis is over time.
                    this.currentCycle = oCycle;
                }
            }
        } catch (Exception e) {
             log.error("get current cycle error: " + e.getMessage(), e);
        }
        return this.currentCycle;
    }


    @Override
    protected void incrCurrentCycle() {
        long now = System.currentTimeMillis();
        try {
            Map<String, String> currCycleInfo = new HashMap<String, String>();
            currCycleInfo.put("cycle", (this.currentCycle + 1) + "");
            currCycleInfo.put("cycleTime", now + "");
            this.redisExecutor.hmset(currCycleKey(), currCycleInfo);
        } catch (Exception e) {
            log.error("incr current cycle error: " + e.getMessage(), e);
        }
        this.cycleTime = now;
        this.currentCycle ++;
    }

}
