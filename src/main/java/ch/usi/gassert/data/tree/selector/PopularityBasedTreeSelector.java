package ch.usi.gassert.data.tree.selector;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;


/**
 * select based on the popularity
 */
@Deprecated
public class PopularityBasedTreeSelector extends ITreeSelector {
    @Override
    public ParentAndTree selectTree(final Tree tree, final Individual individual) {
        return RandomTreeSelector.randomTreeSelector(tree);
        /*
        final String treeString = tree.toString();
        synchronized (InfoEval.class) {
            if (!InfoEval.getAssertionTocriticalVars().containsKey(treeString) || InfoEval.getAssertionTocriticalVars().get(treeString).isEmpty()) {
                return RandomTreeSelector.randomTreeSelector(tree);
            }
            return ITreeSelector.selectTree(tree, InfoEval.getAssertionTocriticalVars().get(treeString));
        }
        */
    }
}
