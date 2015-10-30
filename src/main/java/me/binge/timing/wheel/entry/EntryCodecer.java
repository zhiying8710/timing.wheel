package me.binge.timing.wheel.entry;


public interface EntryCodecer<E extends Entry, T> {

    public T encode(E e);

    public E decode(T t);
}
