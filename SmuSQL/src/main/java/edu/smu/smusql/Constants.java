package edu.smu.smusql;

public class Constants {
    
    public static final boolean LOGGING = true;
    public static final int B_PLUS_TREE_ORDER = 32;
    public static final int CACHE_SIZE = 10000;

    public static String getIndexTableName(String tableName, String column) {
        return "idx_" + tableName + "_" + column ;
    }

}
