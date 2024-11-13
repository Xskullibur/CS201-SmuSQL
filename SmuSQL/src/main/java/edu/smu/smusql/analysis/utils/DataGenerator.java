package edu.smu.smusql.analysis.utils;

import java.util.Random;

class DataGenerator {
    private static final String[] CITIES = {
        "New York", "Los Angeles", "Chicago", "Boston", "Miami",
        "Seattle", "Austin", "Dallas", "Atlanta", "Denver"
    };

    private static final String[] CATEGORIES = {
        "Electronics", "Appliances", "Clothing", "Furniture", "Toys",
        "Sports", "Books", "Beauty", "Garden"
    };

    public static String getRandomCity(Random random) {
        return CITIES[random.nextInt(CITIES.length)];
    }

    public static String getRandomCategory(Random random) {
        return CATEGORIES[random.nextInt(CATEGORIES.length)];
    }
}