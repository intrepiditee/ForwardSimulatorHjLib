package com.intrepiditee;

import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentArrayList<E> extends ArrayList<E> {

    private ReadWriteLock lock;


    public ConcurrentArrayList() {
        super();
        lock = new ReentrantReadWriteLock();
    }

    public ConcurrentArrayList(int capacity) {
        super(capacity);
        lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean add(E element) {
        lock.writeLock().lock();
        boolean ret = super.add(element);
        lock.writeLock().unlock();

        return ret;
    }


    @Override
    public E get(int index) {
        lock.readLock().lock();
        E element = super.get(index);
        lock.readLock().unlock();

        return element;
    }

    @Override
    public int size() {
        lock.readLock().lock();
        int size = super.size();
        lock.readLock().unlock();

        return size;
    }
}
