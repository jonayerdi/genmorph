package ch.usi.gassert;

import ch.usi.gassert.search.AVMSearch;
import ch.usi.gassert.search.VariablesList;
import ch.usi.gassert.util.random.DynamicWeightedMap;

import java.text.DecimalFormat;

import org.apache.log4j.Level;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * This class are the configurations of the tool
 * <p>
 * //TODO load from file custom configurations
 */
public class Config {
    public static String GASSERT_HOME;
    public static String pathDataFolder;
    public static String pathTestInfos;
    public static String outputFile;
    public static String keyExperiment;
    public static int numberWorkingThreads = Runtime.getRuntime().availableProcessors() - 2;

    static {
        // two cases calling from GAssert root or inside the subjects
        GASSERT_HOME = new File("").getAbsolutePath();
    }

    /**
     * path temp file
     */
    public static Level loggerLevel = Level.INFO;
    public static String fileResult = GASSERT_HOME + File.separator + "result.csv";

    /**
     * GAssert is pseudo-deterministic given the seed
     */
    public static int seed = 0;

    /**
     * DEBUG mode
     */
    public static boolean DEBUG = false;

    /**
     * Output final fitness
     */
    public static String FITNESS_FILE = null;

    /**
     * Output per-generation fitness stats
     */
    public static String GENERATION_STATS_FILE = null;

    /**
     * Output final stats
     */
    public static String FINAL_STATS_FILE = null;

    /**
     * Output best individuals
     */
    public static String BEST_INDIVIDUALS_FILE = null;
    public static int BEST_INDIVIDUALS_COUNT = 4;

    /**
     * Dataset sampling
     */
    public static double DATASET_CORRECT_STATES_RATIO = 0.1;
    public static int DATASET_MAX_STATES_SAMPLED(final int time_budget_minutes) {
        return 300 * time_budget_minutes;
    }
    public static int DATASET_CORRECT_STATES_SAMPLED(final int time_budget_minutes) {
        return (int)((double)DATASET_MAX_STATES_SAMPLED(time_budget_minutes) * DATASET_CORRECT_STATES_RATIO);
    }
    public static int DATASET_INCORRECT_STATES_SAMPLED(final int time_budget_minutes) {
        return DATASET_MAX_STATES_SAMPLED(time_budget_minutes) - DATASET_CORRECT_STATES_SAMPLED(time_budget_minutes);
    }
    /**
     * percentage of tests and mutations
     */
    public static float percentageTestMutantsInInput = 1.0f;

    /**
     * Output variable name used to represent "use all the outputs"
     */
    public static final String USE_ALL_OUTPUTS = "@";

    /**
     * Operator name used to represent "use any expression form"
     */
    public static final String FREE_EXPRESSION_FORM = "NONE";

    // ---------------- BEGIN SERIALIZATION -------------------

    public static final boolean IS_ROUNDING_DOUBLE = true;

    public static final double EVAL_NUMBER_PRECISION = 0.0001;

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.####");

    /**
     * this.field -> ,this + DELIMITER_FIELDS + field  we cannot use dot because it cannot be handled by the boolean evaluator
     */
    public static String DELIMITER_FIELDS = "_ACCESSING_FIELDS_";

    /**
     * this.size, - ,this + DELIMITER_METHODS + size    we need something to separate the variables names to the method name for object serialization
     */
    public static String DELIMITER_METHODS = "_ACCESSING_METHODS_";

    /**
     * this.contains(a) -> this + DELIMITER_METHODS + contains + _PARAMETER_METHOD_ a we need something to separate the variables names to the method name for object serialization
     */
    public static String DELIMITER_PARAMETERS_METHODS = "_PARAMETER_METHOD_";


    /**
     * a.m().m1() -> a.m()_PARAMETER_SPECIAL_m1()
     */
    public static String DELIMITER_SPECIAL = "_PARAMETER_SPECIAL_";

    // Don't need this for now
    // public static ISerialization serializer = new HybridSerialization();
    /**
     *
     */
    public static boolean SERIALIZE_ALSO_NONTHIS_PARAM = true;

    /**
     * true if we also want the private fields
     */
    public static boolean SERIALIZE_ONLY_PUBLIC_FIELDS = false;

