package ch.usi.gassert.evolutionary.mutation.conjunctive;

import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.methodtest.ConjunctiveMRIPClause;

public interface IConjunctiveMutation {

    ConjunctiveClausesTree mutate(final ConjunctiveClausesTree tree, final Individual individual,
                                  final ConjunctiveMRIPClause[] originalClauses, final int[] canBeRelaxed);

}
