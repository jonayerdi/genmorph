package ch.usi.gassert.data.tree;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.TreeList;

import java.util.Objects;

/**
 * This is the data structure that represents the assertion
 */
public class Tree implements ITree {

    /**
     * the value can be either a terminals or an operation
     */
    private Object value;
    /**
     * left and right are null in case of terminals
     */
    private Tree left, right;

    public void fixAmbiguity() {
        if (value.equals("==")) {
            if (left.getType().equals(Type.BOOLEAN) && right.getType().equals(Type.BOOLEAN)) {
                value = "<=>";
                type = Type.BOOLEAN;
            }
        }
        if (left != null) {
            left.fixAmbiguity();
        }
        if (right != null) {
            right.fixAmbiguity();
        }
    }

    public boolean isCorrupted() {
        if (left != null && right != null) {
            if (!left.getType().equals(right.getType())) {
                return true;
            }
        }
        boolean res = false;
        if (left != null) {
            res = left.isCorrupted();
        }
        if (right != null) {
            res = right.isCorrupted() || res;
        }
        return res;
    }


    public enum Type {
        BOOLEAN, NUMBER, SEQUENCE;

        public static Type fromString(String str) {
            switch (str) {
                case "BOOLEAN":
                    return BOOLEAN;
                case "NUMBER":
                    return NUMBER;
                case "SEQUENCE":
                    return SEQUENCE;
                default:
                    throw new RuntimeException("Unknown Tree.Type enum value: " + str);
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case BOOLEAN:
                    return "BOOLEAN";
                case NUMBER:
                    return "NUMBER";
                case SEQUENCE:
                    return "SEQUENCE";
                default:
                    throw new RuntimeException("Unknown Tree.Type enum value");
            }
        }
    }

    /**
     * listOfnodes
     */
    //TODO we need to cache them
    //private TreeList listOfNodes;

    /**
     * the type of the node
     */
    private Type type;

    /**
     * create a terminal node
     *
     * @param value
     * @param type
     */
    public Tree(final Object value, final Type type) {
        this(value, null, null, type);
    }

    /**
     * create a Tree element
     *
     * @param value
     * @param left
     * @param right
     * @param type
     */
    public Tree(final Object value, final Tree left, final Tree right, final Type type) {
        this.value = value;
        this.left = left;
        this.right = right;
        this.type = type;
    }


    public Tree(final Tree root) {
        this.value = root.value;
        this.left = root.left == null ? null : new Tree(root.left);
        this.right = root.right == null ? null : new Tree(root.right);
        this.type = root.type;
    }


    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public Tree getLeft() {
        return left;
    }

    public Tree getRight() {
        return right;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public TreeList getListOfNodes() {
        return new TreeList(this);
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public void substitute(final Tree orig, final Tree replacement) {
        if (right == orig) {
            right = replacement;
        } else if (left == orig) {
            left = replacement;
        } else {
            throw new RuntimeException("something is wrong here");
        }
    }

    public Integer getNumberOfNodes() {
        return this.isLeaf() ? 1 : 1 + (left != null ? left.getNumberOfNodes() : 0) + (right != null ? right.getNumberOfNodes() : 0);
    }

    public boolean hasVariables() {
        return this.isLeaf() ? !TreeEval.isLiteral(value.toString()) : (left != null && left.hasVariables()) || (right != null && right.hasVariables());
    }

    @Override
    public Tree asTree() {
        return this;
    }

    @Override
    public Tree cloneTree() {
        return new Tree(this);
    }

    public static boolean isIdentifier(String value) {
        if (TreeEval.isLiteral(value)) {
            return false;
        }
        final char ch = value.charAt(0);
        return Character.isLetter(ch) || ch == '_';
    }

    @Override
    public String toString() {
        if (this.isLeaf()) {
            if (value.toString().contains("ch.usi.gassert.util.Implies.implies")) {
                return "(" + value.toString().replace(", ", ",") + ")";
            } else if (value instanceof Float || value instanceof Double) {
                return Config.DECIMAL_FORMAT.format(value);
            } else {
                return value.toString();
            }
        } else if (isIdentifier(value.toString())) {
            return value.toString() + "(" 
                + left.toString() 
                + (right != null ? (", " + right.toString()) : "") 
                + ")";
        } else {
            return "(" + (left != null ? left.toString() : "") +
                " " + value.toString() + " " + (right != null ? right.toString() : "") + ")";
        }
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tree)) {
            return false;
        }
        final Tree tree = (Tree) o;
        return getType() == tree.getType() &&
                Objects.equals(getValue(), tree.getValue()) &&
                Objects.equals(getLeft(), tree.getLeft()) &&
                Objects.equals(getRight(), tree.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), getLeft(), getRight(), getType());
    }
}




