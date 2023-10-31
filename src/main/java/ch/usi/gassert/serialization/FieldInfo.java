package ch.usi.gassert.serialization;

public class FieldInfo {

    public final String name;
    public final Object value;
    public final boolean isFinal;

    public FieldInfo(final String name, final Object value, final boolean isFinal) {
        this.name = name;
        this.value = value;
        this.isFinal = isFinal;
    }

}
