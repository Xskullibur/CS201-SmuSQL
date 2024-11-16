package edu.smu.smusql.skipHash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class SkipListTest {

    private SkipList<Indexing> skipList;

    @BeforeEach
    public void setUp() {
        skipList = new SkipList<>();
        // Populate skip list with integer test data
        skipList.insert(new Indexing(10, "1", DataType.INTEGER));
        skipList.insert(new Indexing(20, "2", DataType.INTEGER));
        skipList.insert(new Indexing(30, "3", DataType.INTEGER));
        skipList.insert(new Indexing(40, "4", DataType.INTEGER));
        skipList.insert(new Indexing(50, "5", DataType.INTEGER));
    }

    @Test
    public void testGetValuesEqual() {
        List<Indexing> result = skipList.getValuesEqual(new Indexing(30, "-1", DataType.INTEGER));
        assertEquals(1, result.size(), "Expected one match for value 30");
        assertEquals(30, result.get(0).getColumnValue(), "Expected match for value 30");
    }

    @Test
    public void testGetValuesGreater() {
        List<Indexing> result = skipList.getValuesGreater(new Indexing(30, "0", DataType.INTEGER));
        assertEquals(2, result.size(), "Expected two matches for values greater than 30");
        assertEquals(40, result.get(0).getColumnValue(), "First match should be 40");
        assertEquals(50, result.get(1).getColumnValue(), "Second match should be 50");
    }

    @Test
    public void testGetValuesLesserOrEquals() {
        List<Indexing> result = skipList.getValuesLesserOrEquals(new Indexing(30, "0", DataType.INTEGER));
        assertEquals(3, result.size(), "Expected three matches for values less than or equal to 30");
        assertEquals(10, result.get(0).getColumnValue(), "First match should be 10");
        assertEquals(20, result.get(1).getColumnValue(), "Second match should be 20");
        assertEquals(30, result.get(2).getColumnValue(), "Third match should be 30");
    }

    @Test
    public void testGetValuesGreaterOrEquals() {
        List<Indexing> result = skipList.getValuesGreaterOrEquals(new Indexing(30, "0", DataType.INTEGER));
        assertEquals(3, result.size(), "Expected three matches for values greater than or equal to 30");
        assertEquals(30, result.get(0).getColumnValue(), "First match should be 30");
        assertEquals(40, result.get(1).getColumnValue(), "Second match should be 40");
        assertEquals(50, result.get(2).getColumnValue(), "Third match should be 50");
    }

    @Test
    public void testGetValuesLesser() {
        List<Indexing> result = skipList.getValuesLesser(new Indexing(30, "0", DataType.INTEGER));
        assertEquals(2, result.size(), "Expected two matches for values less than 30");
        assertEquals(10, result.get(0).getColumnValue(), "First match should be 10");
        assertEquals(20, result.get(1).getColumnValue(), "Second match should be 20");
    }

    // Additional test for string values
    @Test
    public void testStringValues() {
        SkipList<Indexing> stringSkipList = new SkipList<>();
        // Populate skip list with string test data
        stringSkipList.insert(new Indexing("apple", "1", DataType.STRING));
        stringSkipList.insert(new Indexing("banana", "2", DataType.STRING));
        stringSkipList.insert(new Indexing("cherry", "3", DataType.STRING));
        stringSkipList.insert(new Indexing("date", "4", DataType.STRING));
        stringSkipList.insert(new Indexing("elderberry", "5", DataType.STRING));

        // Test getValuesEqual
        List<Indexing> result = stringSkipList.getValuesEqual(new Indexing("cherry", "0", DataType.STRING));
        assertEquals(1, result.size(), "Expected one match for value 'cherry'");
        assertEquals("cherry", result.get(0).getColumnValue(), "Expected match for value 'cherry'");

        // Test getValuesGreater
        result = stringSkipList.getValuesGreater(new Indexing("banana", "0", DataType.STRING));
        assertEquals(3, result.size(), "Expected three matches for values greater than 'banana'");
        assertEquals("cherry", result.get(0).getColumnValue(), "First match should be 'cherry'");
        assertEquals("date", result.get(1).getColumnValue(), "Second match should be 'date'");
        assertEquals("elderberry", result.get(2).getColumnValue(), "Third match should be 'elderberry'");
    }
}
