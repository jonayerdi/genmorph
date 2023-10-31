package org.mu.testcase.evaluation;

public enum TestResult {

    NONE, TP, TN, FP, FN;

    public char toChar() {
        switch (this) {
            case NONE:
                return '-';
            case TP:
                return 'X';
            case TN:
                return 'O';
            case FP:
                return '!';
            case FN:
                return '?';
            default:
                throw new RuntimeException("Invalid result value");
        }
    }

    public static TestResult fromChar(char c) {
        switch (c) {
            case '-':
                return NONE;
            case 'X':
                return TP;
            case 'O':
                return TN;
            case '!':
                return FP;
            case '?':
                return FN;
            default:
                throw new RuntimeException("Invalid result value");
        }
    }

    @Override
    public String toString() {
        return String.valueOf(this.toChar());
    }

    public static TestResult fromString(String s) {
        if (s.length() == 1) {
            return TestResult.fromChar(s.charAt(0));
        }
        throw new RuntimeException("Invalid result value");
    }

}
