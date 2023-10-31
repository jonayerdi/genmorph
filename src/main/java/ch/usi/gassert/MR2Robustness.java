package ch.usi.gassert;

import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.*;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.util.LazyMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

/**
 * This class prints the robustness function for a given MR (assertion).
 */
public class MR2Robustness {

    public static final VariablesManager noVariables = VariablesManager.fromVariableTypes(new HashMap<>(0));
    public static final Function<String, TreeTemplate> makeComparisonTemplate = operator -> new TreeTemplate(operator,
            new TreeTemplate(new TreeFactory(noVariables), Tree.Type.NUMBER, TreeTemplate.Mode.GENERATED),
            new TreeTemplate(new TreeFactory(noVariables), Tree.Type.NUMBER, TreeTemplate.Mode.GENERATED),
            Tree.Type.BOOLEAN, TreeTemplate.Mode.STATIC
    );
    public static final TreeTemplate[] treeTemplates = {
            makeComparisonTemplate.apply("<="),
            makeComparisonTemplate.apply(">="),
            makeComparisonTemplate.apply("<"),
            makeComparisonTemplate.apply(">")
    };

    public static Tree makeRobustnessTree(final TreeGroup comparisonTree) {
        // Replace comparison with subtraction and reorder so that the expected larger value is on the left
        final String operator = (String) comparisonTree.treeTemplate.getValue();
        final Tree leftSide = comparisonTree.mappings.get(comparisonTree.treeTemplate.getLeft()).asTree();
        final Tree rightSide = comparisonTree.mappings.get(comparisonTree.treeTemplate.getRight()).asTree();
        Tree tree;
        switch (operator) {
            case "<":
            case "<=":
                tree = new Tree("-", rightSide, leftSide, Tree.Type.NUMBER);
                break;
            case ">":
            case ">=":
                tree = new Tree("-", leftSide, rightSide, Tree.Type.NUMBER);
                break;
            default: throw new RuntimeException("Unsupported operator: " + operator);
        }
        // Normalize the robustness by dividing it by left side
        tree = new Tree("/", tree, new Tree(leftSide), Tree.Type.NUMBER);
        return tree;
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Wrong number of parameters: 1 argument expected, got " + args.length);
            System.err.println("MR (assertion)");
            System.exit(-1);
        }
        final Tree tree = TreeReaderGAssert.getTree(args[0], new LazyMap<>(ignored -> Integer.class));
        TreeGroup treeGroup = Arrays.stream(treeTemplates)
                .map(t -> {
                    try {
                        return t.match(tree);
                    }catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Assertion does not match any comparison template"));
        Tree robustnessTree = makeRobustnessTree(treeGroup);
        System.out.println(robustnessTree);
    }

}
