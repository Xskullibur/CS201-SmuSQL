package edu.smu.smusql.bplustreeA.lruCache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;

    public LRUCache(int maxEntries) {
        super(maxEntries, 0.75f, true); // access-order for LRU
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }
}