package ch.usi.gassert.data.tree;

import ch.usi.gassert.Config;
import ch.usi.gassert.Constants;
import ch.usi.gassert.Functions;
import ch.usi.gassert.data.state.IVariablesManager;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.types.Sequence;
import ch.usi.gassert.util.random.MyRandom;

import java.util.*;

import static ch.usi.gassert.Functions.*;

/**
 * This class creates a Tree from scratch
 */
public class TreeFactory {

    public static final TreeFactory EMPTY = new TreeFactory(VariablesManager.EMPTY);

    private final IVariablesManager variablesManager;

    public Set<String> getBooleanVars() {
        return variablesManager.getBooleanVars();
    }

    public Set<String> getNumberVars() {
        return variablesManager.getNumericVars();
    }

    public Set<String> getSequenceVars() {
        return variablesManager.getSequenceVars();
    }

    //ALSO STRING VARS

    public TreeFactory(IVariablesManager variablesManager) {
        this.variablesManager = variablesManager;
    }

    public Tree buildTree(final Tree.Type type, final int depth, final float probConstant) {
        return buildTree(type, depth, MyRandom.getInstance().nextBoolean(), probConstant);
    }

    public Tree buildTree(final Tree.Type type, final int depth, final boolean isFull, final float probConstant) {
        final int depth0 = Math.max(depth, 0);
        return isFull ? full(type, depth0, probConstant) : grow(type, depth0, probConstant);
    }


    private Tree full(final Tree.Type type, final int depth, final float probConstant) {
        return create(type, depth, true, true, probConstant);
    }

    private Tree grow(final Tree.Type type, final int depth, final float probConstant) {
        return create(type, depth, true, false, probConstant);
    }


    private Tree create(final Tree.Type type, final int depth, final boolean mustVar, final boolean isFull, final float probConstant) {
        if (depth > 1 && (isFull || MyRandom.getInstance().nextBoolean())) {
            return createTree(type, depth, mustVar, isFull, probConstant);
        } else {
            return createLeaf(type, mustVar);
        }
    }

    public Number getRandomConstantNumber() {
        return MyRandom.getInstance().getRandomElementList(Constants.numericConstants);
    }

    public float getRandomSmallNumber() {
        MyRandom rng = MyRandom.getInstance();
        final float sign = (rng.nextFloat() <= Config.PROB_NEGATIVE ? -1 : 1);
        final float multiplier = rng.nextFloat();
        final float result = sign * multiplier;
        return Float.parseFloat(Config.DECIMAL_FORMAT.format(result));
    }

    public float getRandomLargeNumber() {
        return getRandomSmallNumber() * Config.MAX_NUMBER;
    }

    public long getRandomIntegerNumber() {
        return Math.round(Math.ceil(getRandomLargeNumber()));
    }

    public Number getRandomNumber() {
        MyRandom rng = MyRandom.getInstance();
        final Number result;
        if (rng.nextFloat() <= Config.PROB_MAGIC_CONSTANT) {
            result = getRandomConstantNumber();
        } else if (rng.nextFloat() <= Config.PROB_INTEGER_CONSTANT) {
            result = getRandomIntegerNumber();
        } else if (rng.nextFloat() <= Config.PROB_LARGE_CONSTANT) {
            result = getRandomLargeNumber();
        } else {
            result = getRandomSmallNumber();
        }
        return Float.parseFloat(Config.DECIMAL_FORMAT.format(result));
    }