    /**
     * how deep should I serialize? (for state serialization)
     */
    public static final int MAX_DEPTH_SERIALIZATION = 1;


    // ---------------- END SERIALIZATION -------------------


    /**
     * Max value for number generation
     */
    public static float MAX_NUMBER = 100.0f;

    public static float PROB_MAGIC_CONSTANT = 0.1f;

    public static float PROB_INTEGER_CONSTANT = 0.5f;

    public static float PROB_LARGE_CONSTANT = 0.5f;

    public static float PROB_NEGATIVE = 0.5f;

    public static int NUMBER_TOP_FOR_REDUCING = 5;

    public static float PROB_UNARY = 0.3f;


    // ---------------- BEGIN TREE GENERATION -------------------


    /**
     * probability of using a boolean expression with math operands
     * <p>
     * e.g., x > 5 VS NOT(B)
     */
    public static float PROB_BOOL_WITH_MATH = 0.3f;
    public static float PROB_BOOL_WITH_SEQUENCE = 0.3f;
    public static float PROB_MATH_WITH_SEQUENCE = 0.3f;
    public static float PROB_SEQUENCE_WITH_MATH = 0.3f;

    /**
     * probability of using a variable rather than a literals
     * <p>
     * e.g., y > x VS y > 10
     */
    public static final float PROB_CONSTANT_MIN = 0.10f;
    public static final float PROB_CONSTANT_MAX = 0.30f;
    public static final float PROB_CONSTANT_DIFF = PROB_CONSTANT_MAX - PROB_CONSTANT_MIN;
    public static float COMPUTE_PROB_CONSTANT(int generation) {
        return Math.min(PROB_CONSTANT_MAX, PROB_CONSTANT_MIN +
                PROB_CONSTANT_DIFF * (((float)generation) / (float)ESTIMATED_LAST_GENERATION));
    }

    /**
     * max depth of a generated tree
     */
    public static int MAX_DEPTH_TREE = 6;
    public static int MAX_DEPTH_IR_TREE = MAX_DEPTH_TREE - 1;
    public static int MAX_DEPTH_OR_TREE = MAX_DEPTH_TREE - 1;

    /**
     * max complexity
     * <p>
     * will not generate assertion with complexity higher than this
     */
    public static int MAX_COMPLEXITY = 32;
    public static int MAX_IR_COMPLEXITY = MAX_COMPLEXITY / 2;
    public static int MAX_OR_COMPLEXITY = MAX_COMPLEXITY / 2;

    /**
     * Maximum and minimum percentage of TestExecutions which a generated MRIP can satisfy.
     * Useful to avoid generating MRIPs which are trivially always/never satisfied.
     *
     * Only used for full MR generation (ch.usi.gassert.Mode.METAMORPHIC_FULL).
     */
    public static double MR_INPUT_RELATION_SATISFACTION_THRESHOLD_MIN = .05;
    public static double MR_INPUT_RELATION_SATISFACTION_THRESHOLD_MAX = .50;

    // ---------------- END TREE GENERATION -------------------

    // -------------- BEGIN CONJUNCTIVE MRIP GENERATION ---------------

    /**
     * Configuration for generating a new ConjunctiveClausesTree
     */
    public static double PROB_CONJUNCTIVE_MRIP_CLAUSE_SELECTED = .8;
    public static double PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAXED = .2;

    /**
     * Configuration for ConjunctiveClausesTree Clause relaxation
     */
    public static double PROB_CONJUNCTIVE_MRIP_CLAUSE_RELAX_RESET = .5;

    // -------------- END CONJUNCTIVE MRIP GENERATION ---------------

    // ---------------- EVOLUTIONARY ALGORITHM -------------------

    /**
     * max population size
     */
    public static int POPULATION_SIZE = 1000;

    public static int sizeForElitism = (int) (POPULATION_SIZE * 0.02);
    public static int sizeForMigration = (int) (POPULATION_SIZE * 0.16);

    public static boolean ELITE_UNIQUE_SIGNATURES = true;

    public static boolean IS_CACHE_ENABLED = false;
    public static int MAX_SIZE_CACHE = 100000;

    public static int countBestIndividuals = 10;

