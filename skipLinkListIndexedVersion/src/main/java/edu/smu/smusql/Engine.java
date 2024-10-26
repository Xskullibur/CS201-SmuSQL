package edu.smu.smusql;

import java.util.*;
import java.util.stream.*;
import java.util.regex.*;

public class Engine<K1, K2, V extends Comparable<V>> {
    private Map<String, DoubleIndexHashMap<K1, K2, V>> database;

    public Engine() {
        this.database = new HashMap<>();
    }

    // Execute an SQL-like query
    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "CREATE":
                return create(tokens);
            case "INSERT":
                return insert(tokens);
            case "SELECT":
                return select(tokens);
            case "UPDATE":
                return update(tokens);
            case "DELETE":
                return delete(tokens);
            default:
                return "ERROR: Unknown command";
        }
    }

    // Create a new DoubleIndexHashMap table
    public String create(String[] tokens) {
        String tableName = tokens[2].toLowerCase();
        DoubleIndexHashMap<K1, K2, V> table = new DoubleIndexHashMap<>();
        database.put(tableName, table);
        return "Table " + tableName + " created successfully.";
    }

    // Insert a new row into the DoubleIndexHashMap
    public String insert(String[] tokens) {
        String tableName = tokens[2].toLowerCase();
        DoubleIndexHashMap<K1, K2, V> table = database.get(tableName);

        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }

        K1 primaryKey = (K1) tokens[4];  // Primary key (assume type casting is correct)
        K2 secondaryKey = (K2) tokens[5]; // Secondary key
        V value = parseValue(tokens[6]);  // Value

        table.insert(primaryKey, secondaryKey, value);
        return "Inserted into " + tableName + " successfully.";
    }

    // Select rows based on conditions
    public String select(String[] tokens) {
        String tableName = tokens[3].toLowerCase();
        DoubleIndexHashMap<K1, K2, V> table = database.get(tableName);

        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }

        List<V> resultValues = new ArrayList<>();
        if (tokens.length > 4 && tokens[4].equalsIgnoreCase("WHERE")) {
            String column = tokens[5];
            String operator = tokens[6];
            V value = parseValue(tokens[7]);

            // Add filtered results based on the column (primary or secondary key)
            if (column.equalsIgnoreCase("primary")) {
                resultValues.addAll(selectByPrimaryKeyCondition(table, operator, value));
            } else if (column.equalsIgnoreCase("secondary")) {
                resultValues.addAll(selectBySecondaryKeyCondition(table, operator, value));
            } else {
                return "ERROR: Unknown column " + column;
            }
        } else {
            // Select all if no conditions
            resultValues.addAll(table.getValuesEqual((V) "*"));
        }

        return formatSelectResults(resultValues);
    }

    // Update rows based on conditions
    public String update(String[] tokens) {
        String tableName = tokens[1].toLowerCase();
        DoubleIndexHashMap<K1, K2, V> table = database.get(tableName);

        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }

        String setColumn = tokens[3];
        V newValue = parseValue(tokens[5]);

        List<K1> keysToUpdate = getKeysByCondition(table, tokens, 6);

        for (K1 key : keysToUpdate) {
            if (setColumn.equalsIgnoreCase("primary")) {
                table.updateByPrimaryKey(key, newValue);
            } else {
                return "ERROR: Unsupported column for update";
            }
        }

        return keysToUpdate.size() + " rows updated successfully.";
    }

    // Delete rows based on conditions
    public String delete(String[] tokens) {
        String tableName = tokens[2].toLowerCase();
        DoubleIndexHashMap<K1, K2, V> table = database.get(tableName);

        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }

        List<K1> keysToDelete = getKeysByCondition(table, tokens, 4);

        for (K1 key : keysToDelete) {
            table.removeByPrimaryKey(key);
        }

        return keysToDelete.size() + " rows deleted successfully.";
    }

    // Helper to parse condition and fetch keys
    private List<K1> getKeysByCondition(DoubleIndexHashMap<K1, K2, V> table, String[] tokens, int whereIndex) {
        List<K1> keys = new ArrayList<>();
    
        if (tokens.length > whereIndex && tokens[whereIndex].equalsIgnoreCase("WHERE")) {
            K2 column = (K2) tokens[whereIndex + 1]; // Cast to K2 type
            String operator = tokens[whereIndex + 2];
            V value = parseValue(tokens[whereIndex + 3]);
    
            if (column.equalsIgnoreCase("primary")) {
                keys.addAll(table.returnKeysByRequirementOnPrimaryKey(value, operator));
            } else if (column.equalsIgnoreCase("secondary")) {
                Map<K2, Map.Entry<V, String>> requirements = Collections.singletonMap(column, new AbstractMap.SimpleEntry<>(value, operator));
                keys.addAll(table.returnKeysByRequirementsOnIndex(requirements));
            }
        }
        return keys;
    }
    

    // Select based on primary key conditions
    private List<V> selectByPrimaryKeyCondition(DoubleIndexHashMap<K1, K2, V> table, String operator, V value) {
        List<V> results;
        switch (operator) {
            case ">":
                results = table.getValuesGreater(value);
                break;
            case ">=":
                results = table.getValuesGreaterOrEquals(value);
                break;
            case "<":
                results = table.getValuesLesser(value);
                break;
            case "<=":
                results = table.getValuesLesserOrEquals(value);
                break;
            case "=":
                results = table.getValuesEqual(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
        return results;
    }

    // Select based on secondary key conditions
    private List<V> selectBySecondaryKeyCondition(DoubleIndexHashMap<K1, K2, V> table, String operator, V value) {
        return selectByPrimaryKeyCondition(table, operator, value);
    }

    // Parse value to appropriate type
    @SuppressWarnings("unchecked")
    private V parseValue(String value) {
        try {
            return (V) Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return (V) value;
        }
    }

    // Helper method to format select results
    private String formatSelectResults(List<V> results) {
        StringBuilder sb = new StringBuilder("Results:\n");
        for (V result : results) {
            sb.append(result).append("\n");
        }
        return sb.toString().trim();
    }
}

