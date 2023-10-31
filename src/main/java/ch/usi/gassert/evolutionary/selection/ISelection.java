package ch.usi.gassert.evolutionary.selection;

import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.util.Pair;

import java.util.List;

public interface ISelection {

    Pair<Individual, Individual> select(List<Individual> population, AssertionManager.Type phase);

}
