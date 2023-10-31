package ch.usi.gassert.evolutionary.crossover;

import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

public interface ICrossover {

    Pair<TreeGroup, TreeGroup> crossover(TreeGroup mother, TreeGroup father, final Individual individualMother, final Individual individualFather);

}
