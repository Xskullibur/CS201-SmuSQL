package edu.smu.smusql.parser.nodes;

// Node for literal values
public class LiteralNode extends ExpressionNode {
    String value;
    String type; // "STRING" or "NUMBER"

    public LiteralNode(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

}