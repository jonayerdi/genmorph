package ch.usi.gassert.evolutionary.mutation.conjunctive;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.methodtest.ConjunctiveMRIPClause;

public class RelaxOperatorConjunctiveMutation extends AbstractConjunctiveMutation {

    public static final String[] RELAXED_OPERATORS = new String[] { "<=", ">=" };

    public RelaxOperatorConjunctiveMutation(ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager) {
        super(conjunctiveBehaviourManager);
    }

    public static void relaxClause(final ConjunctiveMRIPClause clause) {
        final String operator = (String) clause.tree.getValue();
        final String newOperator;
        switch (operator) {
            case "==":
                newOperator = MyRandom.getInstance().getRandomElementArray(RELAXED_OPERATORS);
                break;
            case "<=":
                newOperator = MyRandom.getInstance().nextDouble() < Config.PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAX_RESET
                        ? "==" : ">=";
                break;
            case ">=":
                newOperator = MyRandom.getInstance().nextDouble() < Config.PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAX_RESET
                        ? "==" : "<=";
                break;
            default:
                throw new RuntimeException("Invalid clause root node: " + operator);
        }
        clause.tree.setValue(newOperator);
        /*
        if (operator.equals("==") || MyRandom.getInstance().nextDouble() >= Config.PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAX_RESET) {
            // Select one of the relaxed operators
            clause.tree.setValue(MyRandom.getInstance().getRandomElementArray(RELAXED_OPERATORS));
        } else {
            // Back to strict equality
            clause.tree.setValue("==");
        }
        */
    }

    @Override
    public ConjunctiveClausesTree mutate(ConjunctiveClausesTree tree, Individual individual,
                                         ConjunctiveMRIPClause[] originalClauses, int[] canBeRelaxed) {
        final ConjunctiveClausesTree mutatedTree = new ConjunctiveClausesTree(tree);
        // At least one of the
        if (canBeRelaxed.length > 0) {
            // Select a random clause index
            final int mutationPoint = canBeRelaxed[MyRandom.getInstance().nextInt(canBeRelaxed.length)];
            // Relax the selected clause
            relaxClause(mutatedTree.clauses[mutationPoint]);
        }
        return mutatedTree;
    }

}
