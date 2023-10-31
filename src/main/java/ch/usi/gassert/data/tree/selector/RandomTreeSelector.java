package ch.usi.gassert.data.tree.selector;

import ch.usi.gassert.data.TreeList;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;

public class RandomTreeSelector extends ITreeSelector {

    @Override
    public ParentAndTree selectTree(final Tree tree, final Individual individual) {
        return randomTreeSelector(tree);
    }

    /**
     * static random so all ITreeSelector can access
     *
     * @param tree
     * @return
     */
    protected static ParentAndTree randomTreeSelector(final Tree tree) {
        final TreeList treeList = tree.getListOfNodes();
        final int randomIndex = MyRandom.getInstance().nextInt(treeList.getListNodes().size());
        return new ParentAndTree(treeList.getListParent().get(randomIndex), treeList.getListNodes().get(randomIndex));
    }
}
