package edu.smu.smusql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class EngineTest {

    private Engine engine;
    private Table mockTable;
    private SkipList<Row> mockSkipList; // Add this to mock SkipList

    @BeforeEach
    public void setUp() {
        engine = new Engine();
        mockTable = mock(Table.class);
        mockSkipList = mock(SkipList.class); // Mock SkipList<Row>
        engine.data.put("student", mockTable);
    }

    @Test
    public void testCreateTable() {
        String createQuery = "CREATE TABLE student (id, name, age, gpa)";
        String result = engine.executeSQL(createQuery);
        
        assertEquals("Table student created successfully", result);
        assertTrue(engine.data.containsKey("student"));
    }

    @Test
    public void testInsertIntoTable() {
        when(mockTable.getColumns()).thenReturn(Arrays.asList("id", "name", "age", "gpa"));

        String insertQuery = "INSERT INTO student VALUES (1, 'John Doe', 20, 3.5)";
        String result = engine.executeSQL(insertQuery);

        assertEquals("inserted into student successfully", result);
        verify(mockTable).insertRow(eq("1"), anyMap());
    }

    @Test
    public void testSelectFromTable() {
        // Mock data for the select operation
        when(mockTable.getColumns()).thenReturn(Arrays.asList("id", "name", "age", "gpa"));
        Row mockRow = mock(Row.class);
        when(mockRow.getData()).thenReturn(Map.of("id", "1", "name", "John Doe", "age", "20", "gpa", "3.5"));

        // The method getData() now returns the mocked SkipList instead of a List
        when(mockTable.getData()).thenReturn(mockSkipList);
        when(mockSkipList.iterator()).thenReturn(Arrays.asList(mockRow).iterator());

        // Test the SELECT * FROM statement
        String selectQuery = "SELECT * FROM student";
        String result = engine.executeSQL(selectQuery);

        assertTrue(result.contains("John Doe"));
    }

    @Test
    public void testDeleteFromTable() {
        when(mockTable.returnKeysByRequirementsOnIndex(anyString(), anyString(), anyString())).thenReturn(Arrays.asList("1"));

        String deleteQuery = "DELETE FROM student WHERE age = 20";
        String result = engine.executeSQL(deleteQuery);

        assertEquals("1 rows deleted successfully", result);
        verify(mockTable).deleteRow("1");
    }

    @Test
    public void testUpdateTable() {
        when(mockTable.returnKeysByRequirementsOnIndex(anyString(), anyString(), anyString())).thenReturn(Arrays.asList("1"));
        Row mockRow = mock(Row.class);
        Map<String, String> rowData = new HashMap<>();
        rowData.put("age", "20");
        when(mockRow.getData()).thenReturn(rowData);
        when(mockTable.getRow("1")).thenReturn(mockRow);

        String updateQuery = "UPDATE student SET age = 25 WHERE id = 1";
        String result = engine.executeSQL(updateQuery);

        assertEquals("1 rows updated successfully", result);
        assertEquals("25", rowData.get("age"));
        verify(mockTable).updateRow(eq("1"), anyMap());
    }
}
