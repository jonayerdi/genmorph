package ch.usi.gassert.evolutionary;

import ch.usi.gassert.*;
import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;
import ch.usi.gassert.interpreter.MinimizationAssertion;
import ch.usi.gassert.util.*;
import ch.usi.gassert.util.random.MyRandom;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class EvolutionaryAlgorithm {

    public final IDataManager dataManager;
    public final BehaviourManager behaviourManager;

    public final BestIndividuals bestIndividuals;

    private final List<String> initialAssertions;
    //+ 2 because we need the thread to execute the algorithms
    public final ExecutorService executor = Executors.newFixedThreadPool(Config.numberWorkingThreads + 2);
    final CyclicBarrier barrier = new CyclicBarrier(2);

    public long timestampEnd;
    public int currentGeneration;
    boolean recomputeFitness;

    public Individual getOutputAssertion() {
        System.out.println("before minimization: " + outputAssertion.toString());
        try {
            final Individual minimized = MinimizationAssertion.minimize(outputAssertion, behaviourManager);
            minimized.compute(behaviourManager);
            if (!TrivialAssertions.isTrivial(minimized) && !minimized.getTree().getType().equals(Tree.Type.NUMBER)) {
                System.out.println("after minimization: " + minimized.toString());
                return minimized;
            }
        } catch (final Exception | Error ignored) {

        }
        return outputAssertion;
    }

    Individual outputAssertion = null;
    int iteration;

    public EvolutionaryAlgorithm(final IDataManager dataManager, final BestIndividuals bestIndividuals, final List<String> initialAssertions, final int iteration) {
        LogUtils.log().info("START " + Config.keyExperiment);
        this.behaviourManager = dataManager.getBehaviourManager();
        this.dataManager = dataManager;
        this.bestIndividuals = bestIndividuals;
        this.initialAssertions = initialAssertions;
        this.iteration = iteration;
        this.timestampEnd = Long.MAX_VALUE;
        this.currentGeneration = 0;
        this.recomputeFitness = false;
    }

    private void onNextGen(int gen) {
        // Update current generation
        currentGeneration = gen;
        // Notify all ITreeBuilders of the current generation, in case the behaviour of the evolutionary operator changes
        dataManager.getTreeTemplate().getGeneratedNodes().forEach(tt -> tt.getTreeBuilder().updateGen(gen));
    }

    public void run() {
        Stats.getInstance().set(Stats.KeysCounter.timestampStart, System.currentTimeMillis());
        LogUtils.log().info("start initialization population");
        Time.getInstance().start(Time.KeysCounter.initialPopulation);
        final Population initialPopulation = initializePopulation();
        Time.getInstance().stop(Time.KeysCounter.initialPopulation);
        LogUtils.log().info("finish initialization population");
        final Evolve FP = new Evolve(this, 0, initialPopulation, this::onNextGen);
        final Evolve FN = new Evolve(this, 1, initialPopulation.clone());
        Generations.init(2);
        try {
            timestampEnd = System.currentTimeMillis() + (((long) Config.TIME_BUDGET_MINUTES) * 60 * 1000);
            executor.submit(FP);
            executor.submit(FN);
            executor.awaitTermination(Config.TIME_BUDGET_MINUTES, TimeUnit.MINUTES);
            finish();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final List<Individual> bestFPAll = FP.elitism.getBestIndividuals(currentGeneration);
        this.bestIndividuals.writeIndividuals(currentGeneration, bestFPAll);

        final Individual bestFP = FP.elitism.bestOfTheBest;
        final Individual bestFN = FN.elitism.getBestOfBestIndividual(currentGeneration);
        outputAssertion = CriteriaCompareIndividuals.FP_FN_complexity.getComparator(currentGeneration).compare(bestFN, bestFP) < 0 ? bestFN : bestFP;

        final int generationFoundBest;
        if (bestFP.equals(bestFN)) {
            generationFoundBest = Math.min(FP.elitism.generation, FN.elitism.generation);
        } else {
            generationFoundBest = bestFP.equals(outputAssertion) ? FP.elitism.generation : FN.elitism.generation;
        }
        Stats.getInstance().setIfMissing(Stats.KeysCounter.generationGoodSolution, -1);
        Stats.getInstance().setIfMissing(Stats.KeysCounter.timestampGoodSolution, 0);
        Stats.getInstance().set(Stats.KeysCounter.generationBestSolution, generationFoundBest);
        Stats.getInstance().print();
        Time.getInstance().print(iteration);
        LogUtils.log().info("finish evolution!");
    }

    public void finish() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ignore) {

        } finally {
            if (!executor.isTerminated()) {
                System.out.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
            System.out.println("shutdown finished");
        }
        try {
            // wait a while to let thread finish
            Thread.sleep(3000);
        } catch (final InterruptedException e) {
            // e.printStackTrace();
        }

    }

    /**
     * half population random and half mutation of the initial one if any
     */
    private Population initializePopulation() {
        final Population initialPopulation = new Population();
        System.out.println("initial assertions " + initialAssertions);
        randomlyMutatedInitialAssertionInitialization(initialAssertions, initialPopulation);
        randomlyGeneratedInitialization(initialPopulation);
        return initialPopulation;
    }

    public void randomlyGeneratedInitialization(final Population initialPopulation) {

        System.out.println("current size population is " + initialPopulation.size());
        System.out.println("start random generation initial population.......");

        // naive case that is true still do it randomly
        do {
            final TreeGroup treeGroup = new TreeGroup(dataManager.getTreeTemplate());
            final Tree tree = treeGroup.buildTree();
            final Individual individual = new Individual(tree, treeGroup);
            initialPopulation.add(individual, behaviourManager);
        } while (initialPopulation.size() < Config.POPULATION_SIZE);
        System.out.println("created population of " + initialPopulation.size());
    }


    public void randomlyMutatedInitialAssertionInitialization(final List<String> initialAssertions, final Population initialPopulation) {
        final List<String> assertions = new ArrayList<>(initialAssertions);

        Memory.printMemory();

        final HashMap<String, Individual> stringTOindividual = new HashMap<>();

        final List<String> trivialAssertions = new ArrayList<>();
        for (final String assertion : assertions) {
            if (TrivialAssertions.isTrivial(assertion)) {
                trivialAssertions.add(assertion);
            }
        }
        assertions.removeAll(trivialAssertions);
        System.out.println("assertions to be considered for mutation: " + assertions);

        if (assertions.isEmpty()) {
            return;
        }
        for (final String assertion : assertions) {
            final Individual individual = new Individual(assertion, dataManager.getTreeTemplate(), dataManager.getEvaluationVariableTypes());
            individual.compute(behaviourManager);
            AssertionManager.cacheAssertion(individual);
            stringTOindividual.put(assertion, individual);
            initialPopulation.add(individual, behaviourManager);
        }
        while (initialPopulation.size() < Config.POPULATION_SIZE * Config.PROPORTION_MUTATED_INIT) {
            for (final String assertion : assertions) {
                if (initialPopulation.size() >= Config.POPULATION_SIZE * Config.PROPORTION_MUTATED_INIT) {
                    break;
                }
                final Individual individual =
                        stringTOindividual.get(assertion);
                TreeGroup mutatedTreeGroup = behaviourManager.getMutation().mutate(individual.getTreeGroup(), individual);
                Tree mutatedTree = mutatedTreeGroup.buildTree();
                final String newIndividualString = mutatedTree.toString();
                final Individual newIndividual;
                if (!AssertionManager.containsAssertion(newIndividualString)) {
                    newIndividual = new Individual(mutatedTree, mutatedTreeGroup);
                    newIndividual.compute(behaviourManager);
                    AssertionManager.cacheAssertion(newIndividual);
                } else {
                    newIndividual = AssertionManager.getStringToSolution().get(newIndividualString);
                }
                initialPopulation.add(newIndividual, behaviourManager);
            }
        }
        Memory.printMemory();
    }
}


