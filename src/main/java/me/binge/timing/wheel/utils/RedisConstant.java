package me.binge.timing.wheel.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

public class RedisConstant {

    private static final String DEFAULT_PREFIX = RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") + "__";

    private volatile static String keyPrefix;

    public static void setKeyPrefix(String prefix) {
        keyPrefix = prefix;
    }

    private static String check() {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = DEFAULT_PREFIX;
        }
        if (!keyPrefix.endsWith("__")) {
            keyPrefix = keyPrefix + "__";
        }
        return keyPrefix;
    }

    public static String slotsKey() {
        return check() + "timingwheel_slots";
    }

    public static String slotKeyPrefix() {
        return check() + "timingwheel_slot_idx_";
    }

    public static String currIdxKey() {
        return check() + "timingwheel_current_idx";
    }

    public static String entriesSlotKey() {
        return check() + "timingwheel_entries_slot";
    }

    public static String currCycleKey() {
        return check() + "timingwheel_current_cycle";
    }


}
