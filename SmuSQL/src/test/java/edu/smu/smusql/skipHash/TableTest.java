package edu.smu.smusql.skipHash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class TableTest {

    private Table table;

    @BeforeEach
    public void setup() {
        // Initialize the table with name and columns
        List<String> columns = Arrays.asList("id", "price");
        table = new Table("products", columns);

        // Prepare test data
        Map<String, String> rowData1 = new HashMap<>();
        rowData1.put("id", "key1");
        rowData1.put("price", "100");

        Map<String, String> rowData2 = new HashMap<>();
        rowData2.put("id", "key2");
        rowData2.put("price", "200");

        Map<String, String> rowData3 = new HashMap<>();
        rowData3.put("id", "key3");
        rowData3.put("price", "150");

        Map<String, String> rowData4 = new HashMap<>();
        rowData4.put("id", "key4");
        rowData4.put("price", "100");

        Map<String, String> rowData5 = new HashMap<>();
        rowData5.put("id", "key5");
        rowData5.put("price", "200");

        // Insert rows into the table
        table.insertRow("key1", rowData1);
        table.insertRow("key2", rowData2);
        table.insertRow("key3", rowData3);
        table.insertRow("key4", rowData4);
        table.insertRow("key5", rowData5);
    }

    @Test
    public void testReturnKeysByGreaterThan() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", ">", "150");
        assertEquals(Set.of("key2", "key5"), new HashSet<>(result), "Expected keys greater than 150");
    }

    @Test
    public void testReturnKeysByLessThan() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "<", "150");
        assertEquals(Set.of("key1", "key4"), new HashSet<>(result), "Expected keys less than 150");
    }

    @Test
    public void testReturnKeysByEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "=", "100");
        assertEquals(Set.of("key1", "key4"), new HashSet<>(result), "Expected keys equal to 100");
    }

    @Test
    public void testReturnKeysByNotEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "!=", "150");
        assertEquals(Set.of("key1", "key2", "key4", "key5"), new HashSet<>(result), "Expected keys not equal to 150");
    }

    @Test
    public void testReturnKeysByGreaterThanOrEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", ">=", "150");
        assertEquals(Set.of("key2", "key3", "key5"), new HashSet<>(result), "Expected keys greater than or equal to 150");
    }

    @Test
    public void testReturnKeysByLessThanOrEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "<=", "150");
        assertEquals(Set.of("key1", "key3", "key4"), new HashSet<>(result), "Expected keys less than or equal to 150");
    }
}
