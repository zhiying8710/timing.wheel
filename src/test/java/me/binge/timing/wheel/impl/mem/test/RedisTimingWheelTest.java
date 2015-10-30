package me.binge.timing.wheel.impl.mem.test;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

        final TimingWheel<AgeEntry> wheel = new RedisTimingWheel<AgeEntry>(100, 100, TimeUnit.MILLISECONDS, "xxx", new Expiration<AgeEntry>() {

            @Override
            public void expired(AgeEntry entry) {
                System.out.println(entry.getKey() + "::" + (System.currentTimeMillis() - entry.getAge()));
            }
        }, redisExecutor, null);


        new Thread(new Runnable() {

            @Override
            public void run() {
                wheel.start();
            }
        }).start();

        while (!wheel.running()) {

        }
        System.out.println("wheel running...");
        int x = 100;
        for (int i = 0; i < x; i++) {
            wheel.add(new AgeEntry(System.currentTimeMillis()));
            TimeUnit.SECONDS.sleep(1);
        }

        System.in.read();
    }

}

