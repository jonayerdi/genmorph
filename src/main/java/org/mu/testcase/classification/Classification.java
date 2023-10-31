package org.mu.testcase.classification;

public enum Classification {

    NONE, CORRECT, INCORRECT;

    public char toChar() {
        switch (this) {
            case NONE:
                return '?';
            case CORRECT:
                return 'O';
            case INCORRECT:
                return 'X';
            default:
                throw new RuntimeException("Invalid classification value");
        }
    }

    public static Classification fromChar(char c) {
        switch (c) {
            case '?':
                return NONE;
            case 'O':
                return CORRECT;
            case 'X':
                return INCORRECT;
            default:
                throw new RuntimeException("Invalid classification value");
        }
    }

    @Override
    public String toString() {
        return String.valueOf(this.toChar());
    }

    public static Classification fromString(String s) {
        if (s.length() == 1) {
            return Classification.fromChar(s.charAt(0));
        }
        throw new RuntimeException("Invalid classification value");
    }

    public static Classification metamorphic(final Classification source, final Classification followup) {
        if (source == CORRECT && followup == CORRECT) {
            return CORRECT;
        } else if(source == CORRECT || followup == CORRECT) {
            return null; // This should never happen
        } else if(source == INCORRECT || followup == INCORRECT) {
            return INCORRECT;
        } else {
            return NONE;
        }
    }

}
