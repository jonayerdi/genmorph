package ch.usi.gassert.serialization;

import java.util.Map;
import java.util.stream.Stream;

public interface ISerializer {

    void serialize(final Class<?> clazz, final Object obj, final Map<String, Object> vars,
                   boolean serializeImmutable);
    default void serialize(final Object obj, final Map<String, Object> vars, boolean serializeImmutable) {
        serialize(obj.getClass(), obj, vars, serializeImmutable);
    }

    boolean shouldIgnore(final Class<?> clazz);
    boolean isTerminal(final Class<?> clazz);
    void serializeTerminal(final Class<?> clazz, final Object obj, final String path, final Map<String, Object> vars);
    Stream<FieldInfo> getFields(final Class<?> clazz, final Object obj);
    default Stream<FieldInfo> getFields(final Class<?> clazz) {
        return getFields(clazz, null);
    }

}
