package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.data.tree.ConjunctiveClausesTree;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.Individual;

public class ConjunctiveClauseSelectedValidator implements IValidator {

    final TreeTemplate inputRelationTemplate;

    public ConjunctiveClauseSelectedValidator(final TreeTemplate inputRelationTemplate) {
        this.inputRelationTemplate = inputRelationTemplate;
    }

    @Override
    public boolean validate(Individual sol) {
        ConjunctiveClausesTree tree = (ConjunctiveClausesTree) sol.getTreeGroup().mappings.get(inputRelationTemplate);
        // At least 1 clause must be selected
        return tree.clauseSelected != 0L;
    }

}
