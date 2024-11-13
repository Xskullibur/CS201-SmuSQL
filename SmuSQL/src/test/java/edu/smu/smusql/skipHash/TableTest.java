package edu.smu.smusql.skipHash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.HashMap;
import java.util.Set;

public class TableTest {

    private Table table;
    private SkipList<Indexing> skipList;

    @BeforeEach
    public void setup() {
        table = new Table();
        skipList = new SkipList<>();

        // Mock adding data to the secondary index
        skipList.insert(new Indexing("100", "key1"));
        skipList.insert(new Indexing("200", "key2"));
        skipList.insert(new Indexing("150", "key3"));
        skipList.insert(new Indexing("100", "key4"));
        skipList.insert(new Indexing("250", "key5"));

        // Adding the skip list to the secondary indices map
        HashMap<String, SkipList<Indexing>> secondaryIndices = new HashMap<>();
        secondaryIndices.put("price", skipList);
        table.setSecondaryIndices(secondaryIndices);
    }

    @Test
    public void testReturnKeysByGreaterThan() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", ">", "150");
        assertEquals(List.of("key2", "key5"), result, "Expected keys greater than 150");
    }

    @Test
    public void testReturnKeysByLessThan() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "<", "150");
        assertEquals(Set.of("key1", "key4"), (Set.of(result.toArray())), "Expected keys less than 150");
    }

    @Test
    public void testReturnKeysByEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "=", "100");
        assertEquals(Set.of("key1", "key4"), (Set.of(result.toArray())), "Expected keys equal to 100");
    }

    @Test
    public void testReturnKeysByNotEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "!=", "150");
        assertEquals(Set.of("key1", "key2", "key4", "key5"),  (Set.of(result.toArray())), "Expected keys not equal to 150");
    }

    @Test
    public void testReturnKeysByGreaterThanOrEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", ">=", "150");
        assertTrue(Set.of("key2", "key3", "key5").equals(Set.of(result.toArray())),
                "Expected keys greater than or equal to 150");
    }

    @Test
    public void testReturnKeysByLessThanOrEquals() {
        List<String> result = table.returnKeysByRequirementsOnIndex("price", "<=", "150");
        assertEquals(Set.of("key1", "key3", "key4"), Set.of(result.toArray()), "Expected keys less than or equal to 150");
    }
}