    private Tree createTree(final Tree.Type type, final int depth, final boolean mustVar, final boolean isFull, final float probConstant) {
        final String operator;
        final Tree.Type typeLeft, typeRight;
        boolean isUnary = false;
        if (type.equals(Tree.Type.BOOLEAN)) {
            if (MyRandom.getInstance().nextFloat() < Config.PROB_BOOL_WITH_MATH) {
                operator = MyRandom.getInstance().getRandomElementList(functionsMathReturnsBoolean);
                typeLeft = typeRight = Tree.Type.NUMBER;
            } else if (!getSequenceVars().isEmpty() && MyRandom.getInstance().nextFloat() < Config.PROB_BOOL_WITH_SEQUENCE) {
                operator = MyRandom.getInstance().getRandomElementList(functionsBinarySequenceReturnsBoolean);
                typeLeft = typeRight = Tree.Type.SEQUENCE;
            } else {
                if (MyRandom.getInstance().nextFloat() < Config.PROB_UNARY) {
                    isUnary = true;
                    operator = MyRandom.getInstance().getRandomElementList(functionsUnaryBooleanReturnsBoolean);
                } else {
                    operator = MyRandom.getInstance().getRandomElementList(functionsBooleanReturnsBoolean);
                }
                typeLeft = typeRight = Tree.Type.BOOLEAN;
            }
        } else if (type.equals(Tree.Type.NUMBER)) {
            if (!getSequenceVars().isEmpty() && MyRandom.getInstance().nextFloat() < Config.PROB_MATH_WITH_SEQUENCE && MyRandom.getInstance().nextFloat() < Config.PROB_UNARY) {
                isUnary = true;
                operator = MyRandom.getInstance().getRandomElementList(functionsUnarySequenceReturnsMath);
                typeLeft = typeRight = Tree.Type.SEQUENCE;
            } else {
                if (MyRandom.getInstance().nextFloat() < Config.PROB_UNARY) {
                    isUnary = true;
                    operator = MyRandom.getInstance().getRandomElementList(functionsUnaryMathReturnsMath);
                } else {
                    operator = MyRandom.getInstance().getRandomElementList(functionsMathReturnsMath);
                }
                typeLeft = typeRight = Tree.Type.NUMBER;
            }
        } else if (type.equals(Tree.Type.SEQUENCE)) {
            if (MyRandom.getInstance().nextFloat() < Config.PROB_UNARY) {
                isUnary = true;
                if (MyRandom.getInstance().nextFloat() < Config.PROB_SEQUENCE_WITH_MATH) {
                    operator = MyRandom.getInstance().getRandomElementList(functionsUnaryMathReturnsSequence);
                    typeLeft = typeRight = Tree.Type.NUMBER;
                } else {
                    operator = MyRandom.getInstance().getRandomElementList(functionsUnarySequenceReturnsSequence);
                    typeLeft = typeRight = Tree.Type.SEQUENCE;
                }
            } else {
                operator = MyRandom.getInstance().getRandomElementList(functionsBinarySequenceMathReturnsSequence);
                typeLeft = Tree.Type.SEQUENCE;
                typeRight = Tree.Type.NUMBER;
            }
        } else {
            throw new RuntimeException("Unsupported Tree type: " + type);
        }
        // make sure the lef operand is always a var
        final Tree left = create(typeLeft, depth - 1, mustVar, isFull, probConstant);
        if (isUnary) {
            if (operator.equals(left.getValue())) {
                // Special cases: remove redundancies
                if (Functions.isSymmetric(operator)) {
                    // Applying the operation a second time cancels its effect
                    return left.getLeft();
                } else if (Functions.isReentrant(operator)) {
                    // Applying the operation a second time does nothing
                    return left;
                }
            }
            return new Tree(operator, left, null, type);
        } else {
            Tree right = create(typeRight, depth - 1, MyRandom.getInstance().nextFloat() > probConstant, isFull, probConstant);
            return new Tree(operator, left, right, type);
        }
    }

    public Tree createLeaf(final Tree.Type type, final boolean mustVar) {
        if (type.equals(Tree.Type.NUMBER)) {
            if (mustVar && !getNumberVars().isEmpty()) {
                return new Tree(MyRandom.getInstance().getRandomElementCollection(getNumberVars()), type);
            } else {
                return new Tree(getRandomNumber(), type);
            }
        } else if (type.equals(Tree.Type.BOOLEAN)) {
            if (mustVar && !getBooleanVars().isEmpty()) {
                return new Tree(MyRandom.getInstance().getRandomElementCollection(getBooleanVars()), type);
            } else {
                return new Tree(MyRandom.getInstance().nextBoolean(), type);
            }
        } else if (type.equals(Tree.Type.SEQUENCE)) {
            if (!getSequenceVars().isEmpty()) {
                return new Tree(MyRandom.getInstance().getRandomElementCollection(getSequenceVars()), type);
            } else {
                throw new RuntimeException("Sequence literals not supported");
            }
        } else {
            throw new RuntimeException("Unsupported Tree type: " + type);
        }
    }
}



