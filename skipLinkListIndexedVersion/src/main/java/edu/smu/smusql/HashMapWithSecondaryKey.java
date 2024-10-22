package edu.smu.smusql;

import java.util.*;

public class HashMapWithSecondaryKey<K1 extends Comparable<K1>, K2 extends Comparable<K2>, V> {
    // Primary HashMap indexed by primary key (K1)
    private Map<K1, V> primaryMap;
    
    // Secondary Skip List indexed by secondary key (K2)
    private SkipList<K2> secondaryIndex;
    private Map<K2, List<K1>> secondaryToPrimaryMap;

    private Map<K1, K2> primaryToSecondaryMap;

    public HashMapWithSecondaryKey() {
        this.primaryMap = new HashMap<>();
        this.secondaryIndex = new SkipList<>();
        this.secondaryToPrimaryMap = new HashMap<>();
        this.primaryToSecondaryMap = new HashMap<>();
    }

    // Insert a key-value pair into both primary map and secondary index
    public void insert(K1 primaryKey, K2 secondaryKey, V value) {
        primaryMap.put(primaryKey, value);  // Store value by primary key
        
        // Insert primary key into the secondary index (skip list)
        secondaryIndex.insert(secondaryKey);
        
        // Update the secondary to primary mapping
        secondaryToPrimaryMap.computeIfAbsent(secondaryKey, k -> new ArrayList<>()).add(primaryKey);
    }

    // Retrieve value by primary key
    public V getByPrimaryKey(K1 primaryKey) {
        return primaryMap.get(primaryKey);  // Simple HashMap lookup
    }

    // Retrieve all primary keys by a range on the secondary key
    public List<K1> getBySecondaryKeyRange(K2 lowerBound, K2 upperBound) {
        List<K1> result = new ArrayList<>();
        
        // Get all secondary keys within the range from the skip list
        List<K2> secondaryKeysInRange = secondaryIndex.getValuesBetween(lowerBound, upperBound);
        
        // Retrieve all primary keys corresponding to those secondary keys
        for (K2 secondaryKey : secondaryKeysInRange) {
            result.addAll(secondaryToPrimaryMap.get(secondaryKey));
        }
        
        return result;
    }

    // Remove a value by primary key, and also remove it from the secondary index
    public boolean deleteByPrimaryKey(K1 primaryKey, K2 secondaryKey) {
        if (primaryMap.remove(primaryKey) != null) {
            secondaryIndex.delete(secondaryKey);  // Remove from secondary index (skip list)
            List<K1> primaryKeys = secondaryToPrimaryMap.get(secondaryKey);
            if (primaryKeys != null) {
                primaryKeys.remove(primaryKey);  // Remove primary key from secondary-to-primary map
                if (primaryKeys.isEmpty()) {
                    secondaryToPrimaryMap.remove(secondaryKey);  // Clean up if no more primary keys
                }
            }
            return true;
        }
        return false;
    }

    public void update(K1 primaryKey, K2 newSecondaryKey, V newValue) {
        V oldValue = primaryMap.get(primaryKey);
        if (oldValue != null) {
            K2 oldSecondaryKey = getSecondaryKeyByPrimaryKey(primaryKey); // You might need to track this
            deleteByPrimaryKey(primaryKey, oldSecondaryKey);
            insert(primaryKey, newSecondaryKey, newValue);
        }
    }

    public K2 getSecondaryKeyByPrimaryKey(K1 primaryKey) {
        return primaryToSecondaryMap.get(primaryKey);
    }
    
}

