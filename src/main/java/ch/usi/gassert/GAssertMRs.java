package ch.usi.gassert;

import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.manager.DataManagerFactory;
import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.state.updater.StatesUpdaterFactory;
import ch.usi.gassert.evolutionary.EvolutionaryAlgorithm;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.FileUtils;

import java.io.*;
import java.util.*;

/**
 * This is the main class the entry point of the tool
 */
public class GAssertMRs {
    public static void main(final String[] args) {
        try {
            ch.usi.gassert.util.Memory.printMemory();

            if (args.length != 8) {
                System.err.println("Wrong number of parameters: 8 arguments expected, got " + args.length);
                System.err.println("Manager class");
                System.err.println("Manager args");
                System.err.println("StatesUpdater class");
                System.err.println("StatesUpdater args");
                System.err.println("Initial assertion (numerical expression)");
                System.err.println("Random seed (integer)");
                System.err.println("Time budget in minutes");
                System.err.println("Output file");
                System.exit(-1);
            }

            final Iterator<String> arguments = Arrays.stream(args).iterator();

            final String managerClass = arguments.next();
            final String[] managerArgs = arguments.next().split(";");
            final String statesUpdaterClass = arguments.next();
            final List<String> statesUpdaterArgs = Arrays.asList(arguments.next().split(";"));
            final String initialAssertion = arguments.next();
            Config.seed = Integer.parseInt(arguments.next());
            Config.TIME_BUDGET_MINUTES = Integer.parseInt(arguments.next());
            Config.outputFile = arguments.next();

            Config.FITNESS_FILE = Config.outputFile + ".fitness.csv";
            Config.GENERATION_STATS_FILE = Config.outputFile + ".generations.csv";
            Config.FINAL_STATS_FILE = Config.outputFile + ".stats.json";
            Config.BEST_INDIVIDUALS_FILE = Config.outputFile + ".best.txt";

            final List<String> initialAssertions = new ArrayList<>();
            initialAssertions.add(initialAssertion);

            // Load DataManager
            System.out.println("Loading data [" + managerClass + "]: " + Arrays.toString(managerArgs));
            Time.getInstance().start(Time.KeysCounter.loadState);
            final int sampledCorrectStates = Config.DATASET_CORRECT_STATES_SAMPLED(Config.TIME_BUDGET_MINUTES);
            final int sampledIncorrectStates = Config.DATASET_INCORRECT_STATES_SAMPLED(Config.TIME_BUDGET_MINUTES);
            final IStatesUpdater statesUpdater = StatesUpdaterFactory.load(statesUpdaterClass, statesUpdaterArgs);
            final DataManagerArgs dataManagerArgs = new DataManagerArgs(sampledCorrectStates, sampledIncorrectStates, managerArgs);
            final IDataManager dataManager = DataManagerFactory.load(managerClass, dataManagerArgs, statesUpdater);
            final BestIndividuals bestIndividuals = new BestIndividuals(Config.BEST_INDIVIDUALS_FILE, dataManager, Config.BEST_INDIVIDUALS_COUNT);
            // Sample dataset if there are too many states
            final String correctStatesStr = String.valueOf(dataManager.getCorrectTestExecutions().size());
            final String incorrectStatesStr = String.valueOf(dataManager.getIncorrectTestExecutions().size());
            System.out.println("Correct states: " + correctStatesStr);
            System.out.println("Incorrect states: " + incorrectStatesStr);
            System.out.println("Boolean vars for generation (" + dataManager.getEvaluationVariablesManager().getBooleanVars().size() + "): " + dataManager.getEvaluationVariablesManager().getBooleanVars());
            System.out.println("Numeric vars for generation (" + dataManager.getEvaluationVariablesManager().getNumericVars().size() + "): " + dataManager.getEvaluationVariablesManager().getNumericVars());
            System.out.println("Sequence vars for generation (" + dataManager.getEvaluationVariablesManager().getSequenceVars().size() + "): " + dataManager.getEvaluationVariablesManager().getSequenceVars());
            Time.getInstance().stop(Time.KeysCounter.loadState);

            // Immediately exit if correct or incorrect states dataset is empty: No meaningful assertion can be generated
            if (dataManager.getCorrectTestExecutions().isEmpty() || dataManager.getIncorrectTestExecutions().isEmpty()) {
                System.err.println("GAssertMRs states dataset is empty!!");
                System.exit(0);
            }

            // Run EvolutionaryAlgorithm
            Config.keyExperiment = dataManager + "_seed_" + Config.seed + "_timeStamp_" + System.currentTimeMillis();
            final EvolutionaryAlgorithm evo = new EvolutionaryAlgorithm(dataManager, bestIndividuals, initialAssertions, 0);
            evo.run();

            bestIndividuals.close();

            final Individual bestIndividual = evo.getOutputAssertion();
            String bestAssertion = bestIndividual.getTree().toString();
            System.out.println("Best assertion: " + bestAssertion);

            // Write bestAssertion to file
            try {
                FileUtils.writeTextOnFile(Config.outputFile, bestAssertion, false, true);
            } catch(Exception e) {
                e.printStackTrace();
            }

            // Write final fitness to file
            if (Config.FITNESS_FILE != null) {
                try {
                    final OptionalInt maxGeneration = Generations.getInstance().getGenerationStats().stream()
                            .mapToInt(List::size)
                            .max();
                    final String fitnessCSV = "CorrectStates,IncorrectStates,Generations,FP,FN,complexity\n" +
                            correctStatesStr + "," +
                            incorrectStatesStr + "," +
                            (maxGeneration.isPresent() ? maxGeneration.getAsInt() : "null") + "," +
                            bestIndividual.fitnessValueFP + "," +
                            bestIndividual.fitnessValueFN + "," +
                            bestIndividual.complexity;
                    FileUtils.writeTextOnFile(Config.FITNESS_FILE, fitnessCSV, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Write final stats to file
            if (Config.FINAL_STATS_FILE != null) {
                Writer out = null;
                try {
                    FileUtils.mkdirsFile(Config.FINAL_STATS_FILE);
                    out = new FileWriter(Config.FINAL_STATS_FILE);
                    Stats.getInstance().writeStats(out);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Write generation stats to file
            if (Config.GENERATION_STATS_FILE != null) {
                PrintStream out = null;
                try {
                    FileUtils.mkdirsFile(Config.GENERATION_STATS_FILE);
                    out = new PrintStream(new BufferedOutputStream(new FileOutputStream(Config.GENERATION_STATS_FILE)));
                    Generations.getInstance().writeStats(out);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("GAssertMRs MAIN thread exception!!!");
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
