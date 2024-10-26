package edu.smu.smusql;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DoubleIndexHashMap<K1, K2, V extends Comparable<V>> {
    // Maps for double-indexing
    private Map<K1, V> primaryMap;
    private Map<K2, K1> secondaryToPrimaryMap;
    private Map<K1, K2> primaryToSecondaryMap;

    public DoubleIndexHashMap() {
        this.primaryMap = new HashMap<>();
        this.secondaryToPrimaryMap = new HashMap<>();
        this.primaryToSecondaryMap = new HashMap<>();
    }

    // Insert a new value using primary and secondary keys
    public void insert(K1 primaryKey, K2 secondaryKey, V value) {
        primaryMap.put(primaryKey, value);  // Store in primary map
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
        return primaryKey != null ? primaryMap.get(primaryKey) : null;
    }

    // Update a value by primary key
    public void updateByPrimaryKey(K1 primaryKey, V newValue) {
        if (primaryMap.containsKey(primaryKey)) {
            primaryMap.put(primaryKey, newValue);
        }
    }

    // Update a value by secondary key
    public void updateBySecondaryKey(K2 secondaryKey, V newValue) {
        K1 primaryKey = secondaryToPrimaryMap.get(secondaryKey);
        if (primaryKey != null) {
            primaryMap.put(primaryKey, newValue);
        }
    }

    // Remove by primary key
    public boolean removeByPrimaryKey(K1 primaryKey) {
        if (primaryMap.containsKey(primaryKey)) {
            K2 secondaryKey = primaryToSecondaryMap.remove(primaryKey);
            primaryMap.remove(primaryKey);
            secondaryToPrimaryMap.remove(secondaryKey);
            return true;
        }
        return false;
    }

    // Remove by secondary key
    public boolean removeBySecondaryKey(K2 secondaryKey) {
        K1 primaryKey = secondaryToPrimaryMap.remove(secondaryKey);
        if (primaryKey != null) {
            primaryMap.remove(primaryKey);
            primaryToSecondaryMap.remove(primaryKey);
            return true;
        }
        return false;
    }

    // Iterator over primaryMap entries
    public Iterator<Map.Entry<K1, V>> iterator() {
        return primaryMap.entrySet().iterator();
    }

    // Get values equal to a specific value
    public List<V> getValuesEqual(V value) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(value) == 0) results.add(v);
        });
        return results;
    }

    // Get values greater than a specific value
    public List<V> getValuesGreater(V value) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(value) > 0) results.add(v);
        });
        return results;
    }

    // Get values greater than or equal to a specific value
    public List<V> getValuesGreaterOrEquals(V value) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(value) >= 0) results.add(v);
        });
        return results;
    }

    // Get values lesser than a specific value
    public List<V> getValuesLesser(V value) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(value) < 0) results.add(v);
        });
        return results;
    }

    // Get values lesser than or equal to a specific value
    public List<V> getValuesLesserOrEquals(V value) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(value) <= 0) results.add(v);
        });
        return results;
    }

    // Get values between two specified values (exclusive)
    public List<V> getValuesBetween(V start, V end) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(start) > 0 && v.compareTo(end) < 0) results.add(v);
        });
        return results;
    }

    // Get values between two specified values (inclusive)
    public List<V> getValuesBetweenOrEquals(V start, V end) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(start) >= 0 && v.compareTo(end) <= 0) results.add(v);
        });
        return results;
    }

    // Get values between two values or equal to the start value
    public List<V> getValuesBetweenOrStartEquals(V start, V end) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(start) >= 0 && v.compareTo(end) < 0) results.add(v);
        });
        return results;
    }

    // Get values between two values or equal to the end value
    public List<V> getValuesBetweenOrEndEquals(V start, V end) {
        List<V> results = new ArrayList<>();
        primaryMap.values().forEach(v -> {
            if (v.compareTo(start) > 0 && v.compareTo(end) <= 0) results.add(v);
        });
        return results;
    }

    // toString method for printing DoubleIndexHashMap details
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DoubleIndexHashMap Contents:\n");
        for (Map.Entry<K1, V> entry : primaryMap.entrySet()) {
            K2 secondaryKey = primaryToSecondaryMap.get(entry.getKey());
            sb.append("Primary Key: ").append(entry.getKey())
              .append(", Secondary Key: ").append(secondaryKey)
              .append(", Value: ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public List<K1> returnKeysByRequirementOnId(V value, String operator) {
        List<K1> matchingKeys = new ArrayList<>();
    
        switch (operator) {
            case ">":
                primaryMap.forEach((key, val) -> {
                    if (val.compareTo(value) > 0) {
                        matchingKeys.add(key);
                    }
                });
                break;
            case ">=":
                primaryMap.forEach((key, val) -> {
                    if (val.compareTo(value) >= 0) {
                        matchingKeys.add(key);
                    }
                });
                break;
            case "<":
                primaryMap.forEach((key, val) -> {
                    if (val.compareTo(value) < 0) {
                        matchingKeys.add(key);
                    }
                });
                break;
            case "<=":
                primaryMap.forEach((key, val) -> {
                    if (val.compareTo(value) <= 0) {
                        matchingKeys.add(key);
                    }
                });
                break;
            case "=":
                primaryMap.forEach((key, val) -> {
                    if (val.compareTo(value) == 0) {
                        matchingKeys.add(key);
                    }
                });
                break;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    
        return matchingKeys;
    }

    public List<K1> returnKeysByRequirementsOnIndex(Map<K2, Map.Entry<V, String>> requirements) {
        List<K1> matchingKeys = new ArrayList<>();
    
        outerLoop:
        for (K1 primaryKey : primaryMap.keySet()) {
            for (Map.Entry<K2, Map.Entry<V, String>> condition : requirements.entrySet()) {
                K2 secondaryKey = condition.getKey();
                V targetValue = condition.getValue().getKey();
                String operator = condition.getValue().getValue();
    
                K1 mappedPrimaryKey = secondaryToPrimaryMap.get(secondaryKey);
                if (mappedPrimaryKey == null || !mappedPrimaryKey.equals(primaryKey)) {
                    continue outerLoop;
                }
    
                V cellValue = primaryMap.get(primaryKey);
                if (cellValue == null || !meetsCondition(cellValue, targetValue, operator)) {
                    continue outerLoop;
                }
            }
            matchingKeys.add(primaryKey); // Only add primary key if all conditions are met
        }
    
        return matchingKeys;
    }
    
    private boolean meetsCondition(V cellValue, V targetValue, String operator) {
        if (!(cellValue instanceof Comparable && targetValue instanceof Comparable)) {
            throw new IllegalArgumentException("Values must be comparable for this operation.");
        }
    
        @SuppressWarnings("unchecked")
        Comparable<V> comparableCellValue = (Comparable<V>) cellValue;
    
        switch (operator) {
            case ">":
                return comparableCellValue.compareTo(targetValue) > 0;
            case ">=":
                return comparableCellValue.compareTo(targetValue) >= 0;
            case "<":
                return comparableCellValue.compareTo(targetValue) < 0;
            case "<=":
                return comparableCellValue.compareTo(targetValue) <= 0;
            case "=":
                return comparableCellValue.compareTo(targetValue) == 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
    
}
