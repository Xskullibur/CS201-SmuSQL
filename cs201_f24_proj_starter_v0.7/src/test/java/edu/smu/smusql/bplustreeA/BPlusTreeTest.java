package edu.smu.smusql.bplustreeA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BPlusTreeTest {
    private static BPlusTree<Integer, String> tree;
    private static final int ORDER = 4; // This will create a 4-order B+ tree

    @BeforeAll
    public static void setUp() {
        tree = new BPlusTree<>(ORDER);
    }

    @Test
    public void testBasicRemoval() {
        // Insert some values
        tree.insert(5, "five");
        tree.insert(10, "ten");
        tree.insert(15, "fifteen");

        // Remove middle value
        tree.removeKey(10);

        // Verify size and remaining values
        assertEquals(2, tree.getSize());
        assertNull(tree.search(10));
        assertEquals("five", tree.search(5).get(0));
        assertEquals("fifteen", tree.search(15).get(0));
    }

    @Test
    public void testRemovalCausingUnderflow() {
        // Insert enough values to create multiple nodes
        int[] keys = { 5, 10, 15, 20, 25, 30 };
        for (int key : keys) {
            tree.insert(key, "value" + key);
        }

        // Remove values to cause underflow
        tree.removeKey(5);
        tree.removeKey(10);

        // Verify remaining structure
        assertEquals(4, tree.getSize());
        assertNull(tree.search(5));
        assertNull(tree.search(10));
        assertNotNull(tree.search(15));
    }

    @Test
    public void testRemovalRequiringRebalancing() {
        // Insert values to create a tree requiring rebalancing
        int[] keys = { 10, 20, 30, 40, 50, 60, 70, 80 };
        for (int key : keys) {
            tree.insert(key, "value" + key);
        }

        // Remove values to force rebalancing
        tree.removeKey(20);
        tree.removeKey(30);
        tree.removeKey(40);

        // Verify tree integrity
        assertEquals(5, tree.getSize());
        List<Integer> allKeys = tree.getAllKeys();
        assertTrue(isSorted(allKeys));
    }

    @Test
    public void testRemovalFromRoot() {
        // Insert only enough values to stay in root
        tree.insert(1, "one");
        tree.insert(2, "two");
        tree.insert(3, "three");

        // Remove from root
        tree.removeKey(2);

        // Verify root structure
        assertEquals(2, tree.getSize());
        assertNull(tree.search(2));
        assertEquals("one", tree.search(1).get(0));
        assertEquals("three", tree.search(3).get(0));
    }

    @Test
    public void testRemovalRequiringMerge() {
        // Insert values that will require merging nodes
        int[] keys = { 10, 20, 30, 40, 50, 60, 70, 80, 90 };
        for (int key : keys) {
            tree.insert(key, "value" + key);
        }

        // Remove values to force node merging
        tree.removeKey(20);
        tree.removeKey(30);
        tree.removeKey(40);
        tree.removeKey(50);

        // Verify tree structure
        assertEquals(5, tree.getSize());
        List<Integer> allKeys = tree.getAllKeys();
        assertTrue(isSorted(allKeys));
    }

    @Test
    public void testRemovalOfAllElements() {
        // Insert some values
        int[] keys = { 1, 2, 3, 4, 5 };
        for (int key : keys) {
            tree.insert(key, "value" + key);
        }

        // Remove all values
        for (int key : keys) {
            tree.removeKey(key);
        }

        // Verify empty tree
        assertEquals(0, tree.getSize());
        assertTrue(tree.getAllKeys().isEmpty());
    }

    @Test
    public void testStressTest() {
        // Insert many values
        for (int i = 1; i <= 100000; i++) {
            tree.insert(i, "value" + i);
        }

        // Remove half of them
        for (int i = 1; i <= 100000; i++) {
            System.out.println("Removing key: " + i);
            tree.removeKey(i);
        }

        // Verify tree integrity
        assertEquals(0, tree.getSize());
        List<Integer> allKeys = tree.getAllKeys();
        assertTrue(isSorted(allKeys));
        assertEquals(0, allKeys.size());
    }

    // Helper method to check if a list is sorted
    private boolean isSorted(List<Integer> list) {
        if (list.size() <= 1)
            return true;
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1) >= list.get(i))
                return false;
        }
        return true;
    }

    @Test
    public void testRandomizedRemoval() {
        // Insert values in random order
        Integer[] keys = { 15, 7, 23, 4, 30, 11, 19, 27, 8, 16 };
        for (Integer key : keys) {
            tree.insert(key, "value" + key);
        }

        // Remove values in different order
        Integer[] removalOrder = { 7, 23, 11, 27, 15 };
        for (Integer key : removalOrder) {
            tree.removeKey(key);
        }

        // Verify remaining structure
        assertEquals(5, tree.getSize());
        List<Integer> remainingKeys = tree.getAllKeys();
        assertTrue(isSorted(remainingKeys));
        assertEquals(5, remainingKeys.size());
    }

    @Test
    public void testRemoveValue() {
        BPlusTree<Integer, Integer> indexTree = new BPlusTree<>(4);

        // Insert multiple values for the same key (simulating an index tree)
        indexTree.insert(1, 100); // key 1 represents an indexed field, 100 is a primary key
        indexTree.insert(1, 101);
        indexTree.insert(1, 102);
        indexTree.insert(2, 103);
        indexTree.insert(2, 104);

        // Remove specific values
        indexTree.removeValue(1, 101); // Remove primary key 101 from index key 1

        // Verify the value was removed but the key and other values remain
        List<Integer> valuesForKey1 = indexTree.search(1);
        assertNotNull(valuesForKey1);
        assertEquals(2, valuesForKey1.size());
        assertTrue(valuesForKey1.contains(100));
        assertTrue(valuesForKey1.contains(102));
        assertFalse(valuesForKey1.contains(101));

        // Remove all values for a key
        indexTree.removeValue(2, 103);
        indexTree.removeValue(2, 104);

        // Verify the key was removed when all values were removed
        List<Integer> valuesForKey2 = indexTree.search(2);
        assertTrue(valuesForKey2 == null || valuesForKey2.isEmpty());

        // Verify tree size
        List<Integer> allKeys = indexTree.getAllKeys();
        assertEquals(1, allKeys.size());
        assertTrue(allKeys.contains(1));
    }

    @Test
    public void testUpdate() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4);

        // Test main tree update (single value per key)
        tree.insert(1, "value1");
        tree.update(1, "newValue1");

        List<String> result = tree.search(1);
        assertEquals(1, result.size());
        assertEquals("newValue1", result.get(0));

        // Test updating non-existent key
        assertThrows(IllegalArgumentException.class, () -> tree.update(999, "value"));
    }

    @Test
    public void testUpdateValue() {
        BPlusTree<Integer, Integer> indexTree = new BPlusTree<>(4);

        // Test index tree update (multiple values per key)
        indexTree.insert(1, 100); // key 1 represents indexed field, 100 is primary key
        indexTree.insert(1, 101);
        indexTree.insert(1, 102);

        // Update specific value
        indexTree.updateValue(1, 101, 201);

        List<Integer> values = indexTree.search(1);
        assertEquals(3, values.size());
        assertTrue(values.contains(100));
        assertTrue(values.contains(201));
        assertTrue(values.contains(102));
        assertFalse(values.contains(101));

        // Test updating non-existent value
        assertThrows(IllegalArgumentException.class,
                () -> indexTree.updateValue(1, 999, 888));
    }

    @Test
    public void testUpdateKey() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4);

        // Insert multiple values for a key
        tree.insert(1, "value1");
        tree.insert(1, "value2");

        // Update key
        tree.updateKey(1, 2);

        // Verify old key is removed
        List<String> oldValues = tree.search(1);
        assertTrue(oldValues == null || oldValues.isEmpty());

        // Verify values moved to new key
        List<String> newValues = tree.search(2);
        assertEquals(2, newValues.size());
        assertTrue(newValues.contains("value1"));
        assertTrue(newValues.contains("value2"));

        // Test updating to existing key
        tree.insert(3, "value3");
        assertThrows(IllegalArgumentException.class,
                () -> tree.updateKey(2, 3));
    }

}