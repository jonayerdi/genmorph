package com.udojava.evalex;

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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;


/**
 * <h1>EvalEx - Java Expression Evaluator</h1>
 *
 * <h2>Introduction</h2> EvalEx is a handy expression evaluator for Java, that
 * allows to evaluate simple mathematical and boolean expressions. <br>
 * For more information, see:
 * <a href="https://github.com/uklimaschewski/EvalEx">EvalEx GitHub
 * repository</a>
 * <ul>
 * <li>The software is licensed under the MIT Open Source license (see <a href=
 * "https://raw.githubusercontent.com/uklimaschewski/EvalEx/master/LICENSE">LICENSE
 * file</a>).</li>
 * <li>The *power of* operator (^) implementation was copied from <a href=
 * "http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java">Stack
 * Overflow</a>. Thanks to Gene Marin.</li>
 * <li>The SQRT() function implementation was taken from the book <a href=
 * "http://www.amazon.de/Java-Number-Cruncher-Programmers-Numerical/dp/0130460419">The
 * Java Programmers Guide To numerical Computing</a> (Ronald Mak, 2002).</li>
 * </ul>
 *
 * @authors Thanks to all who contributed to this project: <a href=
 * "https://github.com/uklimaschewski/EvalEx/graphs/contributors">Contributors</a>
 * @see <a href="https://github.com/uklimaschewski/EvalEx">GitHub repository</a>
 */
