package ch.usi.gassert.data.tree.builder;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.mutation.conjunctive.RelaxOperatorConjunctiveMutation;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.methodtest.ConjunctiveMRIPClause;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.usi.gassert.util.Assert.assertAlways;

/**
 * Expression composed of a conjunction of boolean clauses over a single variable. Used for MRIPs.
 *
 * Their form should be: [VARIABLE_FOLLOWUP] [OPERATOR] [F(VARIABLE_SOURCE)]
 *
 * These MRIPs cannot be freely modified like a normal Tree, but it can be relaxed by either:
 *  1. Dropping some clauses.
 *  2. Relaxing the OPERATOR from some clauses (e.g. change == to <= or >=).
 */
public class ConjunctiveClausesTreeBuilder implements ITreeBuilder<ConjunctiveClausesTree> {

    public ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager;

    // Original MRIP clauses
    public final ConjunctiveMRIPClause[] originalClauses;
    // Indices of clauses that can be relaxed (depends on variable types)
    public final int[] canBeRelaxed;

    public ConjunctiveClausesTreeBuilder(final ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager,
                                         final ConjunctiveMRIPClause[] originalClauses) {
        assertAlways(originalClauses.length <= Long.SIZE, "Too many clauses");
        this.conjunctiveBehaviourManager = conjunctiveBehaviourManager;
        this.originalClauses = originalClauses;
        final List<Integer> canBeRelaxed = new ArrayList<>(originalClauses.length);
        for (int i = 0 ; i < originalClauses.length ; ++i) {
            // Clauses should follow the template:
            // [VARIABLE_FOLLOWUP] [OPERATOR] [F(VARIABLE_SOURCE)]
            // So the type of the variable can be inferred from the type of the left node (VARIABLE_FOLLOWUP)
            if (originalClauses[i].getTree().getLeft().getType() == Tree.Type.NUMBER) {
                canBeRelaxed.add(i);
            }
        }
        this.canBeRelaxed = canBeRelaxed.stream().mapToInt(x -> x).toArray();
    }

    @Override
    public Tree.Type getTreeType() {
        return Tree.Type.BOOLEAN;
    }

    @Override
    public ConjunctiveClausesTree buildRandomTree() {
        // Select clauses with PROB_CONJUNCTIVE_MRIP_CLAUSE_SELECTED
        long isClauseSelected = 0L;
        for (int index = 0 ; index < this.originalClauses.length ; ++index) {
            if (MyRandom.getInstance().nextDouble() < Config.PROB_CONJUNCTIVE_MRIP_CLAUSE_SELECTED) {
                isClauseSelected |= 1L << index;
            }
        }
        final ConjunctiveClausesTree newTree = new ConjunctiveClausesTree(this.originalClauses, isClauseSelected);
        // Relax clauses with PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAXED
        for (ConjunctiveMRIPClause clause : newTree.clauses) {
            if (MyRandom.getInstance().nextDouble() < Config.PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAXED) {
                RelaxOperatorConjunctiveMutation.relaxClause(clause);
            }
        }
        return newTree;
    }

    @Override
    public ConjunctiveClausesTree matchTree(Tree tree) {
        final List<ConjunctiveMRIPClause> clauses = new ArrayList<>();
        matchTree(tree, clauses);
        assertAlways(clauses.size() == originalClauses.length,
                "Expected " + originalClauses.length + " clauses, but matched "
                        + clauses.size() + " for expression \"" + tree + "\"");
        return new ConjunctiveClausesTree(clauses.toArray(new ConjunctiveMRIPClause[0]));
    }

    public void matchTree(Tree tree, final List<ConjunctiveMRIPClause> clauses) {
        assertAlways(tree.getType() == Tree.Type.BOOLEAN,
                "Conjunctive clause \"" + tree + "\" is not BOOLEAN");
        if (tree.getValue().equals("&&")) {
            matchTree(tree.getLeft(), clauses);
            matchTree(tree.getRight(), clauses);
        } else {
            clauses.add(new ConjunctiveMRIPClause(tree));
        }
    }

    @Override
    public ConjunctiveClausesTree mutate(final ITree tree, final Individual individual) {
        return conjunctiveBehaviourManager.getMutation().mutate((ConjunctiveClausesTree) tree, individual, originalClauses, canBeRelaxed);
    }

    @Override
    public Pair<ConjunctiveClausesTree, ConjunctiveClausesTree>
    crossover(ITree mother, ITree father,
              final Individual individualMother, final Individual individualFather) {
        return conjunctiveBehaviourManager.getCrossover()
                .crossover((ConjunctiveClausesTree) mother, (ConjunctiveClausesTree) father, individualMother, individualFather);
    }

    @Override
    public void updateGen(int gen) {
        conjunctiveBehaviourManager.updateGen(gen);
    }

    @Override
    public Stream<ConjunctiveClausesTree> minimize(final ITree iTree) {
        final ConjunctiveClausesTree tree = (ConjunctiveClausesTree) iTree;
        return IntStream.range(0, tree.clauses.length)
                .filter(i -> (tree.clauseSelected & (1L << i)) != 0)
                .mapToObj(i -> new ConjunctiveClausesTree(tree.clauses, tree.clauseSelected ^ (1L << i)));
    }

}
