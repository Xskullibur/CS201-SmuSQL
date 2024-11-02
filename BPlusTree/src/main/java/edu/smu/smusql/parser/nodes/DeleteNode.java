package edu.smu.smusql.parser.nodes;

// Node for DELETE statements
public class DeleteNode extends ASTNode {
    String tableName;
    ConditionNode whereClause;

    public DeleteNode(String tableName, ConditionNode whereClause) {
        this.tableName = tableName;
        this.whereClause = whereClause;
    }

    public String getTableName() {
        return tableName;
    }

    public ConditionNode getWhereClause() {
        return whereClause;
    }

}