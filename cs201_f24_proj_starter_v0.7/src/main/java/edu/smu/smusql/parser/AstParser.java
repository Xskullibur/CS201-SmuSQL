package edu.smu.smusql.parser;

import java.util.List;
import java.util.ArrayList;

public class AstParser {

    private List<Token> tokens;
    private int currentIndex;

    public AstParser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentIndex = 0;
    }

    public SQLStatementNode parse() {
        Token firstToken = tokens.get(0);
        ASTNode statement;

        switch (firstToken.value) {
            case "CREATE":
                statement = parseCreateTable();
                break;
            case "INSERT":
                statement = parseInsert();
                break;
            case "SELECT":
                statement = parseSelect();
                break;
            case "UPDATE":
                statement = parseUpdate();
                break;
            case "DELETE":
                statement = parseDelete();
                break;
            default:
                throw new RuntimeException("Unsupported SQL statement: " + firstToken.value);
        }

        return new SQLStatementNode(statement);
    }

    private CreateTableNode parseCreateTable() {
        expect("CREATE");
        expect("TABLE");
        String tableName = expectIdentifier();
        List<String> columns = parseColumnList();
        return new CreateTableNode(tableName, columns);
    }

    private InsertNode parseInsert() {
        expect("INSERT");
        expect("INTO");
        String tableName = expectIdentifier();
        List<String> columns = parseColumnList();
        expect("VALUES");
        List<ExpressionNode> values = parseExpressionList();
        return new InsertNode(tableName, columns, values);
    }

    private SelectNode parseSelect() {
        expect("SELECT");
        List<String> columns = parseColumnNames();
        expect("FROM");
        String tableName = expectIdentifier();
        ConditionNode whereClause = null;
        if (match("WHERE")) {
            whereClause = parseCondition();
        }
        return new SelectNode(columns, tableName, whereClause);
    }

    private UpdateNode parseUpdate() {
        expect("UPDATE");
        String tableName = expectIdentifier();
        expect("SET");
        List<AssignmentNode> assignments = parseAssignments();
        ConditionNode whereClause = null;
        if (match("WHERE")) {
            whereClause = parseCondition();
        }
        return new UpdateNode(tableName, assignments, whereClause);
    }

    private DeleteNode parseDelete() {
        expect("DELETE");
        expect("FROM");
        String tableName = expectIdentifier();
        ConditionNode whereClause = null;
        if (match("WHERE")) {
            whereClause = parseCondition();
        }
        return new DeleteNode(tableName, whereClause);
    }

    private List<String> parseColumnList() {
        List<String> columns = new ArrayList<>();
        expect("(");
        do {
            columns.add(expectIdentifier());
        } while (match(","));
        expect(")");
        return columns;
    }

    private List<ExpressionNode> parseExpressionList() {
        List<ExpressionNode> expressions = new ArrayList<>();
        expect("(");
        do {
            expressions.add(parseExpression());
        } while (match(","));
        expect(")");
        return expressions;
    }

    private List<String> parseColumnNames() {
        List<String> columns = new ArrayList<>();
        if (match("*")) {
            columns.add("*");
        } else {
            do {
                columns.add(expectIdentifier());
            } while (match(","));
        }
        return columns;
    }

    private List<AssignmentNode> parseAssignments() {
        List<AssignmentNode> assignments = new ArrayList<>();
        do {
            String column = expectIdentifier();
            expect("=");
            ExpressionNode value = parseExpression();
            assignments.add(new AssignmentNode(column, value));
        } while (match(","));
        return assignments;
    }

    private ConditionNode parseCondition() {
        ConditionNode condition = parseSimpleCondition();
        
        while (match("AND") || match("OR")) {
            String operator = tokens.get(currentIndex - 1).value;
            ConditionNode right = parseSimpleCondition();
            condition = new ConditionNode(condition, operator, right);
        }
        
        return condition;
    }

    private ConditionNode parseSimpleCondition() {
        ExpressionNode left = parseExpression();
        String operator = expectOperator();
        ExpressionNode right = parseExpression();
        return new ConditionNode(left, operator, right);
    }

    private ExpressionNode parseExpression() {
        Token token = tokens.get(currentIndex);
        if (token.type == TokenType.IDENTIFIER) {
            currentIndex++;
            return new ColumnNode(token.value);
        } else if (token.type == TokenType.LITERAL) {
            currentIndex++;
            String type = token.value.startsWith("'") ? "STRING" : "NUMBER";
            return new LiteralNode(token.value, type);
        } else {
            throw new RuntimeException("Unexpected token in expression: " + token.value);
        }
    }

    private void expect(String expected) {
        Token token = tokens.get(currentIndex);
        if (token.type != TokenType.KEYWORD || !token.value.equalsIgnoreCase(expected)) {
            throw new RuntimeException("Expected " + expected + ", but got " + token.value);
        }
        currentIndex++;
    }

    private String expectIdentifier() {
        Token token = tokens.get(currentIndex);
        if (token.type != TokenType.IDENTIFIER) {
            throw new RuntimeException("Expected identifier, but got " + token.value);
        }
        currentIndex++;
        return token.value;
    }

    private String expectOperator() {
        Token token = tokens.get(currentIndex);
        if (token.type != TokenType.OPERATOR) {
            throw new RuntimeException("Expected operator, but got " + token.value);
        }
        currentIndex++;
        return token.value;
    }

    private boolean match(String value) {
        Token token = tokens.get(currentIndex);
        if (token.type == TokenType.KEYWORD && token.value.equalsIgnoreCase(value)) {
            currentIndex++;
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        
    }
}