public class Expression {

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
     * Definition of PI as a constant, can be used in expressions as variable.
     */
    public static final BigDecimal PI = new BigDecimal(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    /**
     * Definition of e: "Euler's number" as a constant, can be used in
     * expressions as variable.
     */
    public static final BigDecimal e = new BigDecimal(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    /**
     * The {@link MathContext} to use for calculations.
     */
    private MathContext mc = null;

    /**
     * The characters (other than letters and digits) allowed as the first
     * character in a variable.
     */
    private String firstVarChars = "_";

    /**
     * The characters (other than letters and digits) allowed as the second or
     * subsequent characters in a variable.
     */
    private String varChars = "_";

    /**
     * The original infix expression.
     */
    private final String originalExpression;

    /**
     * The current infix expression, with optional variable substitutions.
     */
    private String expression = null;

    /**
     * The cached RPN (Reverse Polish Notation) of the expression.
     */
    private List<Token> rpn = null;

    /**
     * All defined operators with name and implementation.
     */
    private Map<String, LazyOperator> operators = new TreeMap<String, LazyOperator>(
            String.CASE_INSENSITIVE_ORDER);

    /**
     * All defined functions with name and implementation.
     */
    private Map<String, com.udojava.evalex.LazyFunction> functions = new TreeMap<String, com.udojava.evalex.LazyFunction>(
            String.CASE_INSENSITIVE_ORDER);

    /**
     * All defined variables with name and value.
     */
    private Map<String, LazyNumber> variables = new TreeMap<String, LazyNumber>(String.CASE_INSENSITIVE_ORDER);

    /**
     * What character to use for decimal separators.
     */
    private static final char decimalSeparator = '.';

    /**
     * What character to use for minus sign (negative values).
     */
    private static final char minusSign = '-';

    /**
     * The BigDecimal representation of the left parenthesis, used for parsing
     * varying numbers of function parameters.
     */
    private static final LazyNumber PARAMS_START = new LazyNumber() {
        public BigDecimal eval() {
            return null;
        }

        public String getString() {
            return null;
        }
    };

    /**
     * The expression evaluators exception class.
     */
    public static class ExpressionException extends RuntimeException {
        private static final long serialVersionUID = 1118142866870779047L;

        public ExpressionException(final String message) {
            super(message);
        }
    }

    /**
     * LazyNumber interface created for lazily evaluated functions
     */
    public interface LazyNumber {
        BigDecimal eval();

        String getString();
    }

    /**
     * Construct a LazyNumber from a BigDecimal
     */
    private LazyNumber CreateLazyNumber(final BigDecimal bigDecimal) {
        return new LazyNumber() {
            @Override
            public String getString() {
                return bigDecimal.toPlainString();
            }

            @Override
            public BigDecimal eval() {
                return bigDecimal;
            }
        };
    }

    public abstract class LazyFunction extends AbstractLazyFunction {
        /**
         * Creates a new function with given name and parameter count.
         *
         * @param name            The name of the function.
         * @param numParams       The number of parameters for this function.
         *                        <code>-1</code> denotes a variable number of parameters.
         * @param booleanFunction Whether this function is a boolean function.
         */
        public LazyFunction(final String name, final int numParams, final boolean booleanFunction) {
            super(name, numParams, booleanFunction);
        }

        /**
         * Creates a new function with given name and parameter count.
         *
         * @param name      The name of the function.
         * @param numParams The number of parameters for this function.
         *                  <code>-1</code> denotes a variable number of parameters.
         */
        public LazyFunction(final String name, final int numParams) {
            super(name, numParams);
        }
    }

    /**
     * Abstract definition of a supported expression function. A function is
     * defined by a name, the number of parameters and the actual processing
     * implementation.
     */
    public abstract class Function extends AbstractFunction {

        public Function(final String name, final int numParams) {
            super(name, numParams);
        }

        public Function(final String name, final int numParams, final boolean booleanFunction) {
            super(name, numParams, booleanFunction);
        }
    }

    /**
     * Abstract definition of a supported operator. An operator is defined by
     * its name (pattern), precedence and if it is left- or right associative.
     */
    public abstract class Operator extends AbstractOperator {

        /**
         * Creates a new operator.
         *
         * @param oper            The operator name (pattern).
         * @param precedence      The operators precedence.
         * @param leftAssoc       <code>true</code> if the operator is left associative,
         *                        else <code>false</code>.
         * @param booleanOperator Whether this operator is boolean.
         */
        public Operator(final String oper, final int precedence, final boolean leftAssoc, final boolean booleanOperator) {
            super(oper, precedence, leftAssoc, booleanOperator);
        }

        /**
         * Creates a new operator.
         *
         * @param oper       The operator name (pattern).
         * @param precedence The operators precedence.
         * @param leftAssoc  <code>true</code> if the operator is left associative,
         *                   else <code>false</code>.
         */
        public Operator(final String oper, final int precedence, final boolean leftAssoc) {
            super(oper, precedence, leftAssoc);
        }
    }

    public abstract class UnaryOperator extends AbstractUnaryOperator {

        public UnaryOperator(final String oper, final int precedence, final boolean leftAssoc) {
            super(oper, precedence, leftAssoc);
        }
    }

    public enum TokenType {
        VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR, OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRINGPARAM
    }

    public class Token {
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
    private class Tokenizer implements Iterator<Token> {

        /**
         * Actual position in expression string.
         */
        private int pos = 0;

        /**
         * The original input expression.
         */
        private String input;
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

            if (Character.isDigit(ch) || (ch == decimalSeparator && Character.isDigit(peekNextChar()))) {
                if (ch == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X')) {
                    isHex = true;
                }
                while ((isHex
                        && isHexDigit(
                        ch))
                        || (Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E'
                        || (ch == minusSign && token.length() > 0
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
                if (previousToken.type != TokenType.STRINGPARAM) {
                    ch = input.charAt(pos);
                    while (ch != '"') {
                        token.append(input.charAt(pos++));
                        ch = pos == input.length() ? 0 : input.charAt(pos);
                    }
                    token.type = TokenType.STRINGPARAM;
                } else {
                    return next();
                }
            } else if (Character.isLetter(ch) || firstVarChars.indexOf(ch) >= 0) {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || varChars.indexOf(ch) >= 0
                        || token.length() == 0 && firstVarChars.indexOf(ch) >= 0) && (pos < input.length())) {
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
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && firstVarChars.indexOf(ch) < 0
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
            throw new ExpressionException("remove() not supported");
        }

    }

    /**
     * Creates a new expression instance from an expression string with a given
     * default match context of {@link MathContext#DECIMAL32}.
     *
     * @param expression The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                   <code>"sin(y)>0 & max(z, 3)>3"</code>
     */
    public Expression(final String expression) {
        this(expression, MathContext.DECIMAL32);
    }

    /**
     * Creates a new expression instance from an expression string with a given
     * default match context.
     *
     * @param expression         The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                           <code>"sin(y)>0 & max(z, 3)>3"</code>
     * @param defaultMathContext The {@link MathContext} to use by default.
     */
    public Expression(final String expression, final MathContext defaultMathContext) {
        mc = defaultMathContext;
        this.expression = expression;
        originalExpression = expression;
        addOperator(new Operator("+", OPERATOR_PRECEDENCE_ADDITIVE, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.add(v2, mc);
            }
        });
        addOperator(new Operator("-", OPERATOR_PRECEDENCE_ADDITIVE, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.subtract(v2, mc);
            }
        });
        addOperator(new Operator("*", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.multiply(v2, mc);
            }
        });
        addOperator(new Operator("/", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.divide(v2, mc);
            }
        });
        addOperator(new Operator("%", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.remainder(v2, mc);
            }
        });
        addOperator(new Operator("^", OPERATOR_PRECEDENCE_POWER, false) {
            @Override
            public BigDecimal eval(final BigDecimal v1, BigDecimal v2) {
                assertNotNull(v1, v2);
                /*-
                 * Thanks to Gene Marin:
                 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
                 */
                final int signOf2 = v2.signum();
                final double dn1 = v1.doubleValue();
                v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
                final BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
                final BigDecimal n2IntPart = v2.subtract(remainderOf2);
                final BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
                final BigDecimal doublePow = new BigDecimal(Math.pow(dn1, remainderOf2.doubleValue()));

                BigDecimal result = intPow.multiply(doublePow, mc);
                if (signOf2 == -1) {
                    result = BigDecimal.ONE.divide(result, mc.getPrecision(), RoundingMode.HALF_UP);
                }
                return result;
            }
        });
        addOperator(new Operator("&&", OPERATOR_PRECEDENCE_AND, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);

                final boolean b1 = v1.compareTo(BigDecimal.ZERO) != 0;

                if (!b1) {
                    return BigDecimal.ZERO;
                }

                final boolean b2 = v2.compareTo(BigDecimal.ZERO) != 0;
                return b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("||", OPERATOR_PRECEDENCE_OR, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);

                final boolean b1 = v1.compareTo(BigDecimal.ZERO) != 0;

                if (b1) {
                    return BigDecimal.ONE;
                }

                final boolean b2 = v2.compareTo(BigDecimal.ZERO) != 0;
                return b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator(">", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator(">=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("<", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) == -1 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("<=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                if (v1 == v2) {
                    return BigDecimal.ONE;
                }
                if (v1 == null || v2 == null) {
                    return BigDecimal.ZERO;
                }
                return v1.compareTo(v2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new Operator("==", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                return ((Operator) operators.get("=")).eval(v1, v2);
            }
        });

        addOperator(new Operator("!=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                if (v1 == v2) {
                    return BigDecimal.ZERO;
                }
                if (v1 == null || v2 == null) {
                    return BigDecimal.ONE;
                }
                return v1.compareTo(v2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new Operator("<>", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {
                assertNotNull(v1, v2);
                return ((Operator) operators.get("!=")).eval(v1, v2);
            }
        });
        addOperator(new UnaryOperator("-", OPERATOR_PRECEDENCE_UNARY, false) {
            @Override
            public BigDecimal evalUnary(final BigDecimal v1) {
                return v1.multiply(new BigDecimal(-1));
            }
        });
        addOperator(new UnaryOperator("+", OPERATOR_PRECEDENCE_UNARY, false) {
            @Override
            public BigDecimal evalUnary(final BigDecimal v1) {
                return v1.multiply(BigDecimal.ONE);
            }
        });
        /*
         * add by GAssert
         */
        addOperator(new Operator("=>", OPERATOR_PRECEDENCE_UNARY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {

                if (v1 == BigDecimal.ONE) {
                    return v2;
                }
                return BigDecimal.ONE;

            }
        });
        addOperator(new Operator("<=>", OPERATOR_PRECEDENCE_UNARY, false, true) {
            @Override
            public BigDecimal eval(final BigDecimal v1, final BigDecimal v2) {

                //returns true if both are true or false
                if (v1 == v2) {
                    return BigDecimal.ONE;
                }
                return BigDecimal.ZERO;

            }
        });

        addFunction(new Function("FACT", 1, false) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));

                final int number = parameters.get(0).intValue();
                BigDecimal factorial = BigDecimal.ONE;
                for (int i = 1; i <= number; i++) {
                    factorial = factorial.multiply(new BigDecimal(i));
                }
                return factorial;
            }
        });

