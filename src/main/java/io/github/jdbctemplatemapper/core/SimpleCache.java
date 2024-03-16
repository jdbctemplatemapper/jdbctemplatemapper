package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SimpleCache<K, V> {
  private Map<K, V> cache = new ConcurrentHashMap<>();
  private int capacity = -1; // no limit
  private double shrinkPercentage = 0.1; // 10%

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
        // shrink cache by shrinkPercentage
        int removeCnt = (int) (capacity * shrinkPercentage);
        if (removeCnt > 0) {
          List<K> keys = new ArrayList<>(cache.keySet());
          // delete random entries
          Collections.shuffle(keys);
          for (K k : keys) {
            cache.remove(k);
            --removeCnt;
            if (removeCnt <= 0) {
              break;
            }
          }
        }
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
