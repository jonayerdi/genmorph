package org.mu.testcase;

public final class Util {

    public static String joinSourceFollowup(String source, String followup) {
        return source + ":" + followup;
    }
    public static String[] splitSourceFollowup(String row) {
        return row.split(":");
    }
    public static String joinMetricOperator(String metric, String operator) {
        return metric + "_" + operator;
    }
    public static String[] splitMetricOperator(String column) {
        return column.split("_");
    }

}
