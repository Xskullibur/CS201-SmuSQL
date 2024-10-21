package edu.smu.smusql.parser.nodes;

// Node for literal values
public class LiteralNode extends ExpressionNode {

    public enum LiteralNodeType {
        STRING, NUMBER
    }

    Object value;
    LiteralNodeType type;

    public LiteralNode(String value, LiteralNodeType type) {
        this.type = type;

        switch (type) {
            case STRING:
                this.value = value.substring(1, value.length() - 1);
                break;
            case NUMBER:
                this.value = Integer.parseInt(value);
                break;
            default:
                this.value = value;
                break;
        }
    }
    
    public String getStringValue() {
        if (type != LiteralNodeType.STRING) {
            throw new IllegalStateException("Value is not a STRING");
        }
        return (String) value;
    }

    public Integer getIntegerValue() {
        if (type != LiteralNodeType.NUMBER) {
            throw new IllegalStateException("Value is not a NUMBER");
        }
        return (Integer) value;
    }

    public LiteralNodeType getType() {
        return type;
    }

}