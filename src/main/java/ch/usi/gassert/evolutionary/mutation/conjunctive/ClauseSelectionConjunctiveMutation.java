package ch.usi.gassert.evolutionary.mutation.conjunctive;

import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.methodtest.ConjunctiveMRIPClause;

public class ClauseSelectionConjunctiveMutation extends AbstractConjunctiveMutation {

    public ClauseSelectionConjunctiveMutation(ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager) {
        super(conjunctiveBehaviourManager);
    }

    @Override
    public ConjunctiveClausesTree mutate(ConjunctiveClausesTree tree, Individual individual,
                                         ConjunctiveMRIPClause[] originalClauses, int[] canBeRelaxed) {
        // Select a random clause index
        final long mutationPoint = MyRandom.getInstance().nextInt(tree.clauses.length);
        // Flip the bit indicating whether the clause is selected or not
        return new ConjunctiveClausesTree(tree.clauses, tree.clauseSelected ^ (1L << mutationPoint));
    }

}
