package ch.usi.gassert.evolutionary.crossover.conjunctive;

import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.ArrayUtils;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.methodtest.ConjunctiveMRIPClause;

public class SinglePointConjunctiveCrossover extends AbstractConjunctiveCrossover {

    public SinglePointConjunctiveCrossover(ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager) {
        super(conjunctiveBehaviourManager);
    }

    protected static ConjunctiveMRIPClause[] mergeClausesAtCrossoverPoint
            (final ConjunctiveMRIPClause[] a, final ConjunctiveMRIPClause[] b, int crossoverPoint) {
        return ArrayUtils.merge(a, 0, crossoverPoint, b, crossoverPoint, b.length)
                .toArray(ConjunctiveMRIPClause[]::new);
    }

    protected static long mergeSelectedAtCrossoverPoint(long a, long b, int crossoverPoint) {
        final long bitmask = ((1L << crossoverPoint) - 1);
        return  (a & bitmask) | (b & ~bitmask);
    }

    @Override
    public Pair<ConjunctiveClausesTree, ConjunctiveClausesTree>
    crossover(ConjunctiveClausesTree mother, ConjunctiveClausesTree father,
              Individual individualMother, Individual individualFather) {
        final int crossoverPoint = MyRandom.getInstance().nextInt(mother.clauses.length);
        final ConjunctiveClausesTree child1 = new ConjunctiveClausesTree(
                mergeClausesAtCrossoverPoint(mother.clauses, father.clauses, crossoverPoint),
                mergeSelectedAtCrossoverPoint(mother.clauseSelected, father.clauseSelected, crossoverPoint)
        );
        final ConjunctiveClausesTree child2 = new ConjunctiveClausesTree(
                mergeClausesAtCrossoverPoint(father.clauses, mother.clauses, crossoverPoint),
                mergeSelectedAtCrossoverPoint(father.clauseSelected, mother.clauseSelected, crossoverPoint)
        );
        return new Pair<>(child1, child2);
    }

}
