package ch.usi.methodtest;

import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.Tuple3;
import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.Assert.assertAlways;
import static ch.usi.gassert.util.FileUtils.SEPARATORS;

public final class MethodTestExecutor {

    public static Map<Long, String> currentTestId = new ConcurrentHashMap<>();

    private static final XStream xstream = new XStream();

    static {
        xstream.allowTypesByWildcard(new String[] {
                "**"
        });
    }

    public static String getMethodSignature(Class<?> clazz, String method, Class<?>[] params) {
        final StringBuilder sb = new StringBuilder();
        sb.append(clazz.getName());
        sb.append('.');
        sb.append(method);
        sb.append('(');
        for (Class<?> paramClazz : params) {
            sb.append(paramClazz.getName());
            sb.append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    public static Method findMethod(Class<?> clazz, String method, Class<?>[] params) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(method, params);
    }

    public static void executeMethodTest(final MethodTest methodTest, final String testId)
            throws ExecutionException, InterruptedException, TimeoutException {
        executeMethodTest(methodTest, testId, args -> {
            try {
                return findMethod(args.a, args.b, args.c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void executeMethodTest(final MethodTest methodTest,
                                         final String testId,
                                         final Function<Tuple3<Class<?>, String, Class<?>[]>, Method> methodGetter)
            throws ExecutionException, InterruptedException, TimeoutException {
        final MethodParameter thisParameter = methodTest.methodParameters[0];
        final String sutName = thisParameter.name;
        final Class<?> sutClass = thisParameter.clazz;
        final Object sutInstance = thisParameter.value;
        assertAlways(sutName.equals("this"), "Unexpected first variable: " + sutName);
        final Class<?>[] parameterClasses = Arrays.stream(methodTest.methodParameters)
                .skip(1)
                .map(p -> p.clazz)
                .toArray(Class[]::new);
        final Object[] parameterValues = Arrays.stream(methodTest.methodParameters)
                .skip(1)
                .map(p -> p.value)
                .toArray();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<?> future = executor.submit(() -> {
            try {
                final Method method = methodGetter.apply(Tuple3.of(sutClass, methodTest.methodName, parameterClasses));
                final long tid = Thread.currentThread().getId();
                method.setAccessible(true);
                MethodTestExecutor.currentTestId.put(tid, testId);
                method.invoke(sutInstance, parameterValues);
                Objects.requireNonNull(MethodTestExecutor.currentTestId.remove(tid));
                method.setAccessible(false);
            }catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Error invoking method", e);
            }
        });
        // I think 3 seconds is generous enough for a single method call
        future.get(3000, TimeUnit.MILLISECONDS);
        // If the method enters an infinite loop or something, the thread is stuck running forever and Java
        // provides no way to cancel it. Since the executor is created in this scope, this will not block
        // other executions, but the number of running threads will keep increasing until this process terminates.
        // Therefore, it is safer to invoke this class or only one (or a few) method executions, and split
        // large amounts of executions into separate processes to avoid lingering threads and eventual CPU overload.
        executor.shutdownNow();
    }

    public static void executeMethodTest(final File inputsFile)  {
        try {
            final String inputsFilenameWithoutExtension = FileUtils.splitExtension(inputsFile.getName())[0];
            final String testId = Arrays
                .stream(inputsFilenameWithoutExtension.split(Pattern.quote(SEPARATORS[0])))
                .skip(1)
                .collect(Collectors.joining(SEPARATORS[0]));
            final Reader reader = new FileReader(inputsFile);
            try {
                final MethodTest methodTest = (MethodTest) xstream.fromXML(reader);
                try {
                    executeMethodTest(methodTest, testId);
                } catch (ExecutionException e) {
                    System.err.println("ExecutionException executing method test");
                    e.getCause().printStackTrace();
                    System.err.println(inputsFile.getName());
                } catch (TimeoutException | InterruptedException e) {
                    System.err.println("Error executing method test: " + e.getClass().getSimpleName());
                    System.err.println(inputsFile.getName());
                }
            } catch (Exception e) {
                System.err.println("Error deserializing method inputs: " + e.getClass().getSimpleName());
                System.err.println(inputsFile.getName());
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error opening inputs file: " + e.getClass().getSimpleName());
            System.err.println(inputsFile.getName());
        }
    }

    public static void executeMethodTests(final List<File> inputsFiles)  {
        final Map<String, Method> methodsCache = new HashMap<>();
        for (File inputsFile : inputsFiles) {
            try {
                final String inputsFilenameWithoutExtension = FileUtils.splitExtension(inputsFile.getName())[0];
                final String testId = Arrays
                    .stream(inputsFilenameWithoutExtension.split(Pattern.quote(SEPARATORS[0])))
                    .skip(1)
                    .collect(Collectors.joining(SEPARATORS[0]));
                final Reader reader = new FileReader(inputsFile);
                try {
                    final MethodTest methodTest = (MethodTest) xstream.fromXML(reader);
                    try {
                        executeMethodTest(methodTest, testId, args -> {
                            try {
                                final String methodSignature = getMethodSignature(args.a, args.b, args.c);
                                Method method = methodsCache.get(methodSignature);
                                if (method == null) {
                                    method = findMethod(args.a, args.b, args.c);
                                    methodsCache.put(methodSignature, method);
                                }
                                return method;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (ExecutionException e) {
                        System.err.println("ExecutionException executing method test");
                        e.getCause().printStackTrace();
                        System.err.println(inputsFile.getName());
                    } catch (TimeoutException | InterruptedException e) {
                        System.err.println("Error executing method test: " + e.getClass().getSimpleName());
                        System.err.println(inputsFile.getName());
                    }
                } catch (Exception e) {
                    System.err.println("Error deserializing method inputs: " + e.getClass().getSimpleName());
                    System.err.println(inputsFile.getName());
                }
                reader.close();
            } catch (IOException e) {
                System.err.println("Error opening inputs file: " + e.getClass().getSimpleName());
                System.err.println(inputsFile.getName());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Wrong number of parameters: 1 argument expected, got " + args.length);
            System.err.println("Serialized inputs file or directory");
            System.exit(1);
        }
        final String serializedInputsFilename = args[0];
        final File inFile = new File(serializedInputsFilename);
        if (inFile.isDirectory()) {
            System.out.println("Running method tests from " + serializedInputsFilename);
            executeMethodTests(Arrays.asList(Objects.requireNonNull(inFile.listFiles(
                    (file, name) -> name.endsWith(MethodTest.EXTENSION)))));
        } else if (inFile.isFile()) {
            //System.out.println("Running method test " + serializedInputsFilename);
            executeMethodTest(inFile);
        } else {
            throw new RuntimeException("Invalid input: " + inFile.getName());
        }
        System.exit(0); // ExecutorService may prevent the process from terminating if we don't do this
    }

}
