package edu.smu.smusql;

public interface IEngine {

    public String executeSQL(String query);

    public void clearDatabase();
}
