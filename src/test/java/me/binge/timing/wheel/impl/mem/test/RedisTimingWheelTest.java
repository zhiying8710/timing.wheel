package me.binge.timing.wheel.impl.mem.test;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;

import me.binge.redis.exec.RedisExecutor;
import me.binge.redis.exec.RedisExecutors;
import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.impl.redis.RedisTimingWheel;

public class RedisTimingWheelTest {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        InputStream is = RedisTimingWheelTest.class.getClassLoader().getResourceAsStream("redis-conf.properties");
        try {
            props.load(is);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        RedisExecutor<?> redisExecutor = RedisExecutors.get(props);

        final TimingWheel<AgeEntry> wheel = new RedisTimingWheel<AgeEntry>(100, 100, TimeUnit.MILLISECONDS, "xxx", redisExecutor, null, new Expiration<AgeEntry>() {

            @Override
            public void expired(AgeEntry entry) {
                System.out.println(entry + "::" + (System.currentTimeMillis() - entry.getTime()));
            }
        });

        wheel.start();

        while (!wheel.running()) {

        }
        System.out.println("wheel running...");
        int x = 1000;
        for (int i = 0; i < x; i++) {
            wheel.add(new AgeEntry(System.currentTimeMillis()));
            TimeUnit.MILLISECONDS.sleep(RandomUtils.nextLong(200, 1000));
        }

        System.in.read();
    }

}

