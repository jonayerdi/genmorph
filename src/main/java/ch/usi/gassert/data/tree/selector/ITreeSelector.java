package ch.usi.gassert.data.tree.selector;


import ch.usi.gassert.data.TreeList;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * this class selects a Node of the Tree
 */
public abstract class ITreeSelector {

    public abstract ParentAndTree selectTree(Tree tree, Individual sol);

    public static class ParentAndTree {
        private final Tree parent;
        private final Tree tree;

        public ParentAndTree(final Tree parent, final Tree tree) {
            this.parent = parent;
            this.tree = tree;
        }

        public Tree getParent() {
            return parent;
        }

        public Tree getTree() {
            return tree;
        }
    }

    public static ITreeSelector.ParentAndTree selectTree(final Tree tree, final Set<String> vars) {
        // random var
        final String randomVar = MyRandom.getInstance().getRandomElementCollection(vars) + " ";
        final TreeList treeList = tree.getListOfNodes();
        final List<Tree> nodesFiltered = new LinkedList<>();
        final List<Tree> parentsFiltered = new LinkedList<>();
        for (int i = 0; i < treeList.getListNodes().size(); i++) {
            if (treeList.getListNodes().get(i).toString().contains(randomVar)) {
                nodesFiltered.add(treeList.getListNodes().get(i));
                parentsFiltered.add(treeList.getListParent().get(i));
            }
        }
        if (nodesFiltered.isEmpty()) {
            return RandomTreeSelector.randomTreeSelector(tree);
        }
        final int randomIndex = MyRandom.getInstance().nextInt(nodesFiltered.size());
        return new ITreeSelector.ParentAndTree(parentsFiltered.get(randomIndex), nodesFiltered.get(randomIndex));
    }
}


