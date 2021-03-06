package com.intrepiditee;

import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ConcurrentArrayList<E> extends ArrayList<E> {

    private final ReadWriteLock lock;


    ConcurrentArrayList() {
        super();
        lock = new ReentrantReadWriteLock();
    }

    ConcurrentArrayList(int capacity) {
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
