package edu.smu.smusql.analysis.utils;

import edu.smu.smusql.IEngine;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Base class for all benchmark tests
 */
public abstract class AbstractBenchmark {

    protected static final int REPORTING_INTERVAL = 1000;
    protected final IEngine engine;
    protected final Random random;
    protected final String outputFile;
    protected final MemoryMXBean memoryBean;
    protected final UniqueIdManager idManager;

    protected AbstractBenchmark(IEngine engine, long seed, String outputDir, String fileName) {
        this.engine = engine;
        this.random = new Random(seed);
        this.outputFile = outputDir + File.separator + fileName;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.idManager = new UniqueIdManager();
        initializeCSV();
    }

    protected void initializeCSV() {
        File directory = new File(outputFile).getParentFile();
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create results directory: " + directory);
            }
        }

        try (FileWriter fw = new FileWriter(outputFile)) {
            fw.write(getCSVHeader());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV file", e);
        }
    }

    protected abstract String getCSVHeader();

    protected void setupTables() {
        engine.executeSQL("CREATE TABLE users (id, name, age, city)");
        engine.executeSQL("CREATE TABLE products (id, name, price, category)");
        engine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");
        prepopulateTables();
    }

    protected void prepopulateTables() {
        // Add initial IDs to tracking
        idManager.addInitialIds(100);

        // Prepopulate users
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            String name = "User" + id;
            int age = 20 + random.nextInt(41);
            String city = DataGenerator.getRandomCity(random);
            String sql = String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')",
                id, name, age, city);
            engine.executeSQL(sql);
        }

        // Prepopulate products
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            String name = "Product" + id;
            double price = 50 + (random.nextDouble() * 950);
            String category = DataGenerator.getRandomCategory(random);
            String sql = String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')",
                id, name, price, category);
            engine.executeSQL(sql);
        }

        // Prepopulate orders
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            int userId = random.nextInt(100) + 1;
            int productId = random.nextInt(100) + 1;
            int quantity = random.nextInt(10) + 1;
            String sql = String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)",
                id, userId, productId, quantity);
            engine.executeSQL(sql);
        }
    }

    protected void cleanup() {
        engine.clearDatabase();
        performGC();
    }

    protected void performGC() {
        System.gc();
        System.runFinalization();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Common database operations
    protected void executeInsert() {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0 -> insertUser();
            case 1 -> insertProduct();
            case 2 -> insertOrder();
        }
    }

    protected void insertUser() {
        int id = idManager.generateUniqueUserId(random);
        String name = "User" + id;
        int age = random.nextInt(60) + 20;
        String city = DataGenerator.getRandomCity(random);
        String sql = String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')",
            id, name, age, city);
        engine.executeSQL(sql);
    }

    protected void insertProduct() {
        int id = idManager.generateUniqueProductId(random);
        String name = "Product" + id;
        double price = 50 + (random.nextDouble() * 1000);
        String category = DataGenerator.getRandomCategory(random);
        String sql = String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')",
            id, name, price, category);
        engine.executeSQL(sql);
    }

    protected void insertOrder() {
        int id = idManager.generateUniqueOrderId(random);
        Set<Integer> userIds = idManager.getExistingUserIds();
        Set<Integer> productIds = idManager.getExistingProductIds();

        List<Integer> userIdList = new ArrayList<>(userIds);
        List<Integer> productIdList = new ArrayList<>(productIds);

        int userId = userIdList.get(random.nextInt(userIdList.size()));
        int productId = productIdList.get(random.nextInt(productIdList.size()));
        int quantity = random.nextInt(10) + 1;

        String sql = String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)",
            id, userId, productId, quantity);
        engine.executeSQL(sql);
    }

    protected void executeRangeSelect() {
        if (random.nextBoolean()) {
            int minAge = random.nextInt(20) + 20;
            int maxAge = minAge + random.nextInt(30);
            engine.executeSQL(
                String.format("SELECT * FROM users WHERE age >= %d AND age <= %d",
                    minAge, maxAge));
        } else {
            double minPrice = 50 + (random.nextDouble() * 200);
            double maxPrice = minPrice + random.nextDouble() * 500;
            engine.executeSQL(
                String.format("SELECT * FROM products WHERE price >= %.2f AND price <= %.2f",
                    minPrice, maxPrice));
        }
    }


    protected void executeSimpleSelect() {
        String[] tables = {"users", "products", "orders"};
        String table = tables[random.nextInt(tables.length)];
        engine.executeSQL("SELECT * FROM " + table);
    }

    protected void executeUpdate() {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0:
                updateUser();
                break;
            case 1:
                updateProduct();
                break;
            case 2:
                updateOrder();
                break;
        }
    }


    private void updateUser() {
        int id = random.nextInt(10000) + 1;
        int newAge = random.nextInt(60) + 20;
        engine.executeSQL(String.format("UPDATE users SET age = %d WHERE id = %d", newAge, id));
    }

    private void updateProduct() {
        int id = random.nextInt(1000) + 1;
        double newPrice = 50 + (random.nextDouble() * 1000);
        engine.executeSQL(
            String.format("UPDATE products SET price = %.2f WHERE id = %d", newPrice, id));
    }

    private void updateOrder() {
        int id = random.nextInt(10000) + 1;
        int newQuantity = random.nextInt(10) + 1;
        engine.executeSQL(
            String.format("UPDATE orders SET quantity = %d WHERE id = %d", newQuantity, id));
    }

    protected void executeEqualsSelect() {
        if (random.nextBoolean()) {
            String city = DataGenerator.getRandomCity(random);
            engine.executeSQL(String.format("SELECT * FROM users WHERE city = '%s'", city));
        } else {
            String category = DataGenerator.getRandomCategory(random);
            engine.executeSQL(
                String.format("SELECT * FROM products WHERE category = '%s'", category));
        }
    }

    protected void executeRangeUpdate() {
        if (random.nextBoolean()) {
            int minAge = random.nextInt(20) + 20;
            int maxAge = minAge + random.nextInt(30);
            int newAge = random.nextInt(60) + 20;
            engine.executeSQL(
                String.format("UPDATE users SET age = %d WHERE age >= %d AND age <= %d",
                    newAge, minAge, maxAge));
        } else {
            double minPrice = 50 + (random.nextDouble() * 200);
            double maxPrice = minPrice + random.nextDouble() * 500;
            double newPrice = 50 + (random.nextDouble() * 1000);
            engine.executeSQL(
                String.format(
                    "UPDATE products SET price = %.2f WHERE price >= %.2f AND price <= %.2f",
                    newPrice, minPrice, maxPrice));
        }
    }

    protected void executeEqualsUpdate() {
        if (random.nextBoolean()) {
            String city = DataGenerator.getRandomCity(random);
            int newAge = random.nextInt(60) + 20;
            engine.executeSQL(
                String.format("UPDATE users SET age = %d WHERE city = '%s'", newAge, city));
        } else {
            String category = DataGenerator.getRandomCategory(random);
            double newPrice = 50 + (random.nextDouble() * 1000);
            engine.executeSQL(
                String.format("UPDATE products SET price = %.2f WHERE category = '%s'",
                    newPrice, category));
        }
    }

    protected void executeDelete() {
        String[] tables = {"users", "products", "orders"};
        String table = tables[random.nextInt(tables.length)];
        int id = random.nextInt(10000) + 1;
        engine.executeSQL(String.format("DELETE FROM %s WHERE id = %d", table, id));
    }
}
