package edu.smu.smusql.parser.nodes;

import java.util.List;

// Node for CREATE statements
public class CreateTableNode extends ASTNode {
    String tableName;
    List<String> columns;

    public CreateTableNode(String tableName, List<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

}