class Evolve implements Callable<Void> {

    final EvolutionaryAlgorithm evo;
    Population population;
    int indexFitness;
    long timeLastGeneration;
    long sumTimeEachGeneration = 0L;
    Elitism elitism;
    final Consumer<Integer> onNextGen;

    Evolve(final EvolutionaryAlgorithm evo, final int i, final Population population, Consumer<Integer> onNextGen) {
        this.evo = evo;
        this.population = population;
        indexFitness = i;
        timeLastGeneration = System.currentTimeMillis();
        elitism = new Elitism();
        this.onNextGen = onNextGen;
    }

    Evolve(final EvolutionaryAlgorithm evo, final int i, final Population population) {
        this(evo, i, population, null);
    }

    @Override
    public Void call() throws InterruptedException, BrokenBarrierException {

        try {
            ch.usi.gassert.util.Memory.printMemory();

            int gen = 0;
            //storeCSV(gen);
            LogUtils.log().info("START " + CriteriaCompareIndividuals.values()[indexFitness].toString() + " evolution!");
            while (gen <= Config.MAX_GENERATION) {

                // We can use this to check if we are accidentally mutating Individuals in place
                // instead of deep cloning them, because the AssertionManager will have inconsistent entries
                //AssertionManager.checkIntegrity();

                if (Thread.interrupted()) {
                    return null;
                }
                gen++;

                if (this.onNextGen != null) {
                    this.onNextGen.accept(gen);
                }

                // let only one thread to print the info
                if (indexFitness == 0 && gen % 10 == 0) {
                    printInfo(gen, timeLastGeneration);
                    final List<Individual> best = elitism.getBestIndividuals(gen);
                    final Individual bestOfTheBest = elitism.bestOfTheBest;
                    if (bestOfTheBest != null) {
                        System.out.println("BEST INDIVIDUAL: " + bestOfTheBest);
                    }
                    timeLastGeneration = System.currentTimeMillis();
                    this.evo.bestIndividuals.writeIndividuals(gen, best);
                }

                evo.barrier.await();

                // Synchronize with IStatesUpdater, only one of the Evolutionary instances
                if (indexFitness == 0) {
                    evo.recomputeFitness = false;
                    final Integer timeBudget = Config.GENERATION_STATES_UPDATE_TIME_BUDGET(gen, evo.timestampEnd - System.currentTimeMillis());
                    if (timeBudget != null) {
                        final List<ITestExecution> correctTestExecutions
                                = evo.dataManager.getStatesUpdater().sync(elitism.getBestIndividuals(gen), timeBudget);
                        if (!correctTestExecutions.isEmpty()) {
                            evo.dataManager.addCorrectStates(correctTestExecutions);
                            System.out.println("Correct states: " + evo.dataManager.getCorrectTestExecutions().size()
                                    + " (+" + correctTestExecutions.size() + ")\n");
                            Migration.recomputeFitness(evo.behaviourManager);
                            AssertionManager.clearAll();
                            evo.recomputeFitness = true;
                        }
                    }
                }

                evo.barrier.await();

                // Recompute fitnesses after statesUpdater
                if (evo.recomputeFitness) {
                    population.recomputeFitness(evo.behaviourManager);
                    elitism.recomputeFitness(evo.behaviourManager);
                }

                synchronized (AssertionManager.class) {
                    for (final Individual sol : population.population) {
                        AssertionManager.cacheAssertion(sol);
                        AssertionManager.cacheIdsData(AssertionManager.Type.FN, sol, gen);
                        AssertionManager.cacheIdsData(AssertionManager.Type.FP, sol, gen);
                    }
                }

                evo.barrier.await();

                // ELITISM
                final Population newPopulation = new Population();

                final List<Individual> elite = elitism.updateAndGetElitism(population, gen);

                if (evo.behaviourManager.IS_ELITISM_ENABLED && gen % Config.generationElitism == 0) {
                    newPopulation.addAll(elite);
                }

                final Individual bestOfTheBest = elitism.getBestOfBestIndividual(gen);

                if (bestOfTheBest != null) {
                    if (bestOfTheBest.isGood()) {
                        Stats.getInstance().setIfMissing(Stats.KeysCounter.generationGoodSolution, gen);
                        Stats.getInstance().setIfMissing(Stats.KeysCounter.timestampGoodSolution, System.currentTimeMillis());
                    }
                    if (bestOfTheBest.isPerfect() && gen >= Config.MIN_GENERATION) {
                        System.out.println("PERFECT INDIVIDUAL: " + bestOfTheBest.toString());
                        LogUtils.log().info(">>>> FOUND solution with zero FP and zero FN!");
                        LogUtils.log().info("with fitness function " + CriteriaCompareIndividuals.values()[indexFitness].toString());
                        evo.finish();
                        return null;
                    }
                }

                // Store information about the population in this generation
                Generations.getInstance().addGeneration(indexFitness, population, elitism, elitism.generation == gen, gen);

                // MIGRATION
                if (evo.behaviourManager.IS_MIGRATION_ENABLED && gen % Config.generationMigration == 0) {
                    Migration.store(population, indexFitness, gen);
                    evo.barrier.await();
                    newPopulation.addAll(Migration.getEliteFromOtherPopulation(indexFitness));
                }


                evo.barrier.await();

                if (Thread.interrupted()) {
                    return null;
                }

                crossover(newPopulation, gen);

                if (Thread.interrupted()) {
                    return null;
                }

                population.clear();
                population = newPopulation;

            }
        } catch (final InterruptedException e) {
            System.out.println("interrupted");
        } catch (final Exception |
                Error e) {
            e.printStackTrace();
        }
        return null;
    }


