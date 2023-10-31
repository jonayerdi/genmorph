package ch.usi.gassert.util;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class Memory {

    public static void printMemory() {
        final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        System.out.println("Memory heap used:          " + String.valueOf(bytesToMeg(memBean.getHeapMemoryUsage().getUsed())));
    }

    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMeg(final long bytes) {
        return bytes / MEGABYTE;
    }

}
