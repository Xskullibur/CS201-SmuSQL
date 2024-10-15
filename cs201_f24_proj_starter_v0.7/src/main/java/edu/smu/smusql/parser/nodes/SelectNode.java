package edu.smu.smusql.parser.nodes;

import java.util.List;

// Node for SELECT statements
public class SelectNode extends ASTNode {
    List<String> columns;
    String tableName;
    ConditionNode whereClause;

    public SelectNode(List<String> columns, String tableName, ConditionNode whereClause) {
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public ConditionNode getWhereClause() {
        return whereClause;
    }

}