    /**
     * local search for the best value for the constants
     */
    public static final int CONSTANT_VALUES_SEARCH_GEN = 10;
    public static final int CONSTANT_VALUES_SEARCH_PLATEAU = 16;
    public static final double CONSTANT_VALUES_SEARCH_STEP = 0.01;
    public static final Consumer<VariablesList> CONSTANT_VALUES_SEARCH_FN =
            (v) -> AVMSearch.search(v, CONSTANT_VALUES_SEARCH_PLATEAU);

    /**
     * max number of generation
     */
    public static final int MAX_GENERATION = 10000;
    /**
     * min number of generation
     */
    public static final int MIN_GENERATION = 100;
    /**
     * estimated generation where the time budget will expire
     */
    public static final int ESTIMATED_LAST_GENERATION = 200;

    public static int TIME_BUDGET_MINUTES = 10;
    /**
     * probability crossover
     */
    public static float PROB_CROSSOVER = 0.90F;

    /**
     * probability mutation
     */
    public static float PROB_MUTATION = 0.30F;

    /**
     * probability SubTreeMutation generates leaf
     */
    public static float PROB_SUBTREE_LEAF_MUTATION = 0.70F;

    /**
     * proportion of randomly generated assertion for initial population
     */
    public static float PROPORTION_RANDOM_INIT = 0.50F;

    /**
     * proportion of initial assertions generated by mutating the assertions in input
     */
    public static float PROPORTION_MUTATED_INIT = 0.50F;

    /**
     * this is the theta for the complexity penalization
     */
    public static int THETA = 5;

    /**
     * this is the theta for the complexity penalization
     */
    public static boolean IS_COMPLEXITY_PENALIZED = false;

    public static int generationElitism = 1;

    public static boolean IS_MINIMIZATION_ENABLED = false;

    public static int generationMinimization = 100;

    public static int generationMigration = 10;

    public static int generationSaveState = 10;

    /**
     *
     */
    public static final boolean IS_ELITE_VALID_REWARDED = true;

    public static final double ELITE_VALID_REWARD_MIN = 1.0;
    public static final double ELITE_VALID_REWARD_MAX = 0.7;

    public static IntFunction<Double> COMPUTE_ELITE_VALID_REWARD = generation -> {
        final double progress = Math.min(1.0, (((double)generation) / (double)ESTIMATED_LAST_GENERATION));
        return ELITE_VALID_REWARD_MIN * (1.0 - progress) + ELITE_VALID_REWARD_MAX * progress;
    };

    /**
     * probability that the best selection uses the same type of the fitness function
     */
    public static final float PROB_BEST_SELECTION_SAME_TYPE = 0.70f;

    public static final int MAX_SIZE_FOR_BEST_MATCHING_LIST = 10;

    public static final float PROB_MERGE_CROSSOVER_AS_IT_IS = 0.70f;

    public static final double CONSTANT_VALUE_MUTATION_WEIGHT_MIN = 5.0;
    public static final double CONSTANT_VALUE_MUTATION_WEIGHT_MAX = 50.0;
    public static final double CONSTANT_VALUE_MUTATION_WEIGHT_DIFF =
            CONSTANT_VALUE_MUTATION_WEIGHT_MAX - CONSTANT_VALUE_MUTATION_WEIGHT_MIN;
    public static double COMPUTE_CONSTANT_VALUE_MUTATION_WEIGHT(int generation) {
        return Math.min(CONSTANT_VALUE_MUTATION_WEIGHT_MAX, CONSTANT_VALUE_MUTATION_WEIGHT_MIN +
                CONSTANT_VALUE_MUTATION_WEIGHT_DIFF * (((double)generation) / (double)ESTIMATED_LAST_GENERATION));
    }

    /**
     * IStatesUpdater
     */
    public static Integer GENERATION_STATES_UPDATE_TIME_BUDGET(int generation, long remainingBudgetMillis) {
        if (remainingBudgetMillis <= (8L * 60L * 1000L) ) {
            // Do not run StatesUpdater if remaining time budget is less than 8 minutes
            return null;
        }
        if (generation == 30) {
            return 3;
        }
        if (generation == 60) {
            return 4;
        }
        if (generation % 100 == 0) {
            return 6;
        }
        return null;
    }

    // ---------------- END EVOLUTIONARY ALGORITHM -------------------

}



