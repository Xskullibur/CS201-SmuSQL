package edu.smu.smusql.bplustreeA.AstParser.nodes;

// Node for literal values
public class LiteralNode extends ExpressionNode {

    public enum LiteralNodeType {
        STRING, NUMBER, FLOAT
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
            case FLOAT:
                this.value = Float.parseFloat(value);
                break;
            default:
                this.value = value;
                break;
        }
    }

    public String getStringValue() {
        if (type != LiteralNodeType.STRING) {
            throw new IllegalStateException("Expected STRING received: " + value);
        }
        return (String) value;
    }

    public Integer getIntegerValue() {
        if (type != LiteralNodeType.NUMBER) {
            throw new IllegalStateException("Expected NUMBER received: " + value);
        }
        return (Integer) value;
    }

    public Float getFloatValue() {
        if (type != LiteralNodeType.FLOAT) {
            throw new IllegalStateException("Expected FLOAT received: " + value);
        }
        return (Float) value;
    }

    public Object getValue() {
        return value;
    }

    public LiteralNodeType getType() {
        return type;
    }

}