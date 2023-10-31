package ch.usi.methodtest;

import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.methodtest.transformations.*;
import ch.usi.methodtest.transformations.MethodTestTransformationFactory.Transformation;

import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.usi.gassert.util.Assert.assertAlways;
import static ch.usi.gassert.util.Assert.assertProperty;
import static ch.usi.gassert.util.FileUtils.SEPARATORS;

public class MethodTestTransformer {

    /**
     * Get SUT from the filename of a .methodinputs file
     */
    public static String getSUTFromFilename(final String filename) {
        final String[] parts = filename.split(Pattern.quote(SEPARATORS[1]));
        assertAlways(parts.length >= 3, "Cannot get SUT name from filename: " + filename);
        final String clazz = parts[0];
        final String methodName = parts[1];
        final String methodIndex = parts[2].substring(0, parts[2].indexOf(SEPARATORS[0]));
        return String.join(SEPARATORS[1], clazz, methodName, methodIndex);
    }

    /**
     * We need to prepend i_ to all the variable names, because that's what
     * ch.usi.gassert.serialization.StateSerializingMethodVisitor does for the input variables
     */
    public static void processTree(final Tree tree) {
        if (tree != null) {
            if (tree.isLeaf()) {
                if (tree.hasVariables()) {
                    tree.setValue("i_" + tree.getValue().toString());
                }
            } else {
                processTree(tree.getLeft());
                processTree(tree.getRight());
            }
        }
    }

    public static void generateTransformedTestInputs(final List<Transformation> transformations,
                                                     final String originalFilename, final MethodTest methodTest,
                                                     final Set<String> inputVariables, final Path outputDir,
                                                     final Map<String, ConjunctiveMRIP> sutMRIPs) {
        final String inputsFilenameWithoutExtension = originalFilename.split("\\.(?=[^\\.]+$)")[0];
        // Enumerate all possible transformations
        for (final Transformation transform : transformations) {
            // Unpack variables
            final MethodTestTransformation transformation = transform.transformation;
            final List<Integer> paramIndices = transform.params;
            // Generate transformed MethodTest
            final List<MethodParameter> paramsTransformed = transformation.transform(
                paramIndices.stream().map(i -> methodTest.methodParameters[i]).collect(Collectors.toList())
            );
            final MethodTest transformedTest = new MethodTest(methodTest);
            for (int i = 0 ; i < paramIndices.size() ; ++i) {
                transformedTest.methodParameters[paramIndices.get(i)] = paramsTransformed.get(i);
            }
            // Write transformed MethodTest
            final String transformationName = MethodTestTransformationFactory.toString(transformation, paramIndices);
            final String transformedMethodTestFilename = inputsFilenameWithoutExtension
                    + SEPARATORS[0] + transformationName + MethodTest.EXTENSION;
            final File methodTestFile = outputDir.resolve(transformedMethodTestFilename).toFile();
            try {
                final Writer writer = new FileWriter(methodTestFile);
                transformedTest.toXML(writer);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error writing out transformed method inputs to " + methodTestFile.getName(), e);
            }
            // Generate ConjunctiveMRIP
            final List<String> transformedParamNames = paramsTransformed.stream().map(p -> p.name).collect(Collectors.toList());
            final List<ConjunctiveMRIPClause> transformedParamClauses = transformation.makeClauses(transformedParamNames);
            final Map<String, ConjunctiveMRIPClause> paramClauses = new HashMap<>(transformedParamNames.size());
            IntStream.range(0, transformedParamNames.size()).forEach(
                i -> paramClauses.put(transformedParamNames.get(i), transformedParamClauses.get(i))
            );
            final ConjunctiveMRIPClause[] clauses = inputVariables.stream()
                .map(v -> paramClauses.getOrDefault(v, MethodTestTransformation.makeDefaultClause(v)))
                .toArray(ConjunctiveMRIPClause[]::new);
            // Process ConjunctiveMRIPClause trees
            for (ConjunctiveMRIPClause clause : clauses) {
                processTree(clause.getTree());
            }
            final ConjunctiveMRIP mrip = new ConjunctiveMRIP(clauses);
            // Store ConjunctiveMRIP into Map
            ConjunctiveMRIP prevSutMRIP = sutMRIPs.putIfAbsent(transformationName, mrip);
            if (prevSutMRIP != null) {
                assertAlways(
                    mrip.equals(prevSutMRIP),
                        "Conflicting MRIP " + transformationName
                            + ":\nprev: " + prevSutMRIP + "\ncurr: " + mrip
                );
            }
        }
    }

