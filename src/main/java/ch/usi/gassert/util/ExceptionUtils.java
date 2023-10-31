package ch.usi.gassert.util;

import java.util.concurrent.*;

public class ExceptionUtils {


    public static void main(final String[] args) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Void> future;
        future = executor.submit(new TestCallable());
        final boolean isexecudet = false;
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (final RuntimeException e) {
            throw e;
            // e.printStackTrace();
        } catch (final InterruptedException e) {
            future.cancel(true);
        } catch (final ExecutionException e) {
            future.cancel(true);
            final StringBuilder sb = new StringBuilder();
            for (final StackTraceElement el : e.getCause().getStackTrace()) {
                sb.append(el.getClassName() + ":" + el.getMethodName() + ":" + el.getLineNumber() + "-------\n");
            }
            throw new RuntimeException(e.getCause() + " " + sb.toString());
        } catch (
                final TimeoutException e) {
            future.cancel(true);
        } finally {
            executor.shutdownNow();

        }

    }
}

class TestCallable implements Callable<Void> {

    @Override
    public Void call() throws Exception {

        m1();
        return null;
    }

    public void m1() {
        final String n = null;
        n.contains("d");
    }
}
