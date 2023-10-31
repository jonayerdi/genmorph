package ch.usi.gassert.evolutionary.crossover.conjunctive;

import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

public interface IConjunctiveCrossover {

    Pair<ConjunctiveClausesTree, ConjunctiveClausesTree> crossover(
            ConjunctiveClausesTree mother, ConjunctiveClausesTree father,
            final Individual individualMother, final Individual individualFather
    );

}
