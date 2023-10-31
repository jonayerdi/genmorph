package ch.usi.gassert.data.tree;

/*
 * Copyright 2012-2018 Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

import java.util.*;

public class Parser {

    /**
     * Unary operators precedence: + and - as prefix
     */
    public static final int OPERATOR_PRECEDENCE_UNARY = 60;

    /**
     * Equality operators precedence: =, ==, !=. <>
     */
    public static final int OPERATOR_PRECEDENCE_EQUALITY = 7;

    /**
     * Comparative operators precedence: <,>,<=,>=
     */
    public static final int OPERATOR_PRECEDENCE_COMPARISON = 10;

    /**
     * Or operator precedence: ||
     */
    public static final int OPERATOR_PRECEDENCE_OR = 2;

    /**
     * And operator precedence: &&
     */
    public static final int OPERATOR_PRECEDENCE_AND = 4;

    /**
     * Power operator precedence: ^
     */
    public static final int OPERATOR_PRECEDENCE_POWER = 40;

    /**
     * Multiplicative operators precedence: *,/,%
     */
    public static final int OPERATOR_PRECEDENCE_MULTIPLICATIVE = 30;

    /**
     * Additive operators precedence: + and -
     */
    public static final int OPERATOR_PRECEDENCE_ADDITIVE = 20;

    /**
     * What character to use for decimal separators.
     */
    private static final char DECIMAL_SEPARATOR = '.';

    /**
     * What character to use for minus sign (negative values).
     */
    private static final char MINUS_SIGN = '-';

    /**
     * The characters (other than letters and digits) allowed as the first
     * character in a variable.
     */
    private static final String FIRST_VAR_CHARS = "_";

    /**
     * The characters (other than letters and digits) allowed as the second or
     * subsequent characters in a variable.
     */
    private static final String VAR_CHARS = "_.";

    private static abstract class AbstractOperator {
        protected final String op;
        protected final int precedence;
        protected final boolean leftAssoc;
        protected final boolean booleanOperator;
        protected AbstractOperator(String op, int precedence, boolean leftAssoc, boolean booleanOperator) {
            this.op = op;
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
            this.booleanOperator = booleanOperator;
        }
        public String getOp() {
            return this.op;
        }
        public int getPrecedence() {
            return this.precedence;
        }
        public boolean isLeftAssoc() {
            return this.leftAssoc;
        }
        public boolean isBooleanOperator() {
            return this.booleanOperator;
        }
        public abstract String getKey();
    }

    private static class Operator extends AbstractOperator {
        public Operator(final String op, final int precedence, final boolean leftAssoc, final boolean booleanOperator) {
            super(op, precedence, leftAssoc, booleanOperator);
        }
        public String getKey() {
            return this.op;
        }
    }

    private static class UnaryOperator extends AbstractOperator {
        public UnaryOperator(final String op, final int precedence, final boolean leftAssoc, final boolean booleanOperator) {
            super(op, precedence, leftAssoc, booleanOperator);
        }
        public String getKey() {
            return this.op + "u";
        }
    }

    /**
     * All defined operators with name and implementation.
     */
    private static final Map<String, AbstractOperator> operators = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public static void addOperator(final AbstractOperator o) {
        operators.put(o.getKey(), o);
    }
    static {
        addOperator(new Operator("+", OPERATOR_PRECEDENCE_ADDITIVE, true, false) {});
        addOperator(new Operator("-", OPERATOR_PRECEDENCE_ADDITIVE, true, false) {});
        addOperator(new Operator("*", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true, false) {});
        addOperator(new Operator("/", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true, false) {});
        addOperator(new Operator("%", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true, false) {});
        addOperator(new Operator("^", OPERATOR_PRECEDENCE_POWER, false, false) {});
        addOperator(new Operator("&&", OPERATOR_PRECEDENCE_AND, false, true) {});
        addOperator(new Operator("||", OPERATOR_PRECEDENCE_OR, false, true) {});
        addOperator(new Operator(">", OPERATOR_PRECEDENCE_COMPARISON, false, true) {});
        addOperator(new Operator(">=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {});
        addOperator(new Operator("<", OPERATOR_PRECEDENCE_COMPARISON, false, true) {});
        addOperator(new Operator("<=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {});
        addOperator(new Operator("=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {});
        addOperator(new Operator("==", OPERATOR_PRECEDENCE_EQUALITY, false, true) {});
        addOperator(new Operator("!=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {});
        addOperator(new Operator("<>", OPERATOR_PRECEDENCE_EQUALITY, false, true) {});
        addOperator(new UnaryOperator("-", OPERATOR_PRECEDENCE_UNARY, false, false) {});
        addOperator(new UnaryOperator("+", OPERATOR_PRECEDENCE_UNARY, false, false) {});
        // Added for GAssert
        addOperator(new Operator("=>", OPERATOR_PRECEDENCE_UNARY, false, true) {});
        addOperator(new Operator("<=>", OPERATOR_PRECEDENCE_UNARY, false, true) {});
    }

    public enum TokenType {
        VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR, OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRING_PARAM
    }

    public static class Token {
        public StringBuilder surfaceSB = new StringBuilder();
        public String surface = "";
        public TokenType type;
        public int pos;

        public void append(final char c) {
            surfaceSB.append(c);
            surface = surfaceSB.toString();
        }

        public void append(final String s) {
            surfaceSB.append(s);
            surface = surfaceSB.toString();
        }

        public char charAt(final int pos) {
            return surface.charAt(pos);
        }

        public int length() {
            return surface.length();
        }

        @Override
        public String toString() {
            return surface;
        }
    }

    /**
     * Expression tokenizer that allows to iterate over a {@link String}
     * expression token by token. Blank characters will be skipped.
     */
    private static class Tokenizer implements Iterator<Token> {

        /**
         * Actual position in expression string.
         */
        private int pos = 0;

        /**
         * The original input expression.
         */
        private final String input;
        /**
         * The previous token or <code>null</code> if none.
         */
        private Token previousToken;

        /**
         * Creates a new tokenizer for an expression.
         *
         * @param input The expression string.
         */
        public Tokenizer(final String input) {
            this.input = input.trim();
        }

        @Override
        public boolean hasNext() {
            return (pos < input.length());
        }

        /**
         * Peek at the next character, without advancing the iterator.
         *
         * @return The next character or character 0, if at end of string.
         */
        private char peekNextChar() {
            if (pos < (input.length() - 1)) {
                return input.charAt(pos + 1);
            } else {
                return 0;
            }
        }

        private boolean isHexDigit(final char ch) {
            return ch == 'x' || ch == 'X' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
        }

        @Override
        public Token next() {
            final Token token = new Token();

            if (pos >= input.length()) {
                return previousToken = null;
            }
            char ch = input.charAt(pos);
            while (Character.isWhitespace(ch) && pos < input.length()) {
                ch = input.charAt(++pos);
            }
            token.pos = pos;

            boolean isHex = false;

            if (Character.isDigit(ch) || (ch == DECIMAL_SEPARATOR && Character.isDigit(peekNextChar()))) {
                if (ch == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X')) {
                    isHex = true;
                }
                while ((isHex
                        && isHexDigit(
                        ch))
                        || (Character.isDigit(ch) || ch == DECIMAL_SEPARATOR || ch == 'e' || ch == 'E'
                        || (ch == MINUS_SIGN && token.length() > 0
                        && ('e' == token.charAt(token.length() - 1)
                        || 'E' == token.charAt(token.length() - 1)))
                        || (ch == '+' && token.length() > 0
                        && ('e' == token.charAt(token.length() - 1)
                        || 'E' == token.charAt(token.length() - 1))))
                        && (pos < input.length())) {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                token.type = isHex ? TokenType.HEX_LITERAL : TokenType.LITERAL;
            } else if (ch == '"') {
                pos++;
                if (previousToken.type != TokenType.STRING_PARAM) {
                    ch = input.charAt(pos);
                    while (ch != '"') {
                        token.append(input.charAt(pos++));
                        ch = pos == input.length() ? 0 : input.charAt(pos);
                    }
                    token.type = TokenType.STRING_PARAM;
                } else {
                    return next();
                }
            } else if (Character.isLetter(ch) || FIRST_VAR_CHARS.indexOf(ch) >= 0) {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || VAR_CHARS.indexOf(ch) >= 0
                        || token.length() == 0 && FIRST_VAR_CHARS.indexOf(ch) >= 0) && (pos < input.length())) {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                // Remove optional white spaces after function or variable name
                if (Character.isWhitespace(ch)) {
                    while (Character.isWhitespace(ch) && pos < input.length()) {
                        ch = input.charAt(pos++);
                    }
                    pos--;
                }
                token.type = ch == '(' ? TokenType.FUNCTION : TokenType.VARIABLE;
            } else if (ch == '(' || ch == ')' || ch == ',') {
                if (ch == '(') {
                    token.type = TokenType.OPEN_PAREN;
                } else if (ch == ')') {
                    token.type = TokenType.CLOSE_PAREN;
                } else {
                    token.type = TokenType.COMMA;
                }
                token.append(ch);
                pos++;
            } else {
                String greedyMatch = "";
                final int initialPos = pos;
                ch = input.charAt(pos);
                int validOperatorSeenUntil = -1;
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && FIRST_VAR_CHARS.indexOf(ch) < 0
                        && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ','
                        && (pos < input.length())) {
                    greedyMatch += ch;
                    pos++;
                    if (operators.containsKey(greedyMatch)) {
                        validOperatorSeenUntil = pos;
                    }
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                if (validOperatorSeenUntil != -1) {
                    token.append(input.substring(initialPos, validOperatorSeenUntil));
                    pos = validOperatorSeenUntil;
                } else {
                    token.append(greedyMatch);
                }

                if (previousToken == null || previousToken.type == TokenType.OPERATOR
                        || previousToken.type == TokenType.OPEN_PAREN || previousToken.type == TokenType.COMMA) {
                    token.surface += "u";
                    token.type = TokenType.UNARY_OPERATOR;
                } else {
                    token.type = TokenType.OPERATOR;
                }
            }
            return previousToken = token;
        }

        @Override
        public void remove() {
            throw new RuntimeException("remove() not supported");
        }

    }

    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix expression to a RPN expression.
     *
     * @param expression The input expression in infx.
     * @return A RPN representation of the expression, with each token as a list
     * member.
     */
    private static List<Token> shuntingYard(final String expression) {
        final List<Token> outputQueue = new ArrayList<>();
        final Stack<Token> stack = new Stack<>();

        final Tokenizer tokenizer = new Tokenizer(expression);

        Token lastFunction = null;
        Token previousToken = null;
        while (tokenizer.hasNext()) {
            final Token token = tokenizer.next();
            switch (token.type) {
                case STRING_PARAM:
                    stack.push(token);
                    break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (previousToken.type == TokenType.LITERAL || previousToken.type == TokenType.HEX_LITERAL)) {
                        throw new RuntimeException("Missing operator at character position " + token.pos);
                    }
                    outputQueue.add(token);
                    break;
                case VARIABLE:
                    outputQueue.add(token);
                    break;
                case FUNCTION:
                    stack.push(token);
                    lastFunction = token;
                    break;
                case COMMA:
                    if (previousToken != null && previousToken.type == TokenType.OPERATOR) {
                        throw new RuntimeException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty()) {
                        if (lastFunction == null) {
                            throw new RuntimeException("Unexpected comma at character position " + token.pos);
                        } else {
                            throw new RuntimeException(
                                    "Parse error for function '" + lastFunction + "' at character position " + token.pos);
                        }
                    }
                    break;
                case OPERATOR: {
                    if (previousToken != null
                            && (previousToken.type == TokenType.COMMA || previousToken.type == TokenType.OPEN_PAREN)) {
                        throw new RuntimeException(
                                "Missing parameter(s) for operator " + token + " at character position " + token.pos);
                    }
                    final AbstractOperator o1 = operators.get(token.surface);
                    if (o1 == null) {
                        throw new RuntimeException("Unknown operator '" + token + "' at position " + (token.pos + 1));
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR: {
                    if (previousToken != null && previousToken.type != TokenType.OPERATOR
                            && previousToken.type != TokenType.COMMA && previousToken.type != TokenType.OPEN_PAREN) {
                        throw new RuntimeException(
                                "Invalid position for unary operator " + token + " at character position " + token.pos);
                    }
                    final AbstractOperator o1 = operators.get(token.surface);
                    if (o1 == null) {
                        throw new RuntimeException(
                                "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1)
                                        + "' at position " + (token.pos + 1));
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    if (previousToken != null) {
                        if (previousToken.type == TokenType.LITERAL || previousToken.type == TokenType.CLOSE_PAREN
                                || previousToken.type == TokenType.VARIABLE
                                || previousToken.type == TokenType.HEX_LITERAL) {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            final Token multiplication = new Token();
                            multiplication.append("*");
                            multiplication.type = TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == TokenType.FUNCTION) {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == TokenType.OPERATOR) {
                        throw new RuntimeException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty()) {
                        throw new RuntimeException("Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == TokenType.FUNCTION) {
                        outputQueue.add(stack.pop());
                    }
            }
            previousToken = token;
        }

        while (!stack.isEmpty()) {
            final Token element = stack.pop();
            if (element.type == TokenType.OPEN_PAREN || element.type == TokenType.CLOSE_PAREN) {
                throw new RuntimeException("Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private static void shuntOperators(final List<Token> outputQueue, final Stack<Token> stack, final AbstractOperator o1) {
        Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == TokenType.OPERATOR
                || nextToken.type == TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence()))) {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }

    public static List<Token> toRPN(final String expression) {
        return shuntingYard(expression);
    }

}