    private void crossover(final Population newPopulation, final int gen) {
        // I need to compute the size because we might have done the elitism
        // if is empty no issue it will be 0
        final int currentSize = newPopulation.size();
        if (newPopulation.population.size() > Config.POPULATION_SIZE) {
            newPopulation.population = newPopulation.population.subList(0, Config.POPULATION_SIZE);
            return;
        }
        final CountDownLatch latch = new CountDownLatch(Config.numberWorkingThreads);
        try {
            for (int i = 0; i < Config.numberWorkingThreads; i++) {
                evo.executor.submit(() -> {
                    try {
                        final Population partition = new Population((Config.POPULATION_SIZE - currentSize) / Config.numberWorkingThreads);
                        addWithCrossOver(partition, gen);
                        synchronized (newPopulation) {
                            newPopulation.addPartition(partition);
                        }
                        latch.countDown();
                    } catch (final Exception | Error e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (final RuntimeException e) {
            // e.printStackTrace();
        }
        try {
            latch.await();
        } catch (final InterruptedException e) {
            // e.printStackTrace();
        }
        // it is possible that now the population has fewer elements because maybe the partitions cannot be divided by the number of threads
        // or because some generated tests have too high complexity
        // top up again
        synchronized (newPopulation) {
            if (newPopulation.population.size() == Config.POPULATION_SIZE) {
                return;
            }
            addWithCrossOver(newPopulation, gen);
            if (newPopulation.population.size() > Config.POPULATION_SIZE) {
                newPopulation.population = newPopulation.population.subList(0, Config.POPULATION_SIZE);
            }
        }

    }


    private void addWithCrossOver(final Population partition, final int gen) {
        do {
            if (Thread.interrupted()) {
                return;
            }
            Time.getInstance().start(Time.KeysCounter.selection);
            final Pair<Individual, Individual> parents = evo.behaviourManager.getSelection().select(population.population, indexFitness == 0 ? AssertionManager.Type.FP : AssertionManager.Type.FN);
            Time.getInstance().stop(Time.KeysCounter.selection);

            final Individual sol1 = parents.getKey();
            final Individual sol2 = parents.getValue();
            if (MyRandom.getInstance().nextFloat() <= Config.PROB_CROSSOVER) {
                Stats.getInstance().increment(Stats.KeysCounter.numberCrossover);
                Time.getInstance().start(Time.KeysCounter.crossOver);
                final Pair<TreeGroup, TreeGroup> offsprings = evo.behaviourManager.getCrossover().crossover(sol1.getTreeGroup(), sol2.getTreeGroup(), sol1, sol2);
                Time.getInstance().stop(Time.KeysCounter.crossOver);
                addOrMutateWithProb(offsprings.getKey().buildTree(), offsprings.getKey(), partition, gen);
                addOrMutateWithProb(offsprings.getValue().buildTree(),offsprings.getValue(), partition, gen);
            } else {
                addOrMutateWithProb(sol1.getTree(), sol1.getTreeGroup(), partition, gen);
                addOrMutateWithProb(sol2.getTree(), sol2.getTreeGroup(), partition, gen);
            }
        } while (!partition.isFull());
    }


    private void addOrMutateWithProb(final ITree tree, final TreeGroup treeGroup, final Population population, final int gen) {
        final Individual individual;
        final String assertionString = tree.toString();
        if (AssertionManager.containsAssertion(assertionString)) {
            individual = AssertionManager.getStringToSolution().get(assertionString);
        } else {
            individual = new Individual(tree, treeGroup);
        }
        Time.getInstance().start(Time.KeysCounter.mutation);
        Individual newIndividual;
        if (MyRandom.getInstance().nextFloat() <= Config.PROB_MUTATION) {
            final TreeGroup mutatedTreeGroup = evo.behaviourManager.getMutation().mutate(new TreeGroup(treeGroup), individual);
            newIndividual = new Individual(mutatedTreeGroup);
        } else {
            newIndividual = individual;
        }
        Time.getInstance().stop(Time.KeysCounter.mutation);
        if (Config.IS_MINIMIZATION_ENABLED && gen % Config.generationMinimization == 0) {
            population.add(MinimizationAssertion.minimize(newIndividual, evo.behaviourManager), evo.behaviourManager);
        } else {
            population.add(newIndividual, evo.behaviourManager);
        }
    }

    public void printInfo(final int gen, final long timeLastGeneration) {

        System.out.println("# generation:              " + gen);
        final long timeCurrentGeneration = System.currentTimeMillis() - timeLastGeneration;
        sumTimeEachGeneration = sumTimeEachGeneration + timeCurrentGeneration;
        System.out.println("time cost:                 " + TimeUtils.getPrettyPrintTime(timeCurrentGeneration));
        System.out.println("average time cost:         " + (gen > 0 ? TimeUtils.getPrettyPrintTime(sumTimeEachGeneration / gen) : 0));
        System.out.println("AssertionManager cached assertions:          " + AssertionManager.getNumberCachedAssertions());
        final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        System.out.println("CPU load:                  " + String.valueOf(osMxBean.getSystemLoadAverage()));

        final ThreadMXBean threadmxBean = ManagementFactory.getThreadMXBean();
        System.out.println("# running threads:                  " + String.valueOf(threadmxBean.getThreadCount()));

        final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        System.out.println("Memory heap used:          " + String.valueOf(Evolve.bytesToMeg(memBean.getHeapMemoryUsage().getUsed())));
    }

    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMeg(final long bytes) {
        return bytes / MEGABYTE;
    }


}
