package edu.smu.smusql.bplustreeA.AstParser.nodes;

// Node for assignment in UPDATE statements
public class AssignmentNode extends ASTNode {
    String column;
    ExpressionNode value;

    public AssignmentNode(String column, ExpressionNode value) {
        this.column = column;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public ExpressionNode getValue() {
        return value;
    }

}