        addFunction(new Function("NOT", 1, true) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
                return zero ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addLazyFunction(new LazyFunction("IF", 3) {
            @Override
            public LazyNumber lazyEval(final List<LazyNumber> lazyParams) {
                final BigDecimal result = lazyParams.get(0).eval();
                assertNotNull(result);
                final boolean isTrue = result.compareTo(BigDecimal.ZERO) != 0;
                return isTrue ? lazyParams.get(1) : lazyParams.get(2);
            }
        });

        addFunction(new Function("RANDOM", 0) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                final double d = Math.random();
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SIN", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.sin(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COS", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.cos(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TAN", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.tan(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ASIN", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.toDegrees(Math.asin(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ACOS", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.toDegrees(Math.acos(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ATAN", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.toDegrees(Math.atan(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ATAN2", 2) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0), parameters.get(1));
                final double d = Math.toDegrees(Math.atan2(parameters.get(0).doubleValue(), parameters.get(1).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SINH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.sinh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COSH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.cosh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TANH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.tanh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SEC", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: sec(x) = 1 / cos(x) */
                final double one = 1;
                final double d = Math.cos(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("CSC", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: csc(x) = 1 / sin(x) */
                final double one = 1;
                final double d = Math.sin(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("SECH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: sech(x) = 1 / cosh(x) */
                final double one = 1;
                final double d = Math.cosh(parameters.get(0).doubleValue());
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("CSCH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: csch(x) = 1 / sinh(x) */
                final double one = 1;
                final double d = Math.sinh(parameters.get(0).doubleValue());
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("COT", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x) */
                final double one = 1;
                final double d = Math.tan(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("ACOT", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: acot(x) = atan(1/x) */
                if (parameters.get(0).doubleValue() == 0) {
                    throw new ExpressionException("Number must not be 0");
                }
                final double d = Math.toDegrees(Math.atan(1 / parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COTH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: coth(x) = 1 / tanh(x) */
                final double one = 1;
                final double d = Math.tanh(parameters.get(0).doubleValue());
                return new BigDecimal((one / d), mc);
            }
        });
        addFunction(new Function("ASINH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: asinh(x) = ln(x + sqrt(x^2 + 1)) */
                final double d = Math.log(parameters.get(0).doubleValue()
                        + (Math.sqrt(Math.pow(parameters.get(0).doubleValue(), 2) + 1)));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ACOSH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: acosh(x) = ln(x + sqrt(x^2 - 1)) */
                if (Double.compare(parameters.get(0).doubleValue(), 1) < 0) {
                    throw new ExpressionException("Number must be x >= 1");
                }
                final double d = Math.log(parameters.get(0).doubleValue()
                        + (Math.sqrt(Math.pow(parameters.get(0).doubleValue(), 2) - 1)));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ATANH", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /** Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x)) */
                if (Math.abs(parameters.get(0).doubleValue()) > 1 || Math.abs(parameters.get(0).doubleValue()) == 1) {
                    throw new ExpressionException("Number must be |x| < 1");
                }
                final double d = 0.5
                        * Math.log((1 + parameters.get(0).doubleValue()) / (1 - parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("RAD", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.toRadians(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("DEG", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.toDegrees(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("MAX", -1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                if (parameters.size() == 0) {
                    throw new ExpressionException("MAX requires at least one parameter");
                }
                BigDecimal max = null;
                for (final BigDecimal parameter : parameters) {
                    assertNotNull(parameter);
                    if (max == null || parameter.compareTo(max) > 0) {
                        max = parameter;
                    }
                }
                return max;
            }
        });
        addFunction(new Function("MIN", -1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                if (parameters.size() == 0) {
                    throw new ExpressionException("MIN requires at least one parameter");
                }
                BigDecimal min = null;
                for (final BigDecimal parameter : parameters) {
                    assertNotNull(parameter);
                    if (min == null || parameter.compareTo(min) < 0) {
                        min = parameter;
                    }
                }
                return min;
            }
        });
        addFunction(new Function("ABS", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                return parameters.get(0).abs(mc);
            }
        });
        addFunction(new Function("LOG", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.log(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("LOG10", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final double d = Math.log10(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ROUND", 2) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0), parameters.get(1));
                final BigDecimal toRound = parameters.get(0);
                final int precision = parameters.get(1).intValue();
                return toRound.setScale(precision, mc.getRoundingMode());
            }
        });
        addFunction(new Function("FLOOR", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.FLOOR);
            }
        });
        addFunction(new Function("CEILING", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                final BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.CEILING);
            }
        });
        addFunction(new Function("SQRT", 1) {
            @Override
            public BigDecimal eval(final List<BigDecimal> parameters) {
                assertNotNull(parameters.get(0));
                /*
                 * From The Java Programmers Guide To numerical Computing
                 * (Ronald Mak, 2003)
                 */
                final BigDecimal x = parameters.get(0);
                if (x.compareTo(BigDecimal.ZERO) == 0) {
                    return new BigDecimal(0);
                }
                if (x.signum() < 0) {
                    throw new ExpressionException("Argument to SQRT() function must not be negative");
                }
                final BigInteger n = x.movePointRight(mc.getPrecision() << 1).toBigInteger();

                final int bits = (n.bitLength() + 1) >> 1;
                BigInteger ix = n.shiftRight(bits);
                BigInteger ixPrev;
                BigInteger test;
                do {
                    ixPrev = ix;
                    ix = ix.add(n.divide(ix)).shiftRight(1);
                    // Give other threads a chance to work;
                    Thread.yield();
                    test = ix.subtract(ixPrev).abs();
                } while (test.compareTo(BigInteger.ZERO) != 0 && test.compareTo(BigInteger.ONE) != 0);

                return new BigDecimal(ix, mc.getPrecision());
            }
        });

        variables.put("e", CreateLazyNumber(e));
        variables.put("PI", CreateLazyNumber(PI));
        variables.put("NULL", null);
        variables.put("TRUE", CreateLazyNumber(BigDecimal.ONE));
        variables.put("FALSE", CreateLazyNumber(BigDecimal.ZERO));

    }

    private void assertNotNull(final BigDecimal v1) {
        if (v1 == null) {
            throw new ArithmeticException("Operand may not be null");
        }
    }

    private void assertNotNull(final BigDecimal v1, final BigDecimal v2) {
        if (v1 == null) {
            throw new ArithmeticException("First operand may not be null");
        }
        if (v2 == null) {
            throw new ArithmeticException("Second operand may not be null");
        }
    }

    /**
     * Is the string a number?
     *
     * @param st The string.
     * @return <code>true</code>, if the input string is a number.
     */
    private boolean isNumber(final String st) {
        if (st.charAt(0) == minusSign && st.length() == 1) {
            return false;
        }
        if (st.charAt(0) == '+' && st.length() == 1) {
            return false;
        }
        if (st.charAt(0) == decimalSeparator && (st.length() == 1 || !Character.isDigit(st.charAt(1)))) {
            return false;
        }
        if (st.charAt(0) == 'e' || st.charAt(0) == 'E') {
            return false;
        }
        for (final char ch : st.toCharArray()) {
            if (!Character.isDigit(ch) && ch != minusSign && ch != decimalSeparator && ch != 'e' && ch != 'E'
                    && ch != '+') {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix expression to a RPN expression.
     *
     * @param expression The input expression in infx.
     * @return A RPN representation of the expression, with each token as a list
     * member.
     */
    private List<Token> shuntingYard(final String expression) {
        final List<Token> outputQueue = new ArrayList<Token>();
        final Stack<Token> stack = new Stack<Token>();

        final Tokenizer tokenizer = new Tokenizer(expression);

        Token lastFunction = null;
        Token previousToken = null;
        while (tokenizer.hasNext()) {
            final Token token = tokenizer.next();
            switch (token.type) {
                case STRINGPARAM:
                    stack.push(token);
                    break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (previousToken.type == TokenType.LITERAL || previousToken.type == TokenType.HEX_LITERAL)) {
                        throw new ExpressionException("Missing operator at character position " + token.pos);
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
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty()) {
                        if (lastFunction == null) {
                            throw new ExpressionException("Unexpected comma at character position " + token.pos);
                        } else {
                            throw new ExpressionException(
                                    "Parse error for function '" + lastFunction + "' at character position " + token.pos);
                        }
                    }
                    break;
                case OPERATOR: {
                    if (previousToken != null
                            && (previousToken.type == TokenType.COMMA || previousToken.type == TokenType.OPEN_PAREN)) {
                        throw new ExpressionException(
                                "Missing parameter(s) for operator " + token + " at character position " + token.pos);
                    }
                    final LazyOperator o1 = operators.get(token.surface);
                    if (o1 == null) {
                        throw new ExpressionException("Unknown operator '" + token + "' at position " + (token.pos + 1));
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR: {
                    if (previousToken != null && previousToken.type != TokenType.OPERATOR
                            && previousToken.type != TokenType.COMMA && previousToken.type != TokenType.OPEN_PAREN) {
                        throw new ExpressionException(
                                "Invalid position for unary operator " + token + " at character position " + token.pos);
                    }
                    final LazyOperator o1 = operators.get(token.surface);
                    if (o1 == null) {
                        throw new ExpressionException(
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
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty()) {
                        throw new ExpressionException("Mismatched parentheses");
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
                throw new ExpressionException("Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(final List<Token> outputQueue, final Stack<Token> stack, final LazyOperator o1) {
        Expression.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Expression.TokenType.OPERATOR
                || nextToken.type == Expression.TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence()))) {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }


    /**
     * Evaluates the expression.
     *
     * @return The result of the expression. Trailing zeros are stripped.
     */
    public BigDecimal eval() {
        return eval(true);
    }

    /**
     * @param variablesToValue
     * @return
     */
    public BigDecimal eval(final Map<String, Object> variablesToValue) {
        for (final Map.Entry<String, Object> entry : variablesToValue.entrySet()) {
            setVariable(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return eval(true);
    }

    /**
     * Evaluates the expression.
     *
     * @param stripTrailingZeros If set to <code>true</code> trailing zeros in the result are
     *                           stripped.
     * @return The result of the expression.
     */
    public BigDecimal eval(final boolean stripTrailingZeros) {

        final Stack<LazyNumber> stack = new Stack<LazyNumber>();

        for (final Token token : getRPN()) {
            switch (token.type) {
                case UNARY_OPERATOR: {
                    final LazyNumber value = stack.pop();
                    final LazyNumber result = new LazyNumber() {
                        public BigDecimal eval() {
                            return operators.get(token.surface).eval(value, null).eval();
                        }

                        @Override
                        public String getString() {
                            return String.valueOf(operators.get(token.surface).eval(value, null).eval());
                        }
                    };
                    stack.push(result);
                    break;
                }
                case OPERATOR:
                    final LazyNumber v1 = stack.pop();
                    final LazyNumber v2 = stack.pop();
                    final LazyNumber result = new LazyNumber() {
                        public BigDecimal eval() {
                            return operators.get(token.surface).eval(v2, v1).eval();
                        }

                        public String getString() {
                            return String.valueOf(operators.get(token.surface).eval(v2, v1).eval());
                        }
                    };
                    stack.push(result);
                    break;
                case VARIABLE:
                    if (!variables.containsKey(token.surface)) {
                        throw new ExpressionException("Unknown operator or function: " + token);
                    }

                    stack.push(new LazyNumber() {
                        public BigDecimal eval() {
                            final LazyNumber lazyVariable = variables.get(token.surface);
                            final BigDecimal value = lazyVariable == null ? null : lazyVariable.eval();
                            return value == null ? null : value.round(mc);
                        }

                        public String getString() {
                            return token.surface;
                        }
                    });
                    break;
                case FUNCTION:
                    final com.udojava.evalex.LazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
                    final ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(!f.numParamsVaries() ? f.getNumParams() : 0);
                    // pop parameters off the stack until we hit the start of
                    // this function's parameter list
                    while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
                        p.add(0, stack.pop());
                    }

                    if (stack.peek() == PARAMS_START) {
                        stack.pop();
                    }

                    final LazyNumber fResult = f.lazyEval(p);
                    stack.push(fResult);
                    break;
                case OPEN_PAREN:
                    stack.push(PARAMS_START);
                    break;
                case LITERAL:
                    stack.push(new LazyNumber() {
                        public BigDecimal eval() {
                            if (token.surface.equalsIgnoreCase("NULL")) {
                                return null;
                            }

                            return new BigDecimal(token.surface, mc);
                        }

                        public String getString() {
                            return String.valueOf(new BigDecimal(token.surface, mc));
                        }
                    });
                    break;
                case STRINGPARAM:
                    stack.push(new LazyNumber() {
                        public BigDecimal eval() {
                            return null;
                        }

                        public String getString() {
                            return token.surface;
                        }
                    });
                    break;
                case HEX_LITERAL:
                    stack.push(new LazyNumber() {
                        public BigDecimal eval() {
                            return new BigDecimal(new BigInteger(token.surface.substring(2), 16), mc);
                        }

                        public String getString() {
                            return new BigInteger(token.surface.substring(2), 16).toString();
                        }
                    });
                    break;
                default:
                    throw new ExpressionException(
                            "Unexpected token '" + token.surface + "' at character position " + token.pos);
            }
        }
        final BigDecimal result = stack.pop().eval();
        return result == null ? null : stripTrailingZeros ? result.stripTrailingZeros() : result;
    }

    /**
     * Sets the precision for expression evaluation.
     *
     * @param precision The new precision.
     * @return The expression, allows to chain methods.
     */
    public Expression setPrecision(final int precision) {
        mc = new MathContext(precision);
        return this;
    }

    /**
     * Sets the rounding mode for expression evaluation.
     *
     * @param roundingMode The new rounding mode.
     * @return The expression, allows to chain methods.
     */
    public Expression setRoundingMode(final RoundingMode roundingMode) {
        mc = new MathContext(mc.getPrecision(), roundingMode);
        return this;
    }

    /**
     * Sets the characters other than letters and digits that are valid as the
     * first character of a variable.
     *
     * @param chars The new set of variable characters.
     * @return The expression, allows to chain methods.
     */
    public Expression setFirstVariableCharacters(final String chars) {
        firstVarChars = chars;
        return this;
    }

    /**
     * Sets the characters other than letters and digits that are valid as the
     * second and subsequent characters of a variable.
     *
     * @param chars The new set of variable characters.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariableCharacters(final String chars) {
        varChars = chars;
        return this;
    }

    /**
     * Adds an operator to the list of supported operators.
     *
     * @param operator The operator to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public <OPERATOR extends LazyOperator> OPERATOR addOperator(final OPERATOR operator) {
        String key = operator.getOper();
        if (operator instanceof AbstractUnaryOperator) {
            key += "u";
        }
        return (OPERATOR) operators.put(key, operator);
    }

    /**
     * Adds a function to the list of supported functions
     *
     * @param function The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public com.udojava.evalex.Function addFunction(final com.udojava.evalex.Function function) {
        return (com.udojava.evalex.Function) functions.put(function.getName(), function);
    }

    /**
     * Adds a lazy function function to the list of supported functions
     *
     * @param function The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public com.udojava.evalex.LazyFunction addLazyFunction(final com.udojava.evalex.LazyFunction function) {
        return functions.put(function.getName(), function);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable name.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(final String variable, final BigDecimal value) {
        return setVariable(variable, CreateLazyNumber(value));
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable name.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(final String variable, final LazyNumber value) {
        variables.put(variable, value);
        return this;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(final String variable, final String value) {
        if (isNumber(value)) {
            variables.put(variable, CreateLazyNumber(new BigDecimal(value, mc)));
        } else if (value.equalsIgnoreCase("null")) {
            variables.put(variable, null);
        } else {
            final String expStr = value;
            variables.put(variable, new LazyNumber() {
                private final Map<String, LazyNumber> outerVariables = variables;
                private final Map<String, com.udojava.evalex.LazyFunction> outerFunctions = functions;
                private final Map<String, LazyOperator> outerOperators = operators;
                private final String innerExpressionString = expStr;
                private final MathContext inneMc = mc;

                @Override
                public String getString() {
                    return innerExpressionString;
                }

                @Override
                public BigDecimal eval() {
                    final Expression innerE = new Expression(innerExpressionString, inneMc);
                    innerE.variables = outerVariables;
                    innerE.functions = outerFunctions;
                    innerE.operators = outerOperators;
                    final BigDecimal val = innerE.eval();
                    return val;
                }
            });
            rpn = null;
        }
        return this;
    }

    /**
     * Creates a new inner expression for nested expression.
     *
     * @param expression The string expression.
     * @return The inner Expression instance.
     */
    private Expression createEmbeddedExpression(final String expression) {
        final Map<String, LazyNumber> outerVariables = variables;
        final Map<String, com.udojava.evalex.LazyFunction> outerFunctions = functions;
        final Map<String, LazyOperator> outerOperators = operators;
        final MathContext inneMc = mc;
        final Expression exp = new Expression(expression, inneMc);
        exp.variables = outerVariables;
        exp.functions = outerFunctions;
        exp.operators = outerOperators;
        return exp;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression with(final String variable, final BigDecimal value) {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression with(final String variable, final LazyNumber value) {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression and(final String variable, final String value) {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression and(final String variable, final BigDecimal value) {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression and(final String variable, final LazyNumber value) {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression with(final String variable, final String value) {
        return setVariable(variable, value);
    }

    /**
     * Get an iterator for this expression, allows iterating over an expression
     * token by token.
     *
     * @return A new iterator instance for this expression.
     */
    public Iterator<Token> getExpressionTokenizer() {
        final String expression = this.expression;

        return new Tokenizer(expression);
    }

    /**
     * Cached access to the RPN notation of this expression, ensures only one
     * calculation of the RPN per expression instance. If no cached instance
     * exists, a new one will be created and put to the cache.
     *
     * @return The cached RPN instance.
     */
    private List<Token> getRPN() {
        if (rpn == null) {
            rpn = shuntingYard(expression);
            validate(rpn);
        }
        return rpn;
    }

    /**
     * Check that the expression has enough numbers and variables to fit the
     * requirements of the operators and functions, also check for only 1 result
     * stored at the end of the evaluation.
     */
    private void validate(final List<Token> rpn) {
        /*-
         * Thanks to Norman Ramsey:
         * http://http://stackoverflow.com/questions/789847/postfix-notation-validation
         */
        // each push on to this stack is a new function scope, with the value of
        // each
        // layer on the stack being the count of the number of parameters in
        // that scope
        final Stack<Integer> stack = new Stack<Integer>();

        // push the 'global' scope
        stack.push(0);

        for (final Token token : rpn) {
            switch (token.type) {
                case UNARY_OPERATOR:
                    if (stack.peek() < 1) {
                        throw new ExpressionException("Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.peek() < 2) {
                        throw new ExpressionException("Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    final com.udojava.evalex.LazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
                    if (f == null) {
                        throw new ExpressionException("Unknown function '" + token + "' at position " + (token.pos + 1));
                    }

                    final int numParams = stack.pop();
                    if (!f.numParamsVaries() && numParams != f.getNumParams()) {
                        throw new ExpressionException(
                                "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    }
                    if (stack.size() <= 0) {
                        throw new ExpressionException("Too many function calls, maximum scope exceeded");
                    }
                    // push the result of the function
                    stack.set(stack.size() - 1, stack.peek() + 1);
                    break;
                case OPEN_PAREN:
                    stack.push(0);
                    break;
                default:
                    stack.set(stack.size() - 1, stack.peek() + 1);
            }
        }

        if (stack.size() > 1) {
            throw new ExpressionException("Too many unhandled function parameter lists");
        } else if (stack.peek() > 1) {
            throw new ExpressionException("Too many numbers or variables");
        } else if (stack.peek() < 1) {
            throw new ExpressionException("Empty expression");
        }
    }

    /**
     * Get a string representation of the RPN (Reverse Polish Notation) for this
     * expression.
     *
     * @return A string with the RPN representation for this expression.
     */
    public String toRPN() {
        final StringBuilder result = new StringBuilder();
        for (final Token t : getRPN()) {
            if (result.length() != 0) {
                result.append(" ");
            }
            if (t.type == TokenType.VARIABLE && variables.containsKey(t.surface)) {
                final LazyNumber innerVariable = variables.get(t.surface);
                final String innerExp = innerVariable.getString();
                if (isNumber(innerExp)) { // if it is a number, then we don't
                    // expan in the RPN
                    result.append(t.toString());
                } else { // expand the nested variable to its RPN representation
                    final Expression exp = createEmbeddedExpression(innerExp);
                    final String nestedExpRpn = exp.toRPN();
                    result.append(nestedExpRpn);
                }
            } else {
                result.append(t.toString());
            }
        }
        return result.toString();
    }

    /**
     * Exposing declared variables in the expression.
     *
     * @return All declared variables.
     */
    public Set<String> getDeclaredVariables() {
        return Collections.unmodifiableSet(variables.keySet());
    }

    /**
     * Exposing declared operators in the expression.
     *
     * @return All declared operators.
     */
    public Set<String> getDeclaredOperators() {
        return Collections.unmodifiableSet(operators.keySet());
    }

    /**
     * Exposing declared functions.
     *
     * @return All declared functions.
     */
    public Set<String> getDeclaredFunctions() {
        return Collections.unmodifiableSet(functions.keySet());
    }

    /**
     * @return The original expression string
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns a list of the variables in the expression.
     *
     * @return A list of the variable names in this expression.
     */
    public List<String> getUsedVariables() {
        final List<String> result = new ArrayList<String>();
        final Tokenizer tokenizer = new Tokenizer(expression);
        while (tokenizer.hasNext()) {
            final Token nextToken = tokenizer.next();
            final String token = nextToken.toString();
            if (nextToken.type != TokenType.VARIABLE || token.equals("PI") || token.equals("e") || token.equals("TRUE")
                    || token.equals("FALSE")) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    /**
     * return a set of uniqueness
     *
     * @return
     */
    public Set<String> getUsedVariablesSet() {
        final Set<String> result = new HashSet<String>();
        final Tokenizer tokenizer = new Tokenizer(expression);
        while (tokenizer.hasNext()) {
            final Token nextToken = tokenizer.next();
            final String token = nextToken.toString();
            if (nextToken.type != TokenType.VARIABLE || token.equals("PI") || token.equals("e") || token.equals("TRUE")
                    || token.equals("FALSE")) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    /**
     * The original expression used to construct this expression, without
     * variables substituted.
     */
    public String getOriginalExpression() {
        return originalExpression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Expression that = (Expression) o;
        if (expression == null) {
            return that.expression == null;
        } else {
            return expression.equals(that.expression);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return expression == null ? 0 : expression.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return expression;
    }

    /**
     * Checks whether the expression is a boolean expression. An expression is
     * considered a boolean expression, if the last operator or function is
     * boolean. The IF function is handled special. If the third parameter is
     * boolean, then the IF is also considered boolean, else non-boolean.
     *
     * @return <code>true</code> if the last operator/function was a boolean.
     */
    public boolean isBoolean() {
        final List<Token> rpn = getRPN();
        if (rpn.size() > 0) {
            for (int i = rpn.size() - 1; i >= 0; i--) {
                final Token t = rpn.get(i);
                /*
                 * The IF function is handled special. If the third parameter is
                 * boolean, then the IF is also considered a boolean. Just skip
                 * the IF function to check the second parameter.
                 */
                if (t.surface.equals("IF")) {
                    continue;
                }
                if (t.type == TokenType.FUNCTION) {
                    return functions.get(t.surface).isBooleanFunction();
                } else if (t.type == TokenType.OPERATOR) {
                    return operators.get(t.surface).isBooleanOperator();
                }
            }
        }
        return false;
    }

}

