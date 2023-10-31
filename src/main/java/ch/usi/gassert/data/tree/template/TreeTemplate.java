package ch.usi.gassert.data.tree.template;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.builder.ITreeBuilder;

import java.util.*;

/**
 * This is the data structure that represents the structure of a generated Tree.
 */
public class TreeTemplate {

    /**
     * STATIC nodes contain the type and value that the node should have.
     * GENERATED nodes contain the type of the node, and value contains the ITreeBuilder that should be used
     * to generate and evolve the Trees to insert in their location.
     * GENERATED nodes are always leaf nodes in the template.
     */
    public enum Mode {
        STATIC, GENERATED
    }

    /* Fields required for all modes */
    private final Mode mode;
    private final Tree.Type type;
    private final Object value; // Node value for STATIC nodes, ITreeBuilder for GENERATED nodes

    /* Fields required only for STATIC nodes */
    private final TreeTemplate left, right;

    /* Memoized set of GENERATED nodes */
    private transient Set<TreeTemplate> generatedNodes;

    public TreeTemplate(final Object value, final Tree.Type type, final Mode mode) {
        this(value, null, null, type, mode);
    }

    public TreeTemplate(final Object value, final TreeTemplate left, final TreeTemplate right,
                        final Tree.Type type, final Mode mode) {
        this.value = value;
        this.left = left;
        this.right = right;
        this.type = type;
        this.mode = mode;
    }

    public TreeTemplate(final TreeTemplate other) {
        this.value = other.value;
        this.left = other.left == null ? null : new TreeTemplate(other.left);
        this.right = other.right == null ? null : new TreeTemplate(other.right);
        this.type = other.type;
        this.mode = other.mode;
        if (other.generatedNodes != null) {
            this.generatedNodes = new HashSet<>(other.getGeneratedNodes());
        }
    }

    /**
     * Store the output of getGeneratedNodes so that subsequent calls have no overhead.
     * Must be called again if the TreeTemplate is changed.
     */
    public void memoizeGeneratedNodes() {
        this.generatedNodes = getGeneratedNodes();
    }

    /**
     * Get the set of nodes that need to be generated.
     * The map passed to the buildTree method must contain values for exactly this set of keys.
     */
    public Set<TreeTemplate> getGeneratedNodes() {
        if (this.generatedNodes != null) {
            return this.generatedNodes;
        }
        Set<TreeTemplate> generatedNodes = new HashSet<>();
        getGeneratedNodes(generatedNodes);
        return generatedNodes;
    }

    /**
     * Add the set of nodes that need to be generated to the input set.
     * The map passed to the buildTree method must contain values for exactly this set of keys.
     */
    public void getGeneratedNodes(Set<TreeTemplate> generatedNodes) {
        if (this.generatedNodes != null) {
            generatedNodes.addAll(this.generatedNodes);
        } else if (mode == Mode.STATIC) {
            if (!isLeaf()) {
                left.getGeneratedNodes(generatedNodes);
                right.getGeneratedNodes(generatedNodes);
            }
        } else if (mode == Mode.GENERATED) {
            generatedNodes.add(this);
        } else {
            throw new RuntimeException("Unsupported TreeTemplate mode");
        }
    }

    /**
     * Build the final Tree following this template and the generated subtrees passed as input.
     * generatedTrees must contain mappings for all the nodes in the set returned by the getGeneratedNodes method.
     */
    public Tree buildTree(TreeGroup generatedTrees) {
        return buildTree(generatedTrees.mappings);
    }

    /**
     * Build the final Tree following this template and the generated subtrees passed as input.
     * generatedTrees must contain mappings for all the nodes in the set returned by the getGeneratedNodes method.
     */
    public Tree buildTree(Map<TreeTemplate, ITree> generatedTrees) {
        if (mode == Mode.STATIC) {
            return isLeaf() ? new Tree(value, type)
                    : new Tree(value, left.buildTree(generatedTrees), right.buildTree(generatedTrees), type);
        } else if (mode == Mode.GENERATED) {
            return generatedTrees.get(this).asTree();
        } else {
            throw new RuntimeException("Unsupported TreeTemplate mode");
        }
    }

    /**
     * Match the given Tree with this template, resulting in a TreeGroup.
     */
    public TreeGroup match(final Tree tree) {
        final Map<TreeTemplate, ITree> mappings = new HashMap<>();
        match(this, tree, mappings);
        return new TreeGroup(this, mappings);
    }

    /**
     * Match the given Tree with this template and adds the mappings to the given collection.
     * Throws an exception if the template cannot be matched or if there are multiple ways to match it.
     */
    private static void match(final TreeTemplate treeTemplate, final Tree tree, final Map<TreeTemplate, ITree> mappings) {
        if (treeTemplate.getMode() == Mode.STATIC) {
            if (tree.getValue().equals(treeTemplate.getValue()) && tree.getType() == treeTemplate.getType()) {
                if (treeTemplate.getLeft() != null) {
                    match(treeTemplate.getLeft(), tree.getLeft(), mappings);
                } else if (tree.getLeft() != null) {
                    throw new RuntimeException("Cannot match template");
                }
                if (treeTemplate.getRight() != null) {
                    match(treeTemplate.getRight(), tree.getRight(), mappings);
                } else if (tree.getRight() != null) {
                    throw new RuntimeException("Cannot match template");
                }
            } else {
                throw new RuntimeException("Cannot match template");
            }
        } else if (treeTemplate.getMode() == Mode.GENERATED) {
            if (tree.getType() == treeTemplate.getType()) {
                mappings.put(treeTemplate, treeTemplate.getTreeBuilder().matchTree(tree));
            } else {
                throw new RuntimeException("Cannot match template");
            }
        } else {
            throw new RuntimeException("Unsupported TreeTemplate mode");
        }
    }

    public Mode getMode() {
        return mode;
    }

    public Tree.Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public ITreeBuilder<?> getTreeBuilder() {
        return (ITreeBuilder<?>) value; // Will fail on nodes that are not GENERATED
    }

    public TreeTemplate getLeft() {
        return left;
    }

    public TreeTemplate getRight() {
        return right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeTemplate that = (TreeTemplate) o;
        return mode == that.mode
                && type == that.type
                && value.equals(that.value)
                && Objects.equals(left, that.left)
                && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, type, value, left, right);
    }

    @Override
    public String toString() {
        final String thisNode = mode == Mode.GENERATED ? "$" + type.toString() + "_EXPR$" : value.toString();
        if (left == null && right == null) {
            if (value instanceof Float || value instanceof Double) {
                return Config.DECIMAL_FORMAT.format(value);
            } else {
                return thisNode;
            }
        }
        return "(" + (left != null ? left.toString() : "") +
                " " + thisNode + " " + (right != null ? right.toString() : "") + ")";
    }

}
