package ch.usi.gassert.evolutionary.mutation;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;

public interface IMutation {

    TreeGroup mutate(final TreeGroup treeGroup, final Individual individual);

}
