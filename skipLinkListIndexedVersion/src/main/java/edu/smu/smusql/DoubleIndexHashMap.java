package edu.smu.smusql;
import java.util.*;

public class DoubleIndexHashMap<K1, K2, V> {
    // Map to store values by primary key
    private Map<K1, V> primaryMap;
    
    // Map to store the secondary key-to-primary key mapping
    private Map<K2, K1> secondaryToPrimaryMap;
    
    // Map to store primary key-to-secondary key mapping (for reverse lookups)
    private Map<K1, K2> primaryToSecondaryMap;

    public DoubleIndexHashMap() {
        this.primaryMap = new HashMap<>();
        this.secondaryToPrimaryMap = new HashMap<>();
        this.primaryToSecondaryMap = new HashMap<>();
    }

    // Insert a new value using primary and secondary keys
    public void insert(K1 primaryKey, K2 secondaryKey, V value) {
        primaryMap.put(primaryKey, value);             // Store in primary map
        secondaryToPrimaryMap.put(secondaryKey, primaryKey);  // Map secondary key to primary key
        primaryToSecondaryMap.put(primaryKey, secondaryKey);  // Track reverse relationship
    }

    // Get value by primary key
    public V getByPrimaryKey(K1 primaryKey) {
        return primaryMap.get(primaryKey);
    }

    // Get value by secondary key
    public V getBySecondaryKey(K2 secondaryKey) {
        K1 primaryKey = secondaryToPrimaryMap.get(secondaryKey);
        if (primaryKey != null) {
            return primaryMap.get(primaryKey);
        }
        return null;  // Not found
    }

    // Update a value by primary key
    public void updateByPrimaryKey(K1 primaryKey, V newValue) {
        if (primaryMap.containsKey(primaryKey)) {
            primaryMap.put(primaryKey, newValue);  // Simply update the value
        }
    }

    // Update a value by secondary key
    public void updateBySecondaryKey(K2 secondaryKey, V newValue) {
        K1 primaryKey = secondaryToPrimaryMap.get(secondaryKey);
        if (primaryKey != null) {
            primaryMap.put(primaryKey, newValue);  // Update value by primary key
        }
    }

    // Remove by primary key
    public boolean removeByPrimaryKey(K1 primaryKey) {
        if (primaryMap.containsKey(primaryKey)) {
            K2 secondaryKey = primaryToSecondaryMap.get(primaryKey);
            primaryMap.remove(primaryKey);  // Remove from primary map
            secondaryToPrimaryMap.remove(secondaryKey);  // Remove from secondary map
            primaryToSecondaryMap.remove(primaryKey);  // Remove reverse relationship
            return true;
        }
        return false;
    }

    // Remove by secondary key
    public boolean removeBySecondaryKey(K2 secondaryKey) {
        K1 primaryKey = secondaryToPrimaryMap.get(secondaryKey);
        if (primaryKey != null) {
            primaryMap.remove(primaryKey);  // Remove from primary map
            secondaryToPrimaryMap.remove(secondaryKey);  // Remove from secondary map
            primaryToSecondaryMap.remove(primaryKey);  // Remove reverse relationship
            return true;
        }
        return false;
    }

    // Get the secondary key associated with a primary key
    public K2 getSecondaryKeyByPrimaryKey(K1 primaryKey) {
        return primaryToSecondaryMap.get(primaryKey);
    }

    // Get the primary key associated with a secondary key
    public K1 getPrimaryKeyBySecondaryKey(K2 secondaryKey) {
        return secondaryToPrimaryMap.get(secondaryKey);
    }
}


