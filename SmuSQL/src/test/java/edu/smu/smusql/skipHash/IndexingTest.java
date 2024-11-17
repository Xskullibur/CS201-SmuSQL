package edu.smu.smusql.skipHash;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IndexingTest {

    @Test
    public void testCompareTo_NumericValues() {
        Indexing index1 = new Indexing(100, "1", DataType.INTEGER);
        Indexing index2 = new Indexing(200, "2", DataType.INTEGER);

        assertTrue(index1.compareTo(index2) < 0, "Expected index1 to be less than index2");
        assertTrue(index2.compareTo(index1) > 0, "Expected index2 to be greater than index1");
    }

    @Test
    public void testCompareTo_EqualValuesWithSameIntegerPrimaryKey() {
        Indexing index1 = new Indexing(100, "1", DataType.INTEGER);
        Indexing index2 = new Indexing(100, "1", DataType.INTEGER);

        assertEquals(0, index1.compareTo(index2), "Expected index1 to be equal to index2");
    }

    @Test
    public void testCompareTo_DifferentNumericValuesWithSamePrimaryKey() {
        Indexing index1 = new Indexing(100, "1", DataType.INTEGER);
        Indexing index2 = new Indexing(200, "1", DataType.INTEGER);

        assertTrue(index1.compareTo(index2) < 0, "Expected index1 to be less than index2");
        assertTrue(index2.compareTo(index1) > 0, "Expected index2 to be greater than index1");
    }



}
