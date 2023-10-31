package ch.usi.gassert.evolutionary;


import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.*;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.fitness.IFitnessFunction;
import ch.usi.gassert.evolutionary.fitness.IValidator;

import java.util.*;

/**
 * This class represents an individual of the population
 */
public class Individual {

    private final ITree tree;
    private final TreeGroup treeGroup;
    public final Integer complexity;

    public Boolean valid = null;
    public Boolean eliteValid = null;
    public Double fitnessValueFP = null;
    public Double fitnessValueFN = null;

    public long[] idsFPGOOD = null;
    public long[] idsFNGOOD = null;

    // Fitness functions
    private int lastComputedCorrectTestExecutionsSize = 0;
    private int lastComputedIncorrectTestExecutionsSize = 0;

    // Only for InputRelationSatisfactionValidator
    private int lastValidatedTestExecutionsSize = 0;
    private long lastValidatedSatisfactionCount = 0;

    // For testing only
    public Individual(final Boolean valid, final Boolean eliteValid, final Double fitnessValueFP, final Double fitnessValueFN, final Integer complexity) {
        this.tree = null;
        this.treeGroup = null;
        this.valid = valid;
        this.eliteValid = eliteValid;
        this.fitnessValueFP = fitnessValueFP;
        this.fitnessValueFN = fitnessValueFN;
        this.complexity = complexity;
    }

    public Individual(final String expression, final TreeTemplate treeTemplate,
                      final Map<String, Class<?>> variableTypes) {
        this(TreeReaderGAssert.getTree(expression, variableTypes), treeTemplate);
    }

    public Individual(final TreeGroup treeGroup) {
        this(treeGroup.buildTree(), treeGroup);
    }

    public Individual(final ITree tree, final TreeTemplate treeTemplate) {
        this(tree, treeTemplate.match(tree.asTree()));
    }

    public Individual(final ITree tree, final TreeGroup treeGroup) {
        this.tree = tree;
        this.treeGroup = treeGroup;
        this.complexity = tree.getNumberOfNodes();
    }

    public String getAssertionAsString() {
        return tree.toString();
    }

    public Double getFitnessValueFP() {
        return fitnessValueFP;
    }

    public Double getFitnessValueFN() {
        return fitnessValueFN;
    }

    public ITree getTree() {
        return tree;
    }

    public TreeGroup getTreeGroup() {
        return treeGroup;
    }

    public void setIdsFPGOOD(final long[] idsFPGOOD) {
        this.idsFPGOOD = idsFPGOOD;
    }

    public void setIdsFNGOOD(final long[] idsFNGOOD) {
        this.idsFNGOOD = idsFNGOOD;
    }


    public void compute(final IValidator validator, final IValidator eliteValidator, final IFitnessFunction fitnessFP, final IFitnessFunction fitnessFN) {
        if (valid == null) {
            valid = validator.validate(this);
            if (valid) {
                eliteValid = eliteValidator.validate(this);
                fitnessFP.computeFitness(this);
                fitnessFN.computeFitness(this);
            }
        }
    }

    public void compute(final BehaviourManager behaviourManager) {
        compute(behaviourManager.validator, behaviourManager.eliteValidator, behaviourManager.fitnessFP, behaviourManager.fitnessFN);
    }

    public void recompute(final IValidator validator, final IValidator eliteValidator, final IFitnessFunction fitnessFP, final IFitnessFunction fitnessFN) {
        valid = validator.revalidate(this, this.valid);
        if (valid) {
            eliteValid = eliteValidator.revalidate(this, this.eliteValid);
            fitnessFP.recomputeFitness(this);
            fitnessFN.recomputeFitness(this);
        }
    }

    public void recompute(final BehaviourManager behaviourManager) {
        recompute(behaviourManager.validator, behaviourManager.eliteValidator, behaviourManager.fitnessFP, behaviourManager.fitnessFN);
    }

