package ch.usi.gassert;

import ch.usi.gassert.data.manager.LoaderUtils;
import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.evaluator.BasicEvaluator;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.MR;
import ch.usi.gassert.util.StringUtils;
import com.google.gson.stream.JsonReader;
import org.mu.testcase.classification.Classification;
import org.mu.testcase.classification.TestClassifications;
import org.mu.testcase.evaluation.TestEvaluationResults;
import org.mu.testcase.evaluation.TestResult;
import org.mu.util.streams.IStreamLoader;
import org.mu.util.streams.StreamLoaderFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.usi.gassert.util.Assert.assertAlways;
import static org.mu.testcase.classification.Classification.CORRECT;
import static org.mu.testcase.evaluation.TestResult.FN;
import static org.mu.testcase.evaluation.TestResult.FP;
import static org.mu.testcase.evaluation.TestResult.TN;
import static org.mu.testcase.evaluation.TestResult.TP;

public class EvaluateMRs {

    public static final String OR_EXTENSION = ".or.txt";
    public static final String RESULTS_EXTENSION = ".results.csv";

    public static Map<String, Map<String, Map<String, Object>>> loadStates(final IStreamLoader statesDataSource,
                                                                           final Function<String, String> variableNamesMapping) {
        // Load inputs
        final Map<String, Map<String, Map<String, Object>>> states = new HashMap<>();
        for (String entry : statesDataSource.entries()) {
            if (entry.endsWith(".state.json")) {
                try {
                    final JsonReader reader = new JsonReader(statesDataSource.load(entry));
                    final TestExecution testExecution = TestExecution.fromJson(reader);
                    final boolean hasInvalidValue = testExecution.getVariables().getValues().values().stream()
                            .anyMatch(v -> v == null || ClassUtils.isErrorType(v.getClass()));
                    reader.close();
                    // Exclude test executions with invalid values
                    if (!hasInvalidValue) {
                        states.putIfAbsent(testExecution.getSystemId(), new HashMap<>());
                        final Map<String, Object> state = testExecution.getVariables().getValues();
                        final Map<String, Object> stateVariableNamesMapped = new HashMap<>(state.size());
                        for (final String var : state.keySet()) {
                            stateVariableNamesMapped.put(variableNamesMapping.apply(var), state.get(var));
                        }
                        final Map<String, Object> previous = states.get(testExecution.getSystemId())
                                .put(testExecution.getTestId(), stateVariableNamesMapped);
                        if (previous != null) {
                            System.err.println("Duplicate TestExecution for systemId=" + testExecution.getSystemId()
                                    + " testId=" + testExecution.getTestId());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error loading inputs entry: " + entry, e);
                }
            }
        }
        return states;
    }

    public static TestResult getResultFromVerdict(final Classification sourceClassification,
                                                  final Classification followupClassification,
                                                  final boolean verdict) {
        if (sourceClassification == CORRECT && followupClassification == CORRECT) {
            return verdict ? TN : FP;
        } else if (sourceClassification != CORRECT && followupClassification != CORRECT) {
            return verdict ? FN : TP;
        } else {
            throw new RuntimeException("Invalid source:followup classifications: " + sourceClassification + ":" + followupClassification);
        }
    }

    public static void evaluateMR(final Map<String, Map<String, Map<String, Object>>> sourceStates,
                                  final Map<String, TestClassifications> sourceClassifications,
                                  final Map<String, Integer> systemIds, final Map<String, Integer> systemIdsShort, final Map<String, Integer> testIds,
                                  final Path followupStatesDir, final Path followupClassificationsDir,
                                  final IEvaluator evaluator, final Map<String, Class<?>> variableTypes,
                                  final File mr, final File resultsMR) {
        try {
            final String filename = mr.getName();
            final String mrName = filename.substring(0, filename.length() - OR_EXTENSION.length());
            // Init results matrix
            final TestResult[][] results = new TestResult[testIds.size()][systemIds.size()];
            for (TestResult[] column : results) {
                Arrays.fill(column, TestResult.NONE);
            }
            // Read output relation
            final String orString = FileUtils.readContentFile(mr);
            final Tree or = TreeReaderGAssert.getTree(orString, variableTypes);
            // Load followup states
            final IStreamLoader sourceStatesStream = StreamLoaderFactory.forPath(followupStatesDir.toString());
            final Map<String, Map<String, Map<String, Object>>> followupStates = loadStates(sourceStatesStream, MR::VARIABLE_TO_FOLLOWUP);
            // Load followup classifications
            final IStreamLoader sourceClassificationsStream = StreamLoaderFactory.forPath(followupClassificationsDir.toString());
            final Map<String, TestClassifications> followupClassifications = LoaderUtils.loadClassifications(sourceClassificationsStream);
            // Evaluate output relation on all states
            final int classificationsColumnIndex = 0;
            for (final String systemId : followupClassifications.keySet()) {
                final TestClassifications sourceClassificationsSystem = sourceClassifications.get(systemId);
                final TestClassifications followupClassificationsSystem = followupClassifications.get(systemId);
                for (final String followupTestId : followupClassificationsSystem.rowIds.keySet()) {
                    assertAlways(followupTestId.endsWith("followup"), "Invalid followup testId: " + followupTestId);
                    final String testId = followupTestId.substring(0, followupTestId.length() - "followup".length());
                    try {
                        final  Map<String, Map<String, Object>> EMPTY_MAP = new HashMap<>(0);
                        final Map<String, Object> sourceVariables = sourceStates.getOrDefault(systemId, EMPTY_MAP).get(testId);
                        final Map<String, Object> followupVariables = followupStates.getOrDefault(systemId, EMPTY_MAP).get(followupTestId);
                        if (sourceVariables == null || followupVariables == null) {
                            continue;
                        }
                        final Integer classificationsSourceRowIndex = sourceClassificationsSystem.rowIds.get(testId);
                        final Integer classificationsFollowupRowIndex = followupClassificationsSystem.rowIds.get(followupTestId);
                        if (classificationsSourceRowIndex == null || classificationsFollowupRowIndex == null) {
                            continue;
                        }
                        final Classification sourceClassification = sourceClassificationsSystem.get(classificationsSourceRowIndex, classificationsColumnIndex);
                        final Classification followupClassification = followupClassificationsSystem.get(classificationsFollowupRowIndex, classificationsColumnIndex);
                        if (sourceClassification == Classification.NONE && followupClassification == Classification.NONE) {
                            continue;
                        }
                        final Map<String, Object> variables = Stream.concat(sourceVariables.entrySet().stream(), followupVariables.entrySet().stream())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        assertAlways(sourceVariables.size() == followupVariables.size(), "");
                        assertAlways(variables.size() == sourceVariables.size() + followupVariables.size(), "");
                        boolean verdict = evaluator.eval(or, variables);
                        final int row = testIds.get(testId);
                        final int column = systemIds.get(systemId);
                        TestResult result = getResultFromVerdict(sourceClassification, followupClassification, verdict);
                        results[row][column] = result;
                    } catch (Exception e) {
                        throw new RuntimeException("Error handling " + systemId + "!" + testId, e);
                    }
                }
            }
            // Write results
            final TestEvaluationResults tableResults = new TestEvaluationResults(mrName, testIds, systemIdsShort, results);
            final Writer writer = new BufferedWriter(new FileWriter(resultsMR));
            tableResults.writeTo(writer);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Error handling MR: " + mr.getPath(), e);
        }
    }

    public static void evaluateMRs(final Path mrsDir, final String sut,
                                   final Path sourceStatesDir, final Path followupStatesDir,
                                   final Path sourceClassificationsDir, final Path followupClassificationsDir,
                                   final Path resultsDir) {
        // Load source states
        final IStreamLoader sourceStatesStream = StreamLoaderFactory.forPath(sourceStatesDir.resolve(sut).toString());
        final Map<String, Map<String, Map<String, Object>>> sourceStates = loadStates(sourceStatesStream, MR::VARIABLE_TO_SOURCE);
        // Load source classifications
        final IStreamLoader sourceClassificationsStream = StreamLoaderFactory.forPath(sourceClassificationsDir.resolve(sut).toString());
        final Map<String, TestClassifications> sourceClassifications = LoaderUtils.loadClassifications(sourceClassificationsStream);
        // Init other stuff
        final IEvaluator evaluator = new BasicEvaluator();
        final Map<String, Object> someStateVariables = sourceStates
                .values().stream().findAny().orElseThrow(() -> new RuntimeException("sourceStates has no systemIds: " + sut))
                .values().stream().findAny().orElseThrow(() -> new RuntimeException("sourceStates has no testIds: " + sut));
        final Map<String, Class<?>> variableTypesSource = VariablesManager.fromVariableValues(someStateVariables).getVariableTypes();
        final Map<String, Class<?>> variableTypes = new HashMap<>(variableTypesSource.size() * 2);
        for (final Map.Entry<String, Class<?>> sourceVar : variableTypesSource.entrySet()) {
            final String sourceVarName = sourceVar.getKey();
            final String followupVarName = MR.VARIABLE_TO_FOLLOWUP(MR.SOURCE_TO_VARIABLE(sourceVarName));
            final Class<?> sourceVarType = sourceVar.getValue();
            variableTypes.put(sourceVarName, sourceVarType);
            variableTypes.put(followupVarName, sourceVarType);
        }
        final List<String> sortedSystemIds = sourceStates.keySet().stream().sorted().collect(Collectors.toList());
        final String lastSystemId = sortedSystemIds.remove(sortedSystemIds.size() - 1);
        sortedSystemIds.add(lastSystemId.endsWith("original") ? 0 : (sortedSystemIds.size() - 1), lastSystemId);
        final Map<String, Integer> systemIds = new HashMap<>(sortedSystemIds.size());
        for (int i = 0 ; i < sortedSystemIds.size() ; ++i) {
            systemIds.put(sortedSystemIds.get(i), i);
        }
        final int commonPrefixIndex = StringUtils.getCommonPrefixIndex(systemIds.keySet());
        final Map<String, Integer> systemIdsShort = new HashMap<>(sortedSystemIds.size());
        for (final String systemId : systemIds.keySet()) {
            systemIdsShort.put(systemId.substring(commonPrefixIndex, systemId.length()), systemIds.get(systemId));
        }
        final List<String> sortedTestIds = sourceStates.values().stream()
                .flatMap(s -> s.keySet().stream())
                .distinct().sorted()
                .collect(Collectors.toList());
        final Map<String, Integer> testIds = new HashMap<>(sortedTestIds.size());
        for (int i = 0 ; i < sortedTestIds.size() ; ++i) {
            testIds.put(sortedTestIds.get(i), i);
        }
        // Iterate followupClassificationsDir and write results 
        for (final File followupClassificationsDirStrategy : Objects.requireNonNull(followupClassificationsDir.toFile().listFiles())) {
            final String strategy = followupClassificationsDirStrategy.getName();
            final Path resultsDirStrategySut = resultsDir.resolve(strategy).resolve(sut);
            final File resultsDirStrategySutFile = resultsDirStrategySut.toFile();
            assertAlways(resultsDirStrategySutFile.mkdirs() || resultsDirStrategySutFile.isDirectory(), "Error creating directory: " + resultsDirStrategySut);
            final Path followupClassificationsDirStrategySut = followupClassificationsDirStrategy.toPath().resolve(sut);
            for (final File followupClassificationsDirMrip : Objects.requireNonNull(followupClassificationsDirStrategySut.toFile().listFiles())) {
                final String mrip = followupClassificationsDirMrip.getName();
                final Path followupStatesDirMrip = followupStatesDir.resolve(strategy).resolve(sut).resolve(mrip);
                final File resultsMR = resultsDirStrategySut.resolve(mrip + RESULTS_EXTENSION).toFile();
                final File mr = mrsDir.resolve(strategy).resolve(sut).resolve(mrip + OR_EXTENSION).toFile();
                evaluateMR(
                        sourceStates, sourceClassifications,
                        systemIds, systemIdsShort, testIds,
                        followupStatesDirMrip, followupClassificationsDirMrip.toPath(),
                        evaluator, variableTypes,
                        mr, resultsMR
                );
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 7) {
            System.err.println("Wrong number of parameters: 7 arguments expected, got " + args.length);
            System.err.println("MRs dir");
            System.err.println("Source states dir");
            System.err.println("Followup states dir");
            System.err.println("Source classifications dir");
            System.err.println("Followup classifications dir");
            System.err.println("SUTs (';' separated)");
            System.err.println("Results dir");
            System.exit(-1);
        }
        final Iterator<String> arguments = Arrays.stream(args).iterator();
        final String mrsDir = arguments.next();
        final String sourceStatesDir = arguments.next();
        final String followupStatesDir = arguments.next();
        final String sourceClassificationsDir = arguments.next();
        final String followupClassificationsDir = arguments.next();
        final String[] suts = arguments.next().split(";");
        final String resultsDir = arguments.next();
        for (final String sut : suts) {
            evaluateMRs(
                new File(mrsDir).toPath(), sut,
                new File(sourceStatesDir).toPath(), new File(followupStatesDir).toPath(),
                new File(sourceClassificationsDir).toPath(), new File(followupClassificationsDir).toPath(),
                new File(resultsDir).toPath()
            );
        }
    }

}
