package edu.smu.smusql.skipHash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class SkipListTest {

    private SkipList<Indexing> skipList;
    private final String STRING_INT_MAX = Integer.toString(Integer.MAX_VALUE);
    
    @BeforeEach
    public void setUp() {
        skipList = new SkipList<>();
        // Populate skip list with some test data
        skipList.insert(new Indexing("10", "1"));
        skipList.insert(new Indexing("20", "2"));
        skipList.insert(new Indexing("Los Angeles", "3"));
        skipList.insert(new Indexing("40", "4"));
        skipList.insert(new Indexing("50", "5"));
    }

    @Test
    public void testGetValuesEqual() {
        List<Indexing> result = skipList.getValuesEqual(new Indexing("Los Angeles", "-123143"));
        assertEquals(1, result.size(), "Expected one match for value 30");
        assertEquals("Los Angeles", result.get(0).getColumnValue(), "Expected match for value 30");
    }

    @Test
    public void testGetValuesGreater() {
        List<Indexing> result = skipList.getValuesGreater(new Indexing("30", STRING_INT_MAX));
        assertEquals(2, result.size(), "Expected two matches for values greater than 30");
        assertEquals("40", result.get(0).getColumnValue(), "First match should be 40");
        assertEquals("50", result.get(1).getColumnValue(), "Second match should be 50");
    }

    @Test
    public void testGetValuesLesserOrEquals() {
        List<Indexing> result = skipList.getValuesLesserOrEquals(new Indexing("30", STRING_INT_MAX));
        assertEquals(3, result.size(), "Expected three matches for values less than or equal to 30");
        assertEquals("10", result.get(0).getColumnValue(), "First match should be 10");
        assertEquals("20", result.get(1).getColumnValue(), "Second match should be 20");
        assertEquals("30", result.get(2).getColumnValue(), "Third match should be 30");
    }

    @Test
    public void testGetValuesGreaterOrEquals() {
        List<Indexing> result = skipList.getValuesGreaterOrEquals(new Indexing("30", "-2134123"));
        assertEquals(3, result.size(), "Expected three matches for values greater than or equal to 30");
        assertEquals("30", result.get(0).getColumnValue(), "First match should be 30");
        assertEquals("40", result.get(1).getColumnValue(), "Second match should be 40");
    }

    @Test
    public void testGetValuesLesser() {
        List<Indexing> result = skipList.getValuesLesser(new Indexing("30", "-1234213"));
        assertEquals(2, result.size(), "Expected two matches for values less than 30");
        assertEquals("10", result.get(0).getColumnValue(), "First match should be 10");
        assertEquals("20", result.get(1).getColumnValue(), "Second match should be 20");
    }

   
}