    public int getLastComputedCorrectTestExecutionsSize() {
        return lastComputedCorrectTestExecutionsSize;
    }

    public int getLastComputedIncorrectTestExecutionsSize() {
        return lastComputedIncorrectTestExecutionsSize;
    }

    public void setLastComputedCorrectTestExecutionsSize(final int correctTestExecutionsSize) {
        lastComputedCorrectTestExecutionsSize = correctTestExecutionsSize;
    }

    public void setLastComputedIncorrectTestExecutionsSize(final int incorrectTestExecutionsSize) {
        lastComputedIncorrectTestExecutionsSize = incorrectTestExecutionsSize;
    }

    public int getLastValidatedTestExecutionsSize() {
        return this.lastValidatedTestExecutionsSize;
    }

    public void setLastValidatedTestExecutionsSize(final int testExecutionsSize) {
        this.lastValidatedTestExecutionsSize = testExecutionsSize;
    }

    public long getLastValidatedSatisfactionCount() {
        return this.lastValidatedSatisfactionCount;
    }

    public void setLastValidatedSatisfactionCount(final long satisfactionCount) {
        this.lastValidatedSatisfactionCount = satisfactionCount;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isEliteValid() {
        return eliteValid;
    }

    public boolean isGood() {
        return fitnessValueFP == 0.0 && ((1.0 - fitnessValueFN) > 0.000001);
    }

    public boolean isPerfect() {
        return fitnessValueFN == 0.0 && fitnessValueFP == 0.0;
    }

    public int compareFNFPTo(final Individual o) {
        int res = fitnessValueFN.compareTo(o.fitnessValueFN);
        if (res != 0) {
            return res;
        }
        res = fitnessValueFP.compareTo(o.fitnessValueFP);
        return (res != 0 ? res : complexity.compareTo(o.complexity));
    }

    public int compareFPFNTo(final Individual o) {
        int res = fitnessValueFP.compareTo(o.fitnessValueFP);
        if (res != 0) {
            return res;
        }
        res = fitnessValueFN.compareTo(o.fitnessValueFN);
        return (res != 0 ? res : complexity.compareTo(o.complexity));
    }

    public int compareFNFPToPENALIZATION(final Individual o) {
        int res = getFNpenalized().compareTo(o.getFNpenalized());
        if (res != 0) {
            return res;
        }
        res = getFPpenalized().compareTo(o.getFPpenalized());
        return (res != 0 ? res : complexity.compareTo(o.complexity));
    }

    public int compareFPFNToPENALIZATION(final Individual o) {
        int res = getFPpenalized().compareTo(o.getFPpenalized());
        if (res != 0) {
            return res;
        }
        res = getFNpenalized().compareTo(o.getFNpenalized());
        return (res != 0 ? res : complexity.compareTo(o.complexity));
    }


    private Double getFPpenalized() {
        return fitnessValueFP * computePenalization();
    }

    private Double getFNpenalized() {
        return fitnessValueFN * computePenalization();
    }

    private double computePenalization() {
        if (complexity < Config.THETA) {
            return 1.0;
        }
        return Math.pow(2.0, complexity - Config.THETA);
    }

    @Override
    public String toString() {
        return new StringJoiner("\n", "", "\n")
                .add(getAssertionAsString())
                //.add(ConverterExpression.convertGASSERTtoJava(assertionString))
                .add("fitnessFP=" + fitnessValueFP)
                .add("fitnessFN=" + fitnessValueFN)
                .add("complexity=" + complexity)
                //.add("type=" + tree.getType())
                //.add("idsFNGOOD" + (idsFNGOOD != null ? idsFNGOOD.toString() : ""))
                //.add("idsFPGOOD" + (idsFPGOOD != null ? idsFPGOOD.toString() : ""))
                .toString();
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Individual)) {
            return false;
        }
        final Individual individual = (Individual) o;
        return tree.equals(individual.tree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree);
    }

}