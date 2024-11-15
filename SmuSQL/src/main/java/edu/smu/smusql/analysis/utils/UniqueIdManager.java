package edu.smu.smusql.analysis.utils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UniqueIdManager {

    private static final int USER_ID_MIN = 1000;
    private static final int USER_ID_MAX = Integer.MAX_VALUE;
    private static final int PRODUCT_ID_MIN = 1000;
    private static final int PRODUCT_ID_MAX = Integer.MAX_VALUE;
    private static final int ORDER_ID_MIN = 1000;
    private static final int ORDER_ID_MAX = Integer.MAX_VALUE;

    private final Set<Integer> usedUserIds = new HashSet<>();
    private final Set<Integer> usedProductIds = new HashSet<>();
    private final Set<Integer> usedOrderIds = new HashSet<>();

    public synchronized int generateUniqueUserId(Random random) {
        return generateUniqueId(usedUserIds, USER_ID_MIN, USER_ID_MAX, random);
    }

    private int generateUniqueId(Set<Integer> usedIds, int min, int max, Random random) {

        if (usedUserIds.size() >= (USER_ID_MAX - USER_ID_MIN)) {
            throw new RuntimeException("ID space exhausted");
        }

        while (true) {
            int id = min + random.nextInt(max - min + 1);
            if (usedIds.add(id)) {
                return id;
            }
        }
    }

    public synchronized int generateUniqueProductId(Random random) {
        return generateUniqueId(usedProductIds, PRODUCT_ID_MIN, PRODUCT_ID_MAX, random);
    }

    public synchronized int generateUniqueOrderId(Random random) {
        return generateUniqueId(usedOrderIds, ORDER_ID_MIN, ORDER_ID_MAX, random);
    }

    public void addInitialIds(int count) {
        for (int i = 1; i <= count; i++) {
            usedUserIds.add(i);
            usedProductIds.add(i);
            usedOrderIds.add(i);
        }
    }

    public Set<Integer> getExistingUserIds() {
        return new HashSet<>(usedUserIds);
    }

    public Set<Integer> getExistingProductIds() {
        return new HashSet<>(usedProductIds);
    }

    public synchronized void clearUsedIds() {
        usedUserIds.clear();
        usedProductIds.clear();
        usedOrderIds.clear();
    }
}