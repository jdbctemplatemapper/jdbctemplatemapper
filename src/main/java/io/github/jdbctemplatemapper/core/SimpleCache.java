package io.github.jdbctemplatemapper.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SimpleCache<K, V> {
  private Map<K, V> cache = new ConcurrentHashMap<>();
  private int capacity = -1; // no limit

  public SimpleCache() {}

  public SimpleCache(int capacity) {
    this.capacity = capacity;
  }

  public V get(K key) {
    return cache.get(key);
  }

  public void put(K key, V value) {
    if (capacity == -1) { // no limit
      cache.putIfAbsent(key, value);
    } else {
      if (cache.size() < capacity) {
        cache.putIfAbsent(key, value);
      } else {
        // remove a random entry from cache and add new entry
        K k = cache.keySet().iterator().next();
        cache.remove(k);
        cache.putIfAbsent(key, value);
      }
    }
  }

  public V remove(K key) {
    return cache.remove(key);
  }

  public boolean containsKey(K key) {
    return cache.containsKey(key);
  }

  public int getSize() {
    return cache.size();
  }

  public void clear() {
    cache.clear();
  }

}
