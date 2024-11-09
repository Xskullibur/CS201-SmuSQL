package edu.smu.smusql;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

        static Engine dbEngine;

        @BeforeAll
        public static void setup() {
                dbEngine = new Engine();
        }

        public void createTables() {
                dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
                dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
                dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");
        }

        @Test
        public void testCreateTables() {
                assertEquals("Table users created successfully",
                                dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)"));
                assertEquals("Table products created successfully",
                                dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)"));
                assertEquals("Table orders created successfully",
                                dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)"));
        }

        @Test
        public void testInsertData() {
                createTables();
                assertEquals("1 row inserted successfully",
                                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')"));
                assertEquals("1 row inserted successfully",
                                dbEngine.executeSQL("INSERT INTO products VALUES (1, 'Laptop', 10.99, 'Electronics')"));
                assertEquals("1 row inserted successfully",
                                dbEngine.executeSQL("INSERT INTO orders VALUES (1, 1, 1, 2)"));
        }

        @Test
        public void testSelectData() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                assertEquals("id\tname\tage\tcity\n" + //
                                "2\tBob\t25\tLos Angeles", dbEngine.executeSQL("SELECT * FROM users WHERE id = 2"));
        }

        @Test
        public void testComplexSelectQuery() {
                createTables();
                dbEngine.executeSQL("INSERT INTO products VALUES (1, 'Tablet', 200.00, 'Electronics')");
                dbEngine.executeSQL("INSERT INTO products VALUES (2, 'Tablet', 20.99, 'Electronics')");
                dbEngine.executeSQL("INSERT INTO products VALUES (3, 'Tablet', 199.99, 'Electronics')");
                dbEngine.executeSQL("INSERT INTO products VALUES (4, 'Phone', 200.01, 'Electronics')");
                assertEquals("id\tname\tprice\tcategory\n" + //
                                "1\tTablet\t200.00\tElectronics\t\n" + //
                                "4\tPhone\t200.01\tElectronics",
                                dbEngine.executeSQL(
                                                "SELECT * FROM products WHERE price >= 200 AND category = 'Electronics'"));
        }

        @Test
        public void testUpdateData() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                assertEquals("1 row(s) updated successfully",
                                dbEngine.executeSQL("UPDATE users SET age = 36 WHERE id = 3"));
                assertEquals("id\tname\tage\tcity\n" + //
                                "3\tCharlie\t36\tChicago",
                                dbEngine.executeSQL("SELECT * FROM users WHERE id = 3"));
        }

        @Test
        public void testDeleteData() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
                dbEngine.executeSQL("INSERT INTO users VALUES (5, 'Eve', 28, 'San Francisco')");

                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "2\tBob\t25\tLos Angeles\t\n" + //
                                "3\tCharlie\t35\tChicago\t\n" + //
                                "4\tDavid\t40\tBoston\t\n" + //
                                "5\tEve\t28\tSan Francisco", dbEngine.executeSQL("SELECT * FROM users"));
                assertEquals("1 row(s) deleted successfully", dbEngine.executeSQL("DELETE FROM users WHERE id = 4"));
                assertEquals("id\tname\tage\tcity", dbEngine.executeSQL("SELECT * FROM users WHERE id = 4"));
                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "2\tBob\t25\tLos Angeles\t\n" + //
                                "3\tCharlie\t35\tChicago\t\n" + //
                                "5\tEve\t28\tSan Francisco", dbEngine.executeSQL("SELECT * FROM users"));
        }

        @Test
        public void testComplexDeleteQuery() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
                dbEngine.executeSQL("INSERT INTO users VALUES (5, 'Eve', 28, 'San Francisco')");

                // Execute complex DELETE query
                assertEquals("2 row(s) deleted successfully", dbEngine.executeSQL("DELETE FROM users WHERE age > 30"));

                // Verify the remaining rows
                String result = dbEngine.executeSQL("SELECT * FROM users");

                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "2\tBob\t25\tLos Angeles\t\n" + //
                                "5\tEve\t28\tSan Francisco", result);
        }

        @Test
        public void testComplexDeleteQueryWithAnd() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
                dbEngine.executeSQL("INSERT INTO users VALUES (5, 'Eve', 28, 'San Francisco')");

                // Execute complex DELETE query with AND condition
                assertEquals("1 row(s) deleted successfully",
                                dbEngine.executeSQL("DELETE FROM users WHERE age > 30 AND city = 'Chicago'"));

                // Verify the remaining rows
                String result = dbEngine.executeSQL("SELECT * FROM users");

                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "2\tBob\t25\tLos Angeles\t\n" + //
                                "4\tDavid\t40\tBoston\t\n" + //
                                "5\tEve\t28\tSan Francisco", result);
        }

        @Test
        public void testComplexDeleteQueryWithOr() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
                dbEngine.executeSQL("INSERT INTO users VALUES (5, 'Eve', 28, 'San Francisco')");
                
                String check = dbEngine.executeSQL("SELECT * FROM users where city = 'Los Angeles'");
                assertEquals("id\tname\tage\tcity\n" + //
                "2\tBob\t25\tLos Angeles", check, "one row should be returned"
                );

                // Execute complex DELETE query with OR condition
                assertEquals("3 row(s) deleted successfully",
                                dbEngine.executeSQL("DELETE FROM users WHERE age > 30 OR city = 'Los Angeles'"));

                // Verify the remaining rows
                String result = dbEngine.executeSQL("SELECT * FROM users");

                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "5\tEve\t28\tSan Francisco", result);
        }

        @Test
        public void testComplexUpdateQuery() {
                createTables();
                dbEngine.executeSQL("INSERT INTO products VALUES (4, 'TV', 20.99, 'Electronics')");
                assertEquals("1 row(s) updated successfully",
                                dbEngine.executeSQL(
                                                "UPDATE products SET price = 30.99 WHERE category = 'Electronics'"));
                assertEquals("id\tname\tprice\tcategory\n" + //
                                "4\tTV\t30.99\tElectronics",
                                dbEngine.executeSQL("SELECT * FROM products WHERE id = 4"));
        }

        @Test
        public void testRetrieveRangeOfIds() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
                dbEngine.executeSQL("INSERT INTO users VALUES (5, 'Eve', 28, 'San Francisco')");

                String result = dbEngine.executeSQL("SELECT * FROM users WHERE id > 3");

                assertEquals("id\tname\tage\tcity\n" + //
                                "4\tDavid\t40\tBoston\t\n" + //
                                "5\tEve\t28\tSan Francisco", result);
        }

        @Test
        public void testUpdateMultipleColumns() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                assertEquals("1 row(s) updated successfully",
                                dbEngine.executeSQL("UPDATE users SET age = 31, city = 'Los Angeles' WHERE id = 1"));
                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t31\tLos Angeles", dbEngine.executeSQL("SELECT * FROM users WHERE id = 1"));
        }

        @Test
        public void testUpdateWithNoMatchingRows() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                assertEquals("0 row(s) updated, not found",
                                dbEngine.executeSQL("UPDATE users SET age = 31 WHERE id = 999"));
        }

        @Test
        public void testUpdateWithComplexCondition() {
                createTables();
                dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')");
                dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
                dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
                assertEquals("2 row(s) updated successfully",
                                dbEngine.executeSQL("UPDATE users SET age = 40 WHERE age < 30 OR city = 'Chicago'"));
                assertEquals("id\tname\tage\tcity\n" + //
                                "1\tAlice\t30\tNew York\t\n" + //
                                "2\tBob\t40\tLos Angeles\t\n" + //
                                "3\tCharlie\t40\tChicago", dbEngine.executeSQL("SELECT * FROM users"));
        }
}