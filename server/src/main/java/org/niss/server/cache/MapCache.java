package org.niss.server.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ni187
 * 键值对缓存，使用读写锁保持数据一致性
 * 确保只有一个线程进入修改方法，修改时线程不能访问数据，但是未修改数据时，多个线程可以同时读取数据
 * 注：读取数据时，数据可能会有一定的滞后性
 */
public class MapCache<K,V>{

    private ReentrantReadWriteLock lock;

    private Map<K,V> cache;

    /**
     *
     * @param cacheMap 用以保存的键值对Map
     * @param fair 公平性 true:公平锁，false:非公平锁
     */
    public MapCache(Map<K,V> cacheMap,boolean fair){
        this.cache = cacheMap;
        lock = new ReentrantReadWriteLock(fair);
    }

    public MapCache(Map<K,V> cacheMap){
        this.cache = cacheMap;
        lock = new ReentrantReadWriteLock();
    }

    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();

        }finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return cache.isEmpty();
        }finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsKey(Object key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsValue(Object value) {
        lock.readLock().lock();
        try {
            return cache.containsValue(value);
        }finally {
            lock.readLock().unlock();
        }
    }

    public V get(Object key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return cache.put(key, value);
        }finally {
            lock.writeLock().unlock();
        }
    }

    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            return cache.remove(key);
        }finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        }finally {
            lock.writeLock().unlock();
        }
    }

    public Set<K> keySet() {
        lock.readLock().lock();
        try {
            return cache.keySet();
        }finally {
            lock.readLock().unlock();
        }
    }

    public Collection<V> values() {
        lock.readLock().lock();
        try {
            return cache.values();
        }finally {
            lock.readLock().unlock();
        }
    }
}
