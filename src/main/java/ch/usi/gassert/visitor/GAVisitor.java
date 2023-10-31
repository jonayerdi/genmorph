package ch.usi.gassert.visitor;


import ch.usi.gassert.data.ProgramState;
import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.util.MyGson;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.*;
// is thread safe

@Deprecated
public class GAVisitor {

    private static GAVisitor instance;
    private String idVersion;
    private static boolean serEnabled;
    private Map<Long, ProgramState> threadIs2enterMethodState = new Hashtable<>();

    private GAVisitor() {
        super();
    }

    public static GAVisitor getInstance() {
        if (instance == null) {
            instance = new GAVisitor();
        }
        return instance;
    }

    private void checkProperties() {
        serEnabled = Boolean.parseBoolean(System.getProperty("serialization", "true"));
        if (System.getenv("mode") == null) {
            idVersion = "idVersionNull";
        } else if (System.getenv("mode").equals("genTests") || System.getenv("mode").equals("gassert")) {
            idVersion = System.getenv("version_src_gassert");
        } else if (System.getenv("mode").equals("oasis")) {
            idVersion = System.getProperty("version_src_gassert");
        } else {
            idVersion = "idVersionNull";
        }
    }

    /*public void enterMethod(Object... pars) {
        enterMethod(false, pars);
    }*/
    public void enterMethod(final Object... pars) {
        checkProperties();
        System.out.println("GAVisitor:" + serEnabled);
        if (!serEnabled || isRecursiveCall()) {
            return;
        }
        invokeWithTimeOut(new EnterMethodTask(Thread.currentThread().getId(), Thread.currentThread().getStackTrace(),
                pars));

    }

    /*public void assertionPoint(Object... pars) {
    	assertionPoint(false, pars);
    }*/


    public void assertionPoint(final Object... pars) {
        checkProperties();
        System.out.println("assertion point GAVisitor:" + serEnabled);
        if (!serEnabled || isRecursiveCall()) {
            return;
        }
        System.out.println("invoking assertions");

        invokeWithTimeOut(new AssertionPointTask(Thread.currentThread().getId(), Thread.currentThread().getStackTrace(), pars));
    }

    private boolean isRecursiveCall() {
        for (final StackTraceElement el : Thread.currentThread().getStackTrace()) {
            if (el.toString().contains("ch.usi.gassert.data.ser.HybridSerialization.addObserverState(HybridSerialization")) {
                return true;
            }
        }
        return false;
    }


    private void invokeWithTimeOut(final Callable<Void> callable) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Void> future;
        future = executor.submit(callable);
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (final RuntimeException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            future.cancel(true);
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        } catch (final TimeoutException e) {
            future.cancel(true);
            e.printStackTrace();
        }
        executor.shutdownNow();
    }


    /**
     * this class takes the testID
     * <p>
     * which is the calling context point in the test
     * <p>
     * ch.usi.examples.SimpleMethods_ESTest.test01(SimpleMethods_ESTest.java:27)
     *
     * @return
     */
    private static String computeTestId(final StackTraceElement[] stackTrace) {
        if (System.getProperty("testId") != null) {
            if (System.getenv("iter") != null) {
                return "iter" + System.getenv("iter") + "-" + System.getProperty("testId");
            }
            return System.getProperty("testId");
        }
        for (final StackTraceElement frame : stackTrace) {
            if (frame.getMethodName().startsWith("test")) {
                return frame.toString();
            }
        }
        // defensive programming
        return "test-error" + System.currentTimeMillis();
    }


    public static void setSerEnabled(final boolean value) {
        serEnabled = value;
    }

    class AssertionPointTask implements Callable<Void> {
        private Object[] pars;
        private Long threadId;
        private StackTraceElement[] stackTrace;

        public AssertionPointTask(final Long threadId, final StackTraceElement[] stackTrace, final Object... pars) {
            this.pars = pars;
            this.threadId = threadId;
            this.stackTrace = stackTrace;
        }

        @Override
        public Void call() throws Exception {
            //if (!threadIs2enterMethodState.containsKey(threadId)) {
            // in case methodEnter has failed
            //return null;
            // }
            final ProgramState p = new ProgramState(pars);
            final TestExecution exec = new TestExecution(idVersion, computeTestId(stackTrace), null);
            System.out.println("attemptng to write file");

            MyGson.storeTestExecFile(exec);
            return null;
        }
    }

    class EnterMethodTask implements Callable<Void> {
        private Object[] pars;
        private Long threadId;
        private StackTraceElement[] stackTrace;
        ;

        public EnterMethodTask(final Long threadId, final StackTraceElement[] stackTrace, final Object... pars) {
            this.pars = pars;
            this.threadId = threadId;
            this.stackTrace = stackTrace;

        }

        @Override
        public Void call() throws Exception {
            threadIs2enterMethodState.put(threadId, new ProgramState(pars));
            return null;
        }
    }
}

