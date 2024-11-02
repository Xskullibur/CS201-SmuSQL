package edu.smu.smusql.parser;

public class Token {
    TokenType type;
    String value;

    public enum TokenType {
        KEYWORD, OPERATOR, IDENTIFIER, LITERAL, PUNCTUATION
    }

    Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Token{type=%s, value='%s'}", type, value);
    }

}