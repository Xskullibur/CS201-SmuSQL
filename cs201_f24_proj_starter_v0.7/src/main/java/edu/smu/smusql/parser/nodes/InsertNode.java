package edu.smu.smusql.parser.nodes;

import java.util.List;

// Node for INSERT statements
public class InsertNode extends ASTNode {
    String tableName;
    List<String> columns;
    List<ExpressionNode> values;

    public InsertNode(String tableName, List<String> columns, List<ExpressionNode> values) {
        this.tableName = tableName;
        this.columns = columns;
        this.values = values;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<ExpressionNode> getValues() {
        return values;
    }

}