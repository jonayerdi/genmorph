package ch.usi.gassert;

public enum Tool {
    GASSERT, NAIVE_SEARCH_BASED, RANDOM;

    public static Tool fromString(final String name) {
        switch (name.toUpperCase()) {
            case "GASSERT": return GASSERT;
            case "NAIVE": return NAIVE_SEARCH_BASED;
            case "RANDOM": return RANDOM;
            default: throw new RuntimeException("Unknown tool name: " + name);
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case GASSERT: return "GASSERT";
            case NAIVE_SEARCH_BASED: return "NAIVE";
            case RANDOM: return "RANDOM";
            default: throw new RuntimeException("Unknown tool");
        }
    }

}
