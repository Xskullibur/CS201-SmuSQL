package edu.smu.smusql.skipHash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.smu.smusql.IEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkipHashEngineTest {

    private static IEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new SkipHashEngine();
    }

    @AfterEach
    public void cleanup() {
        engine.clearDatabase();
    }

    // CREATE TABLE TESTS
    @Test
    void testCreateTable() {
        assertEquals("Table student created successfully",
                engine.executeSQL("CREATE TABLE student (id, name, age, gpa, deans_list)"));
    }

    // @Test
    // void testCreateDuplicateTable() {
    // engine.executeSQL("CREATE TABLE student (id, name, age)");
    // assertThrows(RuntimeException.class, () -> engine.executeSQL("CREATE TABLE
    // student (id, name, age)"));
    // }

    // INSERT TESTS
    @Test
    void testBasicInsert() {
        engine.executeSQL("CREATE TABLE student (id, name, age, gpa, deans_list)");
        assertEquals("1 row inserted successfully",
                engine.executeSQL("INSERT INTO student VALUES (1, 'John', 20, 3.5, 'True')"));
    }

    @Test
    void testInsertDuplicatePrimaryKey() {
        engine.executeSQL("CREATE TABLE student (id, name, age, gpa, deans_list)");
        engine.executeSQL("INSERT INTO student VALUES (1, 'John', 20, 3.5, 'True')");
        assertEquals("0 row inserted, primary key already exists",
                engine.executeSQL("INSERT INTO student VALUES (1, 'Jane', 22, 3.8, 'True')"));
    }

    // @Test
    // void testInsertInvalidColumnCount() {
    // engine.executeSQL("CREATE TABLE student (id, name, age)");
    // assertThrows(RuntimeException.class, () -> engine.executeSQL("INSERT INTO
    // student VALUES (1, 'John')"));
    // }

    // SELECT TESTS
    @Test
    void testSelectAll() {
        setupStudentTable();
        String expected = "id\tname\tage\tgpa\tdeans_list\n" +
                "1\tJohn\t20\t3.5\tTrue\t\n" +
                "2\tJane\t22\t3.8\tTrue\t\n" +
                "3\tBob\t19\t2.5\tFalse";
        assertEquals(expected, engine.executeSQL("SELECT * FROM student"));
    }

    // Helper method to set up test data
    private void setupStudentTable() {
        engine.executeSQL("CREATE TABLE student (id, name, age, gpa, deans_list)");
        engine.executeSQL("INSERT INTO student VALUES (1, 'John', 20, 3.5, 'True')");
        engine.executeSQL("INSERT INTO student VALUES (2, 'Jane', 22, 3.8, 'True')");
        engine.executeSQL("INSERT INTO student VALUES (3, 'Bob', 19, 2.5, 'False')");
    }

    @Test
    void testSelectWithEquals() {
        setupStudentTable();
        String expected = "id\tname\tage\tgpa\tdeans_list\n" +
                "2\tJane\t22\t3.8\tTrue";
        assertEquals(expected, engine.executeSQL("SELECT * FROM student WHERE id = 2"));
    }

    @Test
    void testSelectWithRange() {
        setupStudentTable();
        String expected = "id\tname\tage\tgpa\tdeans_list\n" +
                "2\tJane\t22\t3.8\tTrue";
        assertEquals(expected, engine.executeSQL("SELECT * FROM student WHERE gpa >= 3.8"));
    }

    @Test
    void testSelectWithComplexAnd() {
        setupStudentTable();
        String expected = "id\tname\tage\tgpa\tdeans_list\n" +
                "2\tJane\t22\t3.8\tTrue";
        assertEquals(expected,
                engine.executeSQL("SELECT * FROM student WHERE gpa >= 3.5 AND age > 20"));
    }

    @Test
    void testSelectWithComplexOr() {
        setupStudentTable();
        String expected = "id\tname\tage\tgpa\tdeans_list\n" +
                "1\tJohn\t20\t3.5\tTrue\t\n" +
                "2\tJane\t22\t3.8\tTrue\t\n" +
                "3\tBob\t19\t2.5\tFalse";
        assertEquals(expected,
                engine.executeSQL("SELECT * FROM student WHERE gpa >= 3.5 OR age < 20"));
    }

    @Test
    void testSelectNoResults() {
        setupStudentTable();
        assertEquals("id\tname\tage\tgpa\tdeans_list",
                engine.executeSQL("SELECT * FROM student WHERE gpa > 4.0"));
    }

    // UPDATE TESTS
    @Test
    void testBasicUpdate() {
        setupStudentTable();
        assertEquals("1 row(s) updated successfully",
                engine.executeSQL("UPDATE student SET age = 21 WHERE id = 1"));

        String result = engine.executeSQL("SELECT * FROM student WHERE id = 1");
        assertTrue(result.contains("21"));
    }

    @Test
    void testUpdateMultipleRows() {
        setupStudentTable();
        assertEquals("2 row(s) updated successfully",
                engine.executeSQL("UPDATE student SET deans_list = 'False' WHERE gpa < 3.8"));
    }

    @Test
    void testUpdateMultipleColumns() {
        setupStudentTable();
        assertEquals("1 row(s) updated successfully",
                engine.executeSQL("UPDATE student SET age = 23, gpa = 3.9 WHERE id = 2"));

        String result = engine.executeSQL("SELECT * FROM student WHERE id = 2");
        assertTrue(result.contains("23") && result.contains("3.9"));
    }

    @Test
    void testUpdateWithComplexCondition() {
        setupStudentTable();
        assertEquals("2 row(s) updated successfully",
                engine.executeSQL(
                        "UPDATE student SET deans_list = 'True' WHERE gpa >= 3.0 AND age >= 20"));
    }

    @Test
    void testUpdateNoMatches() {
        setupStudentTable();
        assertEquals("0 row(s) updated, not found",
                engine.executeSQL("UPDATE student SET age = 25 WHERE id = 99"));
    }

    // DELETE TESTS
    @Test
    void testBasicDelete() {
        setupStudentTable();
        assertEquals("1 row(s) deleted successfully",
                engine.executeSQL("DELETE FROM student WHERE id = 1"));
    }

    @Test
    void testDeleteMultipleRows() {
        setupStudentTable();
        assertEquals("2 row(s) deleted successfully",
                engine.executeSQL("DELETE FROM student WHERE gpa <= 3.5"));
    }

    @Test
    void testDeleteWithComplexCondition() {
        setupStudentTable();
        assertEquals("3 row(s) deleted successfully",
                engine.executeSQL("DELETE FROM student WHERE gpa >= 3.5 OR age < 20"));
    }

    @Test
    void testDeleteNoMatches() {
        setupStudentTable();
        assertEquals("0 row(s) deleted, not found",
                engine.executeSQL("DELETE FROM student WHERE id = 99"));
    }

    @Test
    void testDeleteAllRows() {
        setupStudentTable();
        assertEquals("3 row(s) deleted successfully",
                engine.executeSQL("DELETE FROM student WHERE id >= 0"));
        assertEquals("id\tname\tage\tgpa\tdeans_list",
                engine.executeSQL("SELECT * FROM student"));
    }

    // BOUNDARY AND EDGE CASES
    @Test
    void testLargeDataset() {
        engine.executeSQL("CREATE TABLE large (id, value)");
        // Insert 1000 rows
        for (int i = 1; i <= 1000; i++) {
            engine.executeSQL(String.format("INSERT INTO large VALUES (%d, %d)", i, i * 10));
        }

        // Test range query
        String result = engine.executeSQL("SELECT * FROM large WHERE value >= 9900");
        assertEquals(11, result.split("\n").length - 1); // -1 for header row
    }

    @Test
    void testBoundaryValues() {
        engine.executeSQL("CREATE TABLE boundary (id, value)");
        engine.executeSQL("INSERT INTO boundary VALUES (1, -2147483648)"); // Integer.MIN_VALUE
        engine.executeSQL("INSERT INTO boundary VALUES (2, 2147483647)"); // Integer.MAX_VALUE
        engine.executeSQL("INSERT INTO boundary VALUES (3, 0)");

        String result = engine.executeSQL("SELECT * FROM boundary WHERE value <= 0");
        assertEquals(2, result.split("\n").length - 1); // -1 for header row
    }

    @Test
    void testComplexSelectWithNullHandling() {
        engine.executeSQL("CREATE TABLE test (id, name, age, salary)");
        engine.executeSQL("INSERT INTO test VALUES (1, 'John', 30, 50000)");
        engine.executeSQL("INSERT INTO test VALUES (2, 'Jane', 25, 60000)");
        engine.executeSQL("INSERT INTO test VALUES (3, 'Bob', 35, 45000)");

        // Test complex conditions
        String result1 = engine.executeSQL("SELECT * FROM test WHERE age > 25 AND salary >= 50000");
        String result2 = engine.executeSQL("SELECT * FROM test WHERE age < 30 OR salary > 55000");
        String result3 = engine.executeSQL("SELECT * FROM test WHERE age != 30 AND salary <= 60000");

        // Verify results
        assertEquals("id\tname\tage\tsalary\n" +
                "1\tJohn\t30\t50000", result1);
        assertEquals("id\tname\tage\tsalary\n" +
                "2\tJane\t25\t60000", result2);
        assertEquals("id\tname\tage\tsalary\n" +
                "2\tJane\t25\t60000\t\n" +
                "3\tBob\t35\t45000", result3);
    }
}