package ch.usi.gassert.data.tree.selector;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Bitmap;
import ch.usi.gassert.util.GetCriticalVars;

import java.util.HashSet;
import java.util.Set;

/**
 * For GAssertMRs, with only a single output variable, this is not relevant
 */
public class MutStateDiffTreeSelector extends ITreeSelector {
    @Override
    public ParentAndTree selectTree(final Tree tree, final Individual individual) {
        final Set<String> vars = new HashSet<>();
        for (final Integer id : GetCriticalVars.getMutTestIdsToDifferenceVars().keySet()) {
            if (!Bitmap.isSet(individual.idsFNGOOD, id)) {
                vars.addAll(GetCriticalVars.getMutTestIdsToDifferenceVars().get(id));
            }
        }
        if (vars.isEmpty()) {
            return RandomTreeSelector.randomTreeSelector(tree);
        }
        return ITreeSelector.selectTree(tree, vars);
    }
}
