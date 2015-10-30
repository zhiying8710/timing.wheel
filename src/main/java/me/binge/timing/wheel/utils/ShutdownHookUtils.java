package me.binge.timing.wheel.utils;

import java.util.concurrent.ExecutorService;

public class ShutdownHookUtils {

    public static void hook(final ExecutorService executor, final boolean now) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {

                if (executor != null) {
                    if (now) {
                        executor.shutdownNow();
                    } else {
                        executor.shutdown();
                    }
                }

            }
        }));
    }

}
