package ch.usi.gassert.util;

import ch.usi.gassert.data.tree.Tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class MR {

    public static String VARIABLE_TO_SOURCE(String v) {
        return v + "_s";
    }

    public static String VARIABLE_TO_FOLLOWUP(String v) {
        return v + "_f";
    }

    public static String SOURCE_OR_FOLLOWUP_TO_VARIABLE(String v) {
        return v.endsWith("_s") || v.endsWith("_f") ? v.substring(0, v.length() - 2) : null;
    }

    public static String SOURCE_TO_VARIABLE(String v) {
        return v.endsWith("_s") ? v.substring(0, v.length() - 2) : null;
    }

    public static String FOLLOWUP_TO_VARIABLE(String v) {
        return v.endsWith("_f") ? v.substring(0, v.length() - 2) : null;
    }

    public static boolean IS_OUTPUT_VARIABLE(String v) {
        return v.startsWith("o_");
    }

    public static String JOIN_SOURCE_FOLLOWUP(String source, String followup) {
        return source + ":" + followup;
    }

    public static String[] SPLIT_SOURCE_FOLLOWUP(String row) {
        return row.split(":");
    }

    /**
     * Analyze whether the given Tree represents a metamorphic output relation.
     *
     * An expression must use at least one output variable from both the source and follow-up test executions
     * in order for it to be considered a metamorphic output relation. This means that the Tree must contain
     * any two variables "o_[X]_s" and "o_[X]_f", for any value of "[X]".
     *
     * @param tree The expression Tree to analyze.
     * @return A boolean representing whether the Tree is a metamorphic output relation.
     */
    public static boolean isMetamorphicOutputRelation(final Tree tree) {
        final Deque<Tree> toExplore = new ArrayDeque<>(64);
        toExplore.push(tree);
        final Set<String> variablesSource = new HashSet<>(16);
        final Set<String> variablesFollowup = new HashSet<>(16);
        while (!toExplore.isEmpty()) {
            final Tree currentNode = toExplore.pollLast();
            if (currentNode == null || !currentNode.hasVariables()) {
                // Fast path: Skip subtrees with no variables
                continue;
            }
            if (currentNode.isLeaf()) {
                final String value = currentNode.getValue().toString();
                if (IS_OUTPUT_VARIABLE(value)) {
                    String variableName = SOURCE_TO_VARIABLE(value);
                    if (variableName != null) {
                        if (variablesFollowup.contains(variableName)) {
                            return true;
                        }
                        variablesSource.add(variableName);
                    } else {
                        variableName = FOLLOWUP_TO_VARIABLE(value);
                        if (variableName != null) {
                            if (variablesSource.contains(variableName)) {
                                return true;
                            }
                            variablesFollowup.add(variableName);
                        }
                    }
                }
            } else {
                if (currentNode.getLeft() != null) {
                    toExplore.push(currentNode.getLeft());
                }
                if (currentNode.getRight() != null) {
                    toExplore.push(currentNode.getRight());
                }
            }
        }
        return false;
    }

}
