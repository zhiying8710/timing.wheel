package me.binge.timing.wheel.impl.mem.test;

import java.util.UUID;

import me.binge.timing.wheel.entry.Entry;

public class AgeEntry extends Entry {

    public AgeEntry() {
    }

    private long age;

    public AgeEntry(long age) {
        super(UUID.randomUUID().toString());
        this.age = age;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "AgeEntry [age=" + age + ", key=" + key + ", time=" + time
                + ", cycle=" + cycle + ", slotId=" + slotId + "]";
    }


}
