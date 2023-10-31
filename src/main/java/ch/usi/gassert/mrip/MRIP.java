package ch.usi.gassert.mrip;

import ch.usi.gassert.data.state.VariablesHelper;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evaluator.IEvaluator;
import org.mu.util.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MRIP {
    public final Tree tree;
    public final TreeGroup treeGroup;
    public final Set<String> coveredTestCases;
    // The Pair class overrides Object.equals so that we cannot have 2 distinct pairs with the same test cases selected
    public final Set<Pair<String, String>> selectedPairs;

    public MRIP(final TreeGroup treeGroup, final IEvaluator evaluator, final Map<String, Map<String, Object>> testInputs) {
        this(treeGroup, evaluator, testInputs, Integer.MAX_VALUE);
    }

    public MRIP(final TreeGroup treeGroup, final IEvaluator evaluator, final Map<String, Map<String, Object>> testInputs, final int maxCoverage) {
        this.tree = treeGroup.buildTree();
        this.treeGroup = treeGroup;
        this.coveredTestCases = new HashSet<>(testInputs.size());
        this.selectedPairs = new HashSet<>(testInputs.size());
        // Test every possible test pair combination (in both ways)
        for (final String source : testInputs.keySet()) {
            for (final String followup : testInputs.keySet()) {
                if (!source.equals(followup)) {
                    // Build variables combining the source and the follow-up inputs
                    final Map<String, Object> metamorphicVariables =
                            VariablesHelper.makeMetamorphic(testInputs.get(source), testInputs.get(followup));
                    // Check whether the MRIP matches this pair
                    if (evaluator.eval(tree, metamorphicVariables)) {
                        coveredTestCases.add(source);
                        coveredTestCases.add(followup);
                        selectedPairs.add(Pair.of(source, followup));
                        if (coveredTestCases.size() > maxCoverage) {
                            return; // Fast path: Bail out if we went over the coverage limit
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MRIP mrip = (MRIP) o;
        return tree.equals(mrip.tree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree);
    }
}