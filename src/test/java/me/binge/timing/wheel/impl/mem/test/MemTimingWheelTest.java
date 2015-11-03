package me.binge.timing.wheel.impl.mem.test;

import java.util.concurrent.TimeUnit;

import me.binge.timing.wheel.TimingWheel;
import me.binge.timing.wheel.expire.Expiration;
import me.binge.timing.wheel.impl.mem.MemTimingWheel;

public class MemTimingWheelTest {

    public static void main(String[] args) throws Exception {

        final TimingWheel<AgeEntry> wheel = new MemTimingWheel<AgeEntry>(500, 20, TimeUnit.MILLISECONDS, "xxx", new Expiration<AgeEntry>() {

            @Override
            public void expired(AgeEntry entry) {
                System.out.println(entry.getKey() + "::" + (System.currentTimeMillis() - entry.getAge()));
            }
        });


        new Thread(new Runnable() {

            @Override
            public void run() {
                wheel.start();
            }
        }).start();

        int x = 5;
        for (int i = 0; i < x; i++) {
            wheel.add(new AgeEntry(System.currentTimeMillis()));
            TimeUnit.SECONDS.sleep(1);
        }

        System.in.read();
    }

}

