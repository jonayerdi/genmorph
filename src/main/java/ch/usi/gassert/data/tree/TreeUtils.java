package ch.usi.gassert.data.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class TreeUtils {

    public static void visitLeafNodes(final Tree root, final Consumer<Tree> visitor) {
        final List<Tree> nodes = new ArrayList<>();
        nodes.add(root);
        while (!nodes.isEmpty()) {
            final Tree node = nodes.remove(nodes.size() - 1);
            if (node != null) {
                if (node.isLeaf()) {
                    visitor.accept(node);
                } else {
                    nodes.add(node.getLeft());
                    nodes.add(node.getRight());
                }
            }
        }
    }

    public static List<Tree> getConstantNodes(final Tree root) {
        final List<Tree> constantNodes = new ArrayList<>();
        visitLeafNodes(root, node -> {
            String value = node.getValue().toString();
            if (TreeEval.isLiteral(value)) {
                constantNodes.add(node);
            }
        });
        return constantNodes;
    }

    public static List<Tree> getVariableNodes(final Tree root) {
        final List<Tree> variableNodes = new ArrayList<>();
        visitLeafNodes(root, node -> {
            String value = node.getValue().toString();
            if (!TreeEval.isLiteral(value)) {
                variableNodes.add(node);
            }
        });
        return variableNodes;
    }

    public static Set<String> getUsedVariables(final Tree root) {
        final Set<String> variables = new HashSet<>();
        visitLeafNodes(root, node -> {
            String value = node.getValue().toString();
            if (!TreeEval.isLiteral(value)) {
                variables.add((String)node.getValue());
            }
        });
        return variables;
    }

}