    public static void generateTransformedTestInputs(final List<Transformation> transformations,
                                                     final List<File> inputsFiles, final File stateFile,
                                                     final Path outputDir) {
        // Read state file and extract inputs
        final Set<String> inputVariables;
        try (final JsonReader reader = new JsonReader(new FileReader(stateFile))) {
            inputVariables = TestExecution.fromJson(reader)
                    .getVariables()
                    .getInputs()
                    .stream()
                    .map(var -> assertProperty(var, v -> v.startsWith("i_"), "Invalid input variable name: " + var).substring("i_".length()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Error opening state file: " + stateFile.getName(), e);
        }
        final Map<String, Map<String, ConjunctiveMRIP>> mrips = new HashMap<>();
        // Iterate over .methodinputs files and generate transformed test inputs
        for (File inputsFile : inputsFiles) {
            try (final Reader reader = new FileReader(inputsFile)) {
                try {
                    final MethodTest methodTest = MethodTest.fromXML(reader);
                    final String inputsFilename = inputsFile.getName();
                    final String sutName = getSUTFromFilename(inputsFilename);
                    mrips.putIfAbsent(sutName, new HashMap<>());
                    generateTransformedTestInputs(transformations, inputsFilename,
                            methodTest, inputVariables, outputDir, mrips.get(sutName));
                } catch (Exception e) {
                    System.err.println("Error transforming method inputs " + inputsFile.getName() +":\n " + e);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error opening inputs file: " + inputsFile.getName(), e);
            }
        }
        // Write out ConjunctiveMRIPs
        for (final String sutName : mrips.keySet()) {
            final Map<String, ConjunctiveMRIP> sutMRIPs = mrips.get(sutName);
            for (final String mripName : sutMRIPs.keySet()) {
                final String mripFilename = sutName + SEPARATORS[0] + mripName + ".cmrip";
                final File mripFile = outputDir.resolve(mripFilename).toFile();
                try (final Writer writer = new FileWriter(mripFile)) {
                    sutMRIPs.get(mripName).write(writer);
                } catch (IOException e) {
                    throw new RuntimeException("Error writing out MRIP to " + mripFilename, e);
                }
            }
        }
    }

    public static List<Transformation> makeDefaultTransformations(final MethodTest methodTest) {
        final List<Transformation> transformations = new ArrayList<>();
        for (final MethodTestTransformation transformation : MethodTestTransformerConfig.DEFAULT_TRANSFORMATIONS) {
            for (final List<Integer> params : transformation.findTransformations(methodTest)) {
                transformations.add(new Transformation(transformation, params));
            }
        }
        return transformations;
    }

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Wrong number of parameters: 3 or 4 arguments expected, got " + args.length);
            System.err.println("Serialized inputs file or directory");
            System.err.println("State file");
            System.err.println("Output directory");
            System.err.println("[Transformations file]");
            System.exit(1);
        }
        final Iterator<String> arguments = Arrays.stream(args).iterator();
        final File serializedInputsFile = new File(arguments.next());
        final File stateFile = new File(arguments.next());
        final File outputDir = new File(arguments.next());
        final File transformationsFile;
        if (arguments.hasNext()) {
            transformationsFile = new File(arguments.next());
        } else {
            transformationsFile = null;
        }
        outputDir.mkdirs();
        final List<File> inputsFiles = new ArrayList<>();
        if (serializedInputsFile.isDirectory()) {
            inputsFiles.addAll(Arrays.asList(Objects.requireNonNull(serializedInputsFile.listFiles(
                    (file, name) -> name.endsWith(MethodTest.EXTENSION)))));
        } else if (serializedInputsFile.isFile()) {
            inputsFiles.add(serializedInputsFile);
        } else {
            throw new RuntimeException("Invalid input: " + serializedInputsFile.getName());
        }
        final List<Transformation> transformations = transformationsFile != null
                ? MethodTestTransformerConfig.loadTransformationsFromFile(transformationsFile, true)
                : makeDefaultTransformations(MethodTest.fromXML(inputsFiles.get(0)));
        generateTransformedTestInputs(transformations, inputsFiles, stateFile, outputDir.toPath());
    }

}
