package ch.usi.gassert.mrip;

import ch.usi.gassert.data.manager.method.MethodDataManager;
import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.VariablesHelper;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.evaluator.BasicEvaluator;
import ch.usi.gassert.evaluator.IEvaluator;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.testcase.metamorphic.MRInfoDB;
import org.mu.util.streams.StreamLoaderFactory;

import java.io.*;
import java.util.*;

import static ch.usi.gassert.util.Assert.assertAlways;

public class MRInfoGenerator {

    public static Map<String, Tree> loadMRIPs(final String mripsFile, final Map<String, Class<?>> variableTypes) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(mripsFile))) {
            return loadMRIPs(reader, variableTypes);
        } catch (IOException e) {
            throw new RuntimeException("Error reading MRIPs file: " + mripsFile, e);
        }
    }

    public static Map<String, Tree> loadMRIPs(final BufferedReader mripsReader, final Map<String, Class<?>> variableTypes) {
        final Map<String, Tree> mrips = new HashMap<>();
        final Iterator<String> lines = mripsReader.lines().iterator();
        while (lines.hasNext()) {
            String mripName = lines.next();
            if (mripName == null) {
                assertAlways(!lines.hasNext(), "Error parsing MRIPs file");
                break;
            }
            mripName = mripName.trim();
            if (mripName.isEmpty()) {
                assertAlways(!lines.hasNext(), "Error parsing MRIPs file");
                break;
            }
            final String mripExpression = lines.next();
            final Tree mripTree = TreeReaderGAssert.getTree(mripExpression, variableTypes);
            mrips.put(mripName, mripTree);
        }
        return mrips;
    }

    public static MRInfoDB generateMRInfos(final Map<String, Tree> mrips, final Collection<TestExecution> testExecutions) {
        final List<MRInfo> mrinfos = new ArrayList<>();
        final IEvaluator evaluator = new BasicEvaluator();
        for (final TestExecution source : testExecutions) {
            for (final TestExecution followup : testExecutions) {
                if (source != followup) {
                    final Map<String, Object> variables =
                            VariablesHelper.makeMetamorphic(source.getVariables(), followup.getVariables()).getValues();
                    for (final String mripName : mrips.keySet()) {
                        final Tree mripTree = mrips.get(mripName);
                        final boolean mripSatisfied = evaluator.eval(mripTree, variables);
                        if (mripSatisfied) {
                            mrinfos.add(new MRInfo(mripName, source.getTestId(), followup.getTestId()));
                        }
                    }
                }
            }
        }
        return new MRInfoDB(mrinfos);
    }

    public static void main(String[] args) {
        // Parse args
        if (args.length != 4) {
            System.err.println("Wrong number of parameters: 4 arguments expected, got " + args.length);
            System.err.println("States directory");
            System.err.println("Original system ID");
            System.err.println("MRIPs file");
            System.err.println("MRInfos file");
        }
        Iterator<String> arguments = Arrays.stream(args).sequential().iterator();
        final String statesDir = arguments.next();
        final String originalSystemId = arguments.next();
        final String mripsFile = arguments.next();
        final File mrinfosFile = new File(arguments.next());
        mrinfosFile.getParentFile().mkdirs();
        // Load TestExecutions from file
        final Collection<TestExecution> testExecutions = MethodDataManager.loadAllTestExecutions(
                StreamLoaderFactory.forPath(statesDir)
        ).get(originalSystemId).values();
        System.out.println("Loaded " + testExecutions.size() + " test executions");
        // Select any test execution and infer variable types
        final TestExecution testExecution = testExecutions.stream().findAny().orElse(null);
        final Map<String, Class<?>> variableTypes
                = testExecution != null
                    ? VariablesManager.fromVariableValues(testExecution.getVariables().getValues())
                        .makeMetamorphic()
                        .getVariableTypes()
                    : new HashMap<>();
        System.out.println("Using variables:");
        for (String v : variableTypes.keySet()) {
            System.out.println("  " + v + ": " + variableTypes.get(v).getSimpleName());
        }
        // Load MRIPs from file
        final Map<String, Tree> mrips = loadMRIPs(mripsFile, variableTypes);
        System.out.println("Loaded " + mrips.size() + " MRIPs");
        // Generate MRInfos
        final MRInfoDB mrinfos = generateMRInfos(mrips, testExecutions);
        // Write MRInfos
        try (final Writer writer = new FileWriter(mrinfosFile)) {
            mrinfos.toCsv(writer);
        } catch (IOException e) {
            throw new RuntimeException("Error writing MRInfos file: " + mrinfosFile, e);
        }
    }
}
