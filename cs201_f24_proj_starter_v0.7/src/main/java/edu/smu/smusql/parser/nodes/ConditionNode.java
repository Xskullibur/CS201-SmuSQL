package edu.smu.smusql.parser.nodes;

// Node for WHERE conditions
public class ConditionNode extends ASTNode {
    ASTNode left;
    String operator;
    ASTNode right;

    public ConditionNode(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ASTNode getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getRight() {
        return right;
    }

}