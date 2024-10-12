package edu.smu.smusql.parser;

import java.util.List;

// Base class for all AST nodes
abstract class ASTNode {
}

// Root node for all SQL statements
class SQLStatementNode extends ASTNode {
    ASTNode statement;

    SQLStatementNode(ASTNode statement) {
        this.statement = statement;
    }
}

// Node for CREATE statements
class CreateTableNode extends ASTNode {
    String tableName;
    List<String> columns;

    CreateTableNode(String tableName, List<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }
}

// Node for INSERT statements
class InsertNode extends ASTNode {
    String tableName;
    List<String> columns;
    List<ExpressionNode> values;

    InsertNode(String tableName, List<String> columns, List<ExpressionNode> values) {
        this.tableName = tableName;
        this.columns = columns;
        this.values = values;
    }
}

// Node for SELECT statements
class SelectNode extends ASTNode {
    List<String> columns;
    String tableName;
    ConditionNode whereClause;

    SelectNode(List<String> columns, String tableName, ConditionNode whereClause) {
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
    }
}

// Node for UPDATE statements
class UpdateNode extends ASTNode {
    String tableName;
    List<AssignmentNode> assignments;
    ConditionNode whereClause;

    UpdateNode(String tableName, List<AssignmentNode> assignments, ConditionNode whereClause) {
        this.tableName = tableName;
        this.assignments = assignments;
        this.whereClause = whereClause;
    }
}

// Node for DELETE statements
class DeleteNode extends ASTNode {
    String tableName;
    ConditionNode whereClause;

    DeleteNode(String tableName, ConditionNode whereClause) {
        this.tableName = tableName;
        this.whereClause = whereClause;
    }
}

// Node for assignment in UPDATE statements
class AssignmentNode extends ASTNode {
    String column;
    ExpressionNode value;

    AssignmentNode(String column, ExpressionNode value) {
        this.column = column;
        this.value = value;
    }
}

// Node for WHERE conditions
class ConditionNode extends ASTNode {
    ASTNode left;
    String operator;
    ASTNode right;

    ConditionNode(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

// Base class for expressions (columns, literals)
abstract class ExpressionNode extends ASTNode {
}

// Node for column references
class ColumnNode extends ExpressionNode {
    String name;

    ColumnNode(String name) {
        this.name = name;
    }
}

// Node for literal values
class LiteralNode extends ExpressionNode {
    String value;
    String type; // "STRING" or "NUMBER"

    LiteralNode(String value, String type) {
        this.value = value;
        this.type = type;
    }
}