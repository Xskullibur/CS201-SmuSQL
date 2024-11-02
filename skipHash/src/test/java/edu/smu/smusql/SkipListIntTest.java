package edu.smu.smusql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SkipListIntTest {
    private SkipList<Integer> skipList;

    @BeforeEach
    public void setUp() {
        skipList = new SkipList<>();
        // Populate the skip list with sample integer values
        skipList.insert(10);
        skipList.insert(20);
        skipList.insert(30);
        skipList.insert(40);
        skipList.insert(50);
    }

    @Test
    public void testGetValuesEqual() {
        List<Integer> result = skipList.getValuesEqual(30);
        assertEquals(1, result.size(), "Expected one match for value 30");
        assertEquals(30, result.get(0), "Match should be 30");
    }

    @Test
    public void testGetValuesGreater() {
        List<Integer> result = skipList.getValuesGreater(30);
        assertEquals(2, result.size(), "Expected two matches for values greater than 30");
        assertEquals(40, result.get(0), "First match should be 40");
        assertEquals(50, result.get(1), "Second match should be 50");
    }

    @Test
    public void testGetValuesGreaterOrEquals() {
        List<Integer> result = skipList.getValuesGreaterOrEquals(30);
        assertEquals(3, result.size(), "Expected three matches for values greater than or equal to 30");
        assertEquals(30, result.get(0), "First match should be 30");
        assertEquals(40, result.get(1), "Second match should be 40");
        assertEquals(50, result.get(2), "Third match should be 50");
    }

    @Test
    public void testGetValuesLesser() {
        List<Integer> result = skipList.getValuesLesser(30);
        assertEquals(2, result.size(), "Expected two matches for values less than 30");
        assertEquals(10, result.get(0), "First match should be 10");
        assertEquals(20, result.get(1), "Second match should be 20");
    }

    @Test
    public void testGetValuesLesserOrEquals() {
        List<Integer> result = skipList.getValuesLesserOrEquals(30);
        assertEquals(3, result.size(), "Expected three matches for values less than or equal to 30");
        assertEquals(10, result.get(0), "First match should be 10");
        assertEquals(20, result.get(1), "Second match should be 20");
        assertEquals(30, result.get(2), "Third match should be 30");
    }

    @Test
    public void testInsertAndRetrieve() {
        skipList.insert(25);
        List<Integer> result = skipList.getValuesGreaterOrEquals(20);
        assertTrue(result.contains(25), "Inserted value 25 should be present");
    }

    @Test
    public void testEmptyList() {
        SkipList<Integer> emptyList = new SkipList<>();
        List<Integer> result = emptyList.getValuesGreaterOrEquals(10);
        assertTrue(result.isEmpty(), "Expected no matches in an empty skip list");
    }
}
