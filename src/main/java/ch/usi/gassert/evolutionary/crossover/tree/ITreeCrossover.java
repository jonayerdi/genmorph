package ch.usi.gassert.evolutionary.crossover.tree;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

public interface ITreeCrossover {

    Pair<Tree, Tree> crossover(Tree mother, Tree father, final Individual individualMother, final Individual individualFather);

}
