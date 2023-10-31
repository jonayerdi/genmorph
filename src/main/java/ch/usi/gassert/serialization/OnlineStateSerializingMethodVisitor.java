package ch.usi.gassert.serialization;

import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.Variables;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MethodVisitor that serializes all the TestExecutions into the observedTestExecutions list.
 */
/*
 * Serializer calls added indirectly by ch.usi.gassert.filechange.AddInstrumentationMethod
 */
@SuppressWarnings("unused")
public class OnlineStateSerializingMethodVisitor {

    public static final AtomicInteger CurrentTestId = new AtomicInteger(0);

    public static final OnlineStateSerializingMethodVisitor INSTANCE = new OnlineStateSerializingMethodVisitor(DefaultSerializer.getInstance(), false);

    public static OnlineStateSerializingMethodVisitor getInstance() {
        return INSTANCE;
    }

    public static void enter(final Object... args) {
        INSTANCE.serializeEnter(args);
    }

    public static void exit(final Object... args) {
        INSTANCE.serializeExit(args);
    }

    public static List<TestExecution> getTestExecutions() {
        return INSTANCE.observedTestExecutions;
    }

    public static void clearTestExecutions() {
        INSTANCE.observedTestExecutions.clear();
    }

    private static String computeTestId(final long tid) {
        return String.valueOf(CurrentTestId.incrementAndGet());
    }

    public static String getSerializedVariableName(final String namespace, final String path, final boolean isInput) {
        final StringBuilder s = new StringBuilder();
        s.append(isInput ? "i_" : "o_");
        s.append(namespace);
        if (!path.isEmpty()) {
            s.append(".").append(path);
        }
        return s.toString();
    }

    public final ISerializer serializer;
    public final boolean serializeRecursiveCalls;

    // We are serializing/detecting recursive calls, hence the Stack
    private final Map<Long, Stack<TestExecution>> threadId2variables = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> serializedCount = new ConcurrentHashMap<>();
    public final Vector<TestExecution> observedTestExecutions = new Vector<>();

    public OnlineStateSerializingMethodVisitor(final ISerializer serializer, final boolean serializeRecursiveCalls) {
        this.serializer = serializer;
        this.serializeRecursiveCalls = serializeRecursiveCalls;
    }

    public void addVariables(final Variables variables, final boolean isInput, final Iterator<?> argsIter) {
        while (argsIter.hasNext()) {
            final String name = (String) argsIter.next();
            final Class<?> clazz = (Class<?>) argsIter.next();
            final Object value = argsIter.next();
            final boolean isRetval = (Boolean) argsIter.next();
            final Map<String, Object> vars = new HashMap<>();
            this.serializer.serialize(clazz, value, vars, isInput || isRetval);
            for (final String var : vars.keySet()) {
                variables.add(getSerializedVariableName(name, var, isInput), vars.get(var), isInput);
            }
        }
    }

    public void addInputVariables(final Variables variables, final Iterator<?> argsIter) {
        this.addVariables(variables, true, argsIter);
    }

    public void addOutputVariables(final Variables variables, final Iterator<?> argsIter) {
        this.addVariables(variables, false, argsIter);
    }

    public void serializeEnter(final Object... args) {
        final Iterator<Object> argsIter = Arrays.stream(args).iterator();
        final long tid = Thread.currentThread().getId();
        threadId2variables.putIfAbsent(tid, new Stack<>());
        final Stack<TestExecution> stack = threadId2variables.get(tid);
        if (serializeRecursiveCalls || stack.isEmpty()) {
            final String systemId = (String) argsIter.next();
            final String methodName = (String) argsIter.next();
            final int methodIndex = (int) argsIter.next();
            final String testId = computeTestId(tid);
            final TestExecution testExecution = new TestExecution(systemId, testId, new Variables());
            serializedCount.putIfAbsent(systemId, new HashMap<>());
            serializedCount.get(systemId).putIfAbsent(testId, 0);
            addInputVariables(testExecution.getVariables(), argsIter);
            stack.push(testExecution);
        } else {
            // We push a null value if we have a recursive call but do not want to serialize it
            stack.push(null);
        }
    }

    public void serializeExit(final Object... args) {
        final Iterator<?> argsIter = Arrays.stream(args).iterator();
        final long tid = Thread.currentThread().getId();
        final Stack<TestExecution> stack = threadId2variables.get(tid);
        if (stack.isEmpty()) {
            return; // exit() was called without a matching enter() somehow
        }
        TestExecution testExecution = stack.pop();
        // We push a null value if we have a recursive call but do not want to serialize it
        if (testExecution != null) {
            addOutputVariables(testExecution.getVariables(), argsIter);
            final int count = serializedCount.get(testExecution.getSystemId()).get(testExecution.getTestId());
            serializedCount.get(testExecution.getSystemId()).put(testExecution.getTestId(), count + 1);
            if (count > 0) {
                testExecution = new TestExecution(testExecution.getSystemId(),
                        testExecution.getTestId() + "_" + count, testExecution.getVariables());
            }
            observedTestExecutions.add(testExecution);
        }
    }

}
