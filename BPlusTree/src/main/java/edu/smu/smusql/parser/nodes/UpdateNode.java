package edu.smu.smusql.parser.nodes;

import java.util.List;

// Node for UPDATE statements
public class UpdateNode extends ASTNode {
    String tableName;
    List<AssignmentNode> assignments;
    ConditionNode whereClause;

    public UpdateNode(String tableName, List<AssignmentNode> assignments, ConditionNode whereClause) {
        this.tableName = tableName;
        this.assignments = assignments;
        this.whereClause = whereClause;
    }

    public String getTableName() {
        return tableName;
    }

    public List<AssignmentNode> getAssignments() {
        return assignments;
    }

    public ConditionNode getWhereClause() {
        return whereClause;
    }

}