package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class FitnessFunctionTest {

    @Test
    public void test1() {

        final Individual bestFP = new Individual(true, true, 0.0, 0.3, 5);
        final Individual bestFN = new Individual(true, true, 0.5, 0.3, 5);

        assertEquals(bestFP, CriteriaCompareIndividuals.FP_FN_complexity.getComparator(0).compare(bestFN, bestFP) < 0 ? bestFN : bestFP);

    }

    @Test
    public void test2() {

        final Individual bestFP = new Individual(true, true, 0.5, 0.3, 5);
        final Individual bestFN = new Individual(true, true, 0.5, 0.0, 5);

        assertEquals(bestFN, CriteriaCompareIndividuals.FP_FN_complexity.getComparator(0).compare(bestFN, bestFP) < 0 ? bestFN : bestFP);

    }

}