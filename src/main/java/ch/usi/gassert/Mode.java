package ch.usi.gassert;

public enum Mode {
    REGULAR_ASSERTIONS, METAMORPHIC_OUTPUT_RELATION, METAMORPHIC_INPUT_RELATION_RELAX, METAMORPHIC_FULL;

    public static Mode fromString(final String name) {
        switch (name.toUpperCase()) {
            case "REGULAR_ASSERTIONS": return REGULAR_ASSERTIONS;
            case "METAMORPHIC_OUTPUT_RELATION": return METAMORPHIC_OUTPUT_RELATION;
            case "METAMORPHIC_INPUT_RELATION_RELAX": return METAMORPHIC_INPUT_RELATION_RELAX;
            case "METAMORPHIC_FULL": return METAMORPHIC_FULL;
            default: throw new RuntimeException("Unknown mode name: " + name);
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case REGULAR_ASSERTIONS: return "REGULAR_ASSERTIONS";
            case METAMORPHIC_OUTPUT_RELATION: return "METAMORPHIC_OUTPUT_RELATION";
            case METAMORPHIC_INPUT_RELATION_RELAX: return "METAMORPHIC_INPUT_RELATION_RELAX";
            case METAMORPHIC_FULL: return "METAMORPHIC_FULL";
            default: throw new RuntimeException("Unknown mode");
        }
    }

}
