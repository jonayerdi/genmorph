package ch.usi.gassert.interpreter;

import ch.usi.gassert.Config;
import ch.usi.gassert.Stats;
import ch.usi.gassert.Time;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.TreeReaderJava;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.Population;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MinimizationAssertion {


    public static Population minimizeAllPopulation(final Population p, final BehaviourManager behaviourManager) {
        final Population minimizedPopulation = new Population();
        for (final Individual i : p.getPopulation()) {
            if (Thread.interrupted()) {
                return null;
            }
            minimizedPopulation.add(minimize(i, behaviourManager), behaviourManager);
        }
        return minimizedPopulation;
    }

    public static Individual minimize(final Individual individual, final BehaviourManager behaviourManager) {
        Time.getInstance().start(Time.KeysCounter.minimization);

        Individual sol;
        Individual bestIndividual = individual;

        do {

            sol = bestIndividual;

            if (sol.complexity == 1) {
                break;
            }

            try {

                if (!AssertionManager.containsAssertion(sol.getAssertionAsString())) {
                    Stats.getInstance().increment(Stats.KeysCounter.numberCacheMissAssertion);
                    Time.getInstance().start(Time.KeysCounter.computeFitnessFunction);
                    sol.compute(behaviourManager);
                    Time.getInstance().stop(Time.KeysCounter.computeFitnessFunction);
                    AssertionManager.cacheAssertion(sol);
                } else {
                    Stats.getInstance().increment(Stats.KeysCounter.numberCacheHitAssertion);
                }
                final long[] solFNIds = sol.idsFNGOOD;
                final long[] solFPIds = sol.idsFPGOOD;

                /*
                bestIndividual.compute();
                String constructing = "";
                for (final String stringSolmin : CreateAssertStatementsMinimization.convert(sol.getAssertionAsString())) {
                    if (constructing.isEmpty()) {
                        constructing = stringSolmin;
                        continue;
                    }
                    final String stringSolmin2 = "(" + constructing + ") && (" + stringSolmin + ")";
                    final Individual solMinimized = new Individual(stringSolmin2);
                    solMinimized.compute();
                    final Individual solConstructing = new Individual(constructing);
                    solConstructing.compute();
                    final Set<Integer> solConstFNIds = solConstructing.idsFNGOOD;
                    final Set<Integer> solConstFPIds = solConstructing.idsFPGOOD;

                    final Set<Integer> minFNIds = solMinimized.idsFNGOOD;
                    final Set<Integer> minFPIds = solMinimized.idsFPGOOD;
                    if (!isMinEquivalent(solConstFNIds, solConstFPIds, minFNIds, minFPIds)) {
                        constructing = stringSolmin2;
                    }
                }

                if (!constructing.isEmpty()) {
                    sol = new Individual(constructing);
                    sol.compute();
                    solFNIds = sol.idsFNGOOD;
                    solFPIds = sol.idsFPGOOD;
                    bestIndividual = sol;
                }
                */

                for (final Individual solMinimized : getAllSubtreesComputeFPFN(sol, behaviourManager)) {
                    final long[] minFNIds = solMinimized.idsFNGOOD;
                    final long[] minFPIds = solMinimized.idsFPGOOD;
                    if (isMinEquivalent(solFNIds, solFPIds, minFNIds, minFPIds)) {
                        if (solMinimized.complexity < bestIndividual.complexity) {
                            bestIndividual = solMinimized;
                        }
                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();
            }
        } while (bestIndividual != sol);

        Time.getInstance().stop(Time.KeysCounter.minimization);

        return new Individual(bestIndividual.getTree().cloneTree(), new TreeGroup(bestIndividual.getTreeGroup()));
    }

    public static class CreateAssertStatementsMinimization {

        public static List<String> convert(final String assertion) {
            final Tree tree = TreeReaderJava.getTree(assertion);
            final CreateAssertStatementsMinimization ga = new CreateAssertStatementsMinimization();
            ga.recursion(tree);
            return ga.assertions;
        }


        final List<String> assertions = new LinkedList<>();

        public void recursion(final Tree t) {
            if (t.getValue().equals("&&")) {
                recursion(t.getLeft());
                recursion(t.getRight());
            } else {
                final String assertion = t.toString();
                assertions.add(assertion);
            }
        }
    }

    private static boolean isMinEquivalent(final long[] solFNIds, final long[] solFPIds, final long[] minFNIds, final long[] minFPIds) {
        return Arrays.equals(solFNIds, minFNIds) && Arrays.equals(solFPIds, minFPIds);
    }

    /**
     * get all subtrees
     *
     * @param sol
     * @return
     */
    protected static List<Individual> getAllSubtreesComputeFPFN(final Individual sol, final BehaviourManager behaviourManager) {
        final List<Individual> individuals = new LinkedList<>();
        final TreeGroup treeGroup = sol.getTreeGroup();
        // FIXME: With TreeTemplates, I only test minimizing one of the generated subtrees at a time
        for (TreeTemplate subTree : treeGroup.mappings.keySet()) {
            subTree.getTreeBuilder().minimize(treeGroup.mappings.get(subTree)).forEach(minimizedSubTree -> {
                final TreeGroup minimizedTreeGroup = new TreeGroup(treeGroup);
                minimizedTreeGroup.mappings.put(subTree, minimizedSubTree);
                final Tree minimizedTree = minimizedTreeGroup.buildTree();
                Stats.getInstance().increment(Stats.KeysCounter.minimizations);
                final String assertionString = minimizedTree.toString();
                if (AssertionManager.getStringToSolution().containsKey(assertionString)) {
                    Stats.getInstance().increment(Stats.KeysCounter.numberCacheHitAssertion);
                    individuals.add(AssertionManager.getStringToSolution().get(assertionString));
                } else {
                    Stats.getInstance().increment(Stats.KeysCounter.numberCacheMissAssertion);
                    final Individual solSub = new Individual(minimizedTree, minimizedTreeGroup);
                    Time.getInstance().start(Time.KeysCounter.computeFitnessFunction);
                    solSub.compute(behaviourManager);
                    Time.getInstance().stop(Time.KeysCounter.computeFitnessFunction);
                    AssertionManager.cacheAssertion(solSub);
                    if (sol.isValid()) {
                        Stats.getInstance().increment(Stats.KeysCounter.minimizationImprovments);
                        individuals.add(solSub);
                    }
                }
            });
        }

        return individuals;
    }
}
