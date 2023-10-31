package ch.usi.gassert.evolutionary.mutation.tree;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;

public interface ITreeMutation {

    Tree mutate(final Tree tree, final Individual individual, final TreeFactory treeFactory);

}
