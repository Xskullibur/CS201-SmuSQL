package edu.smu.smusql;

import edu.smu.smusql.bplustreeA.BPlusTreeEngine;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    static BPlusTreeEngine dbEngine;

    @BeforeAll
    public static void setup() {
        dbEngine = new BPlusTreeEngine();
    }

    public void createTables() {
        dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
        dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
        dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");
    }

    @Test
    public void testCreateTables() {
        assertEquals("Table users created successfully", dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)"));
        assertEquals("Table products created successfully", dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)"));
        assertEquals("Table orders created successfully", dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)"));
    }

    @Test
    public void testInsertData() {
        createTables();
        assertEquals("1 row inserted successfully", dbEngine.executeSQL("INSERT INTO users VALUES (1, 'Alice', 30, 'New York')"));
        assertEquals("1 row inserted successfully", dbEngine.executeSQL("INSERT INTO products VALUES (1, 'Laptop', 10.99, 'Electronics')"));
        assertEquals("1 row inserted successfully", dbEngine.executeSQL("INSERT INTO orders VALUES (1, 1, 1, 2)"));
    }

    @Test
    public void testSelectData() {
        createTables();
        dbEngine.executeSQL("INSERT INTO users VALUES (2, 'Bob', 25, 'Los Angeles')");
        assertEquals("name\tage\tcity\nBob\t25\tLos Angeles", dbEngine.executeSQL("SELECT * FROM users WHERE id = 2"));
    }

    @Test
    public void testUpdateData() {
        dbEngine.executeSQL("INSERT INTO users VALUES (3, 'Charlie', 35, 'Chicago')");
        assertEquals("Record updated successfully.", dbEngine.executeSQL("UPDATE users SET age = 36 WHERE id = 3"));
        assertEquals("[{id=3, name=Charlie, age=36, city=Chicago}]", dbEngine.executeSQL("SELECT * FROM users WHERE id = 3"));
    }

    @Test
    public void testDeleteData() {
        dbEngine.executeSQL("INSERT INTO users VALUES (4, 'David', 40, 'Boston')");
        assertEquals("Record deleted successfully.", dbEngine.executeSQL("DELETE FROM users WHERE id = 4"));
        assertEquals("[]", dbEngine.executeSQL("SELECT * FROM users WHERE id = 4"));
    }

    @Test
    public void testComplexSelectQuery() {
        createTables();
        dbEngine.executeSQL("INSERT INTO products VALUES (4, 'Tablet', 200.00, 'Electronics')");
        dbEngine.executeSQL("INSERT INTO products VALUES (3, 'Tablet', 20.99, 'Electronics')");
        dbEngine.executeSQL("INSERT INTO products VALUES (3, 'Tablet', 199.99, 'Electronics')");
        dbEngine.executeSQL("INSERT INTO products VALUES (2, 'Phone', 200.01, 'Electronics')");
        assertEquals("name\tprice\tcategory\nPhone\t200.01\tElectronics\t\nTablet\t200.0\tElectronics",
                dbEngine.executeSQL("SELECT * FROM products WHERE price >= 200 AND category = 'Electronics'"));
    }

    @Test
    public void testComplexUpdateQuery() {
        dbEngine.executeSQL("INSERT INTO products VALUES (4, 'TV', 20.99, 'Electronics')");
        assertEquals("Record updated successfully.", dbEngine.executeSQL("UPDATE products SET price = 30.99 WHERE category = 'Electronics'"));
        assertEquals("[{id=4, name=TV, price=749.99, category=Electronics}]",
                dbEngine.executeSQL("SELECT * FROM products WHERE id = 4"));
    }
}