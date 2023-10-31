package ch.usi.gassert.serialization;

import ch.usi.gassert.util.Pair;
import ch.usi.methodtest.MethodTestExecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.Assert.assertAlways;
import static ch.usi.gassert.util.FileUtils.SEPARATORS;
import static ch.usi.methodtest.MethodTestTransformerConfig.NUMERIC_FORMAT;

/*
 * Serializer calls added indirectly by ch.usi.gassert.filechange.AddInstrumentationMethodValues
 */
@SuppressWarnings("unused")
public class ValuesSerializingMethodVisitor {

    public static final ValuesSerializingMethodVisitor INSTANCE = new ValuesSerializingMethodVisitor();

    public static ValuesSerializingMethodVisitor getInstance() {
        return INSTANCE;
    }

    public static void enter(final Object... args) {
        INSTANCE.serializeEnter(args);
    }

    public static void exit(final Object... args) {
        INSTANCE.serializeExit(args);
    }

    // Generic methods that evaluate to the passed expression

    public static <T> T foundLiteral(final T value) {
        return INSTANCE.serializeFoundLiteral(value);
    }

    public static <T> T foundVariable(final int id, final T value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }

    // Overloads por primitive parameters

    public static boolean foundLiteral(final boolean value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static char foundLiteral(final char value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static byte foundLiteral(final byte value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static short foundLiteral(final short value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static int foundLiteral(final int value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static long foundLiteral(final long value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static float foundLiteral(final float value) {
        return INSTANCE.serializeFoundLiteral(value);
    }
    public static double foundLiteral(final double value) {
        return INSTANCE.serializeFoundLiteral(value);
    }

    public static boolean foundVariable(final int id, final boolean value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static byte foundVariable(final int id, final byte value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static short foundVariable(final int id, final short value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static int foundVariable(final int id, final int value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static long foundVariable(final int id, final long value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static float foundVariable(final int id, final float value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }
    public static double foundVariable(final int id, final double value) {
        return INSTANCE.serializeFoundVariable(id, value);
    }

    private static String computeTestId(final long tid) {
        return Objects.requireNonNull(MethodTestExecutor.currentTestId.get(tid));
    }

    final Path outdir;

    // We are avoiding recursive calls, hence the Stack
    private final Map<Long, Stack<String>> threadId2executionId = new ConcurrentHashMap<>();
    private final Map<String, Values> executionId2values = new ConcurrentHashMap<>();

    public ValuesSerializingMethodVisitor() {
        final File outdirfile = new File(System.getenv("gassert_outdir"));
        if (!outdirfile.isDirectory() && !outdirfile.mkdirs()) {
            throw new RuntimeException("Could not create output directory: " + outdirfile);
        }
        this.outdir = outdirfile.toPath();
    }

    public void serializeEnter(final Object... args) {
        final long tid = Thread.currentThread().getId();
        final Iterator<Object> argsIter = Arrays.stream(args).iterator();
        final String systemId = (String) argsIter.next();
        final String methodName = (String) argsIter.next();
        final int methodIndex = (int) argsIter.next();
        final String executionId = systemId + SEPARATORS[0] + computeTestId(tid);
        executionId2values.putIfAbsent(executionId, new Values());
        threadId2executionId.putIfAbsent(tid, new Stack<>());
        threadId2executionId.get(tid).push(executionId);
    }

    public void serializeExit(final Object... args) {
        final long tid = Thread.currentThread().getId();
        final String testId = computeTestId(tid);
        final Iterator<Object> argsIter = Arrays.stream(args).iterator();
        final String systemId = (String) argsIter.next();
        final String methodName = (String) argsIter.next();
        final int methodIndex = (int) argsIter.next();
        final String executionId = systemId + SEPARATORS[0] + computeTestId(tid);
        assertAlways(threadId2executionId.get(tid).pop().equals(executionId), "Called exit without matching enter");
        final Values values = executionId2values.remove(executionId);
        writeValuesList(outdir.resolve(executionId + ".literals.txt").toFile(), values.getLiterals()
                .entrySet().stream()
                .map(e -> "" + e.getKey() + "," + e.getValue())
                .collect(Collectors.toList())
        );
        writeValuesList(outdir.resolve(executionId + ".variables.txt").toFile(), values.getVariables()
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "" + e.getKey() + "," + e.getValue().get().fst+ "," + e.getValue().get().snd)
                .collect(Collectors.toList())
        );
    }

    public <T> T serializeFoundLiteral(final T value) {
        final long tid = Thread.currentThread().getId();
        final Stack<String> executionIds = threadId2executionId.get(tid);
        assertAlways(!executionIds.empty(), "Called foundLiteral before enter");
        // if systemIds.size() > 1, then it's a recursive call, which we ignore
        if (executionIds.size() == 1 && value instanceof Number) {
            final Values values = executionId2values.get(executionIds.peek());
            values.addLiteral(String.format(NUMERIC_FORMAT, ((Number)value).doubleValue()));
        }
        return value;
    }

    public <T> T serializeFoundVariable(final int id, final T value) {
        final long tid = Thread.currentThread().getId();
        final Stack<String> executionIds = threadId2executionId.get(tid);
        assertAlways(!executionIds.empty(), "Called foundVariable before enter");
        // if systemIds.size() > 1, then it's a recursive call, which we ignore
        if (executionIds.size() == 1 && value instanceof Number) {
            final Values values = executionId2values.get(executionIds.peek());
            values.addVariableValue(id, String.format(NUMERIC_FORMAT, ((Number)value).doubleValue()));
        }
        return value;
    }

    public static void writeValuesList(final File file, final Collection<String> values) {
        //assertAlways(!file.exists(), file.getName() + " already exists");
        try (final Writer writer = new FileWriter(file)) {
            for(final String value : values) {
                writer.write(value);
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing to " + file.getName(), e);
        }
    }

    static class Values {

        public Map<String, Integer> literals;
        public Map<Integer, Optional<Pair<String, Integer>>> variables;

        public Values() {
            literals = new HashMap<>(8);
            variables = new HashMap<>(64);
        }

        public void addLiteral(final String value) {
            final int count = literals.getOrDefault(value, 0);
            literals.put(value, count + 1);
        }

        public void addVariableValue(final int id, final String value) {
            if (variables.containsKey(id)) {
                final Optional<Pair<String, Integer>> current = variables.get(id);
                if (current.isPresent()) {
                    if (current.get().fst.equals(value)) {
                        variables.put(id, Optional.of(Pair.of(value, current.get().snd + 1)));
                    } else {
                        variables.put(id, Optional.empty());
                    }
                }
            } else {
                variables.put(id, Optional.of(Pair.of(value, 1)));
            }
        }

        public Map<String, Integer> getLiterals() {
            return literals;
        }

        public Map<Integer, Optional<Pair<String, Integer>>> getVariables() {
            return variables;
        }

    }

}
