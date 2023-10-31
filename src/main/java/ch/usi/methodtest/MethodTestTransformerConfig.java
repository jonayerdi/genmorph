package ch.usi.methodtest;

import ch.usi.methodtest.transformations.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.Assert.assertProperty;

public class MethodTestTransformerConfig {

    public static final String NUMERIC_FORMAT = "%.6f";

    public static final double CONSTANTS_EPSILON = 0.1;

    public static final double[] CONSTANTS_IGNORED = new double[] {
            0, 1, -1
    };

    public static final List<MethodTestTransformation> DEFAULT_TRANSFORMATIONS = Arrays.asList(
            new SwitchParams(),
            new BooleanFlip(),
            new NumericAddition(1),
            new NumericAddition(-1),
            new NumericMultiplication(-1),
            new SequenceFlip(),
            new SequenceRemove(0),
            new SequenceRemove(-1)
    );

    public static boolean constantEquals(final double a, final double b) {
        return Math.abs(a - b) < CONSTANTS_EPSILON;
    }

    public static boolean constantEqualsAny(final double a, final double[] values) {
        for (final double value : values) {
            if (constantEquals(a, value)) {
                return true;
            }
        }
        return false;
    }

    public static List<Double> filterConstants(final List<Double> constants) {
        final List<Double> filteredConstants = new ArrayList<>(constants.size());
        constants.sort(null);
        double previous = Double.POSITIVE_INFINITY;
        for (final Double constant : constants) {
            if (!constantEquals(constant, previous)) {
                filteredConstants.add(constant);
            } else {
                previous = constant;
            }
        }
        return filteredConstants;
    }

    public static boolean useConstant(final double constant) {
        return Double.isFinite(constant) 
            && !constantEqualsAny(constant, CONSTANTS_IGNORED)
            && constant >= -100.0
            && constant <= 100.0;
    }

    public static List<MethodTestTransformationFactory.Transformation> loadTransformationsFromFile(final File file, boolean params) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> MethodTestTransformationFactory.fromString(l, params))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + file.getName(), e);
        }
    }

    public static void writeTransformationsToFile(final File file, final List<MethodTestTransformation> transformations) {
        try (final Writer writer = new FileWriter(file)) {
            for (final MethodTestTransformation transformation : transformations) {
                writer.write(MethodTestTransformationFactory.toString(transformation));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing " + file.getName(), e);
        }
    }

    public static void writeTransformationsToFile(final File file, final List<MethodTestTransformation> transformations, final MethodTest methodTest) {
        try (final Writer writer = new FileWriter(file)) {
            for (final MethodTestTransformation transformation : transformations) {
                for (final List<Integer> params : transformation.findTransformations(methodTest)) {
                    writer.write(MethodTestTransformationFactory.toString(transformation, params));
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing " + file.getName(), e);
        }
    }

    public static List<MethodTestTransformation> makeTransformationsFromConstants(final double[] constants) {
        final List<MethodTestTransformation> transformations = new ArrayList<>(DEFAULT_TRANSFORMATIONS.size() + constants.length);
        transformations.addAll(DEFAULT_TRANSFORMATIONS);
        for(final double constant : constants) {
            if (useConstant(constant)) {
                transformations.add(new NumericAddition(constant));
                transformations.add(new NumericMultiplication(constant));
            }
        }
        return transformations;
    }

    public static List<Double> loadConstantsList(final File file) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> Double.parseDouble(
                        assertProperty(
                            l.split(","),
                            r -> r.length == 2,
                            "Invalid row in constants list: " + l
                        )[0]
                    ))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + file.getName(), e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.err.println("Wrong number of parameters: 2 or 3 arguments expected, got " + args.length);
            System.err.println("Constants file (input)");
            System.err.println("Transformations file (output)");
            System.err.println("MethodTest file (input, optional)");
            System.exit(1);
        }
        final Iterator<String> arguments = Arrays.stream(args).iterator();
        final File constantsFile = new File(arguments.next());
        final File transformationsFile = new File(arguments.next());
        final List<Double> constantsRaw = loadConstantsList(constantsFile);
        final List<Double> constantsFiltered = filterConstants(constantsRaw);
        final List<MethodTestTransformation> transformations = makeTransformationsFromConstants(
                constantsFiltered.stream().mapToDouble(Double::doubleValue).toArray()
        );
        if (args.length == 3) {
            final MethodTest methodTest = MethodTest.fromXML(new File(arguments.next()));
            writeTransformationsToFile(transformationsFile, transformations, methodTest);
        } else {
            writeTransformationsToFile(transformationsFile, transformations);
        }
    }

}
