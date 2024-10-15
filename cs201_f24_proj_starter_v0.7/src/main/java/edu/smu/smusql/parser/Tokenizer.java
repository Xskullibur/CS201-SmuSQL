package edu.smu.smusql.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.smu.smusql.parser.Token.TokenType;

public class Tokenizer {

    private static final String[] KEYWORDS = {
            "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE", "UPDATE", "SET", "DELETE",
            "CREATE", "TABLE", "AND", "OR"
    };

    private static final String[] OPERATORS = {
            "=", "<", ">", "<=", ">=", "<>", "!="
    };

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?i)\\b(" + String.join("|", KEYWORDS) + ")\\b" + "|" +
                    "(" + String.join("|", OPERATORS) + ")" + "|" +
                    "(\\d+)" + "|" +
                    "'([^']*)'" + "|" +
                    "([a-zA-Z_]\\w*)" + "|" +
                    "(,|\\(|\\))");

    public static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(new Token(TokenType.KEYWORD, matcher.group(1).toUpperCase()));
            } else if (matcher.group(2) != null) {
                tokens.add(new Token(TokenType.OPERATOR, matcher.group(2)));
            } else if (matcher.group(3) != null) {
                tokens.add(new Token(TokenType.LITERAL, matcher.group(3)));
            } else if (matcher.group(4) != null) {
                tokens.add(new Token(TokenType.LITERAL, "'" + matcher.group(4) + "'"));
            } else if (matcher.group(5) != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, matcher.group(5)));
            } else if (matcher.group(6) != null) {
                tokens.add(new Token(TokenType.PUNCTUATION, matcher.group(6)));
            }
        }

        return tokens;
    }

    public static void main(String[] args) {
        // Test: Tokenize Keywords
        String input1 = "SELECT * FROM table WHERE column = 10";
        List<Token> tokens1 = Tokenizer.tokenize(input1);

        System.out.println("Test: Tokenize Keywords");
        System.out.println(
                "Expected: [(KEYWORD, SELECT), (PUNCTUATION, *), (KEYWORD, FROM), (IDENTIFIER, table), (KEYWORD, WHERE), (IDENTIFIER, column), (OPERATOR, =), (LITERAL, 10)]");
        System.out.print("Actual: [");
        for (Token token : tokens1) {
            System.out.print("(" + token.type + ", " + token.value + "), ");
        }
        System.out.println("]");
        System.out.println();

        // Test: Tokenize Operators
        String input2 = "column1 >= 5 AND column2 < 10";
        List<Token> tokens2 = Tokenizer.tokenize(input2);

        System.out.println("Test: Tokenize Operators");
        System.out.println(
                "Expected: [(IDENTIFIER, column1), (OPERATOR, >=), (LITERAL, 5), (KEYWORD, AND), (IDENTIFIER, column2), (OPERATOR, <), (LITERAL, 10)]");
        System.out.print("Actual: [");
        for (Token token : tokens2) {
            System.out.print("(" + token.type + ", " + token.value + "), ");
        }
        System.out.println("]");
        System.out.println();

        // Test: Tokenize Literals
        String input3 = "INSERT INTO table VALUES (1, 'text', 3.14)";
        List<Token> tokens3 = Tokenizer.tokenize(input3);

        System.out.println("Test: Tokenize Literals");
        System.out.println(
                "Expected: [(KEYWORD, INSERT), (KEYWORD, INTO), (IDENTIFIER, table), (KEYWORD, VALUES), (PUNCTUATION, (), (LITERAL, 1), (PUNCTUATION, ,), (LITERAL, 'text'), (PUNCTUATION, ,), (LITERAL, 3.14), (PUNCTUATION, ))]");
        System.out.print("Actual: [");
        for (Token token : tokens3) {
            System.out.print("(" + token.type + ", " + token.value + "), ");
        }
        System.out.println("]");
        System.out.println();

        // Test: Tokenize Complex Query
        String input4 = "UPDATE table SET column1 = 'value' WHERE column2 <> 100";
        List<Token> tokens4 = Tokenizer.tokenize(input4);

        System.out.println("Test: Tokenize Complex Query");
        System.out.println(
                "Expected: [(KEYWORD, UPDATE), (IDENTIFIER, table), (KEYWORD, SET), (IDENTIFIER, column1), (OPERATOR, =), (LITERAL, 'value'), (KEYWORD, WHERE), (IDENTIFIER, column2), (OPERATOR, <>), (LITERAL, 100)]");
        System.out.print("Actual: [");
        for (Token token : tokens4) {
            System.out.print("(" + token.type + ", " + token.value + "), ");
        }
        System.out.println("]");
    }
}