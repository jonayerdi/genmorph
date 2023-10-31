package ch.usi.gassert.util;

/*
 * Class with no information representing some Object that could not be (de)serialized
 */
public final class OpaqueObject {
    private static OpaqueObject INSTANCE = new OpaqueObject();
    private OpaqueObject() {}
    public static OpaqueObject get() {
        return INSTANCE;
    }
}
