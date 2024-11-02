package edu.smu.smusql.parser.nodes;

import java.util.List;

// Node for INSERT statements
public class InsertNode extends ASTNode {
    String tableName;
    ExpressionNode primaryKey;
    List<ExpressionNode> values;

    public InsertNode(String tableName, ExpressionNode primaryKey, List<ExpressionNode> values) {
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.values = values;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ExpressionNode> getValues() {
        return values;
    }

    public LiteralNode getPrimaryKey() {
        return (LiteralNode) primaryKey;
    }

}