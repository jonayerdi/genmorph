package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.MR;

public class IsMetamorphicOutputRelationValidator implements IValidator {

    final TreeTemplate outputRelationTemplate;

    public IsMetamorphicOutputRelationValidator(final TreeTemplate outputRelationTemplate) {
        this.outputRelationTemplate = outputRelationTemplate;
    }

    @Override
    public boolean validate(Individual sol) {
        final Tree outputRelation = sol.getTreeGroup().mappings.get(outputRelationTemplate).asTree();
        return MR.isMetamorphicOutputRelation(outputRelation);
    }

}
