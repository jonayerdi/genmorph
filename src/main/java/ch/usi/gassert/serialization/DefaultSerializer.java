package ch.usi.gassert.serialization;

import ch.usi.gassert.util.ClassUtils;
import ch.usi.staticanalysis.PurityAnalysis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Stream;

public class DefaultSerializer implements ISerializer {

    public static final DefaultSerializer INSTANCE = new DefaultSerializer();
    public static final int DEFAULT_MAX_DEPTH = 2;
    public static final PurityAnalysis purityAnalysis = null;
    //public static final PurityAnalysis purityAnalysis = new PurityAnalysis(null);

    public static DefaultSerializer getInstance() {
        return INSTANCE;
    }

    public final int maxDepth;

    private DefaultSerializer() {
        this(DEFAULT_MAX_DEPTH);
    }

    private DefaultSerializer(final int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public void serialize(final Class<?> clazz, final Object obj, final Map<String, Object> vars,
                          boolean serializeImmutable) {
        serialize(clazz, obj, vars, serializeImmutable, 0, "");
    }

    private void serialize(final Class<?> clazz, final Object obj, final Map<String, Object> vars,
                           boolean serializeImmutable, int depth, String path) {
        if (this.shouldIgnore(clazz)) {
            return;
        }
        final boolean isImmutable = ClassUtils.isImmutable(clazz);
        if (isImmutable && !serializeImmutable) {
            return;
        }
        if (this.isTerminal(clazz)) {
            this.serializeTerminal(clazz, obj, path, vars);
        } else if (depth < maxDepth) {
            this.getFields(clazz, obj).forEach(field -> {
                try {
                    serialize(field.value.getClass(), field.value, vars, serializeImmutable || !isImmutable, depth + 1, path + field.name + ".");
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    public boolean shouldIgnore(final Class<?> clazz) {
        return Character.class.equals(clazz);
    }

    @Override
    public boolean isTerminal(final Class<?> clazz) {
        return ClassUtils.isBooleanType(clazz) || ClassUtils.isNumericType(clazz) || ClassUtils.isSequenceType(clazz);
    }

    @Override
    public void serializeTerminal(final Class<?> clazz, final Object obj, final String path, final Map<String, Object> vars) {
        final String name = path.isEmpty() ? path : path.substring(0, path.length() - 1);
        if (ClassUtils.isBooleanType(clazz)) {
            vars.put(name, obj != null ? ClassUtils.booleanAsBoolean(obj) : null);
        } else if (ClassUtils.isNumericType(clazz)) {
            vars.put(name, obj != null ? ClassUtils.numericAsPrimitive(obj) : null);
        } else if (ClassUtils.isSequenceType(clazz)) {
            vars.put(name, obj != null ? ClassUtils.sequenceAsSequence(obj) : null);
        } else {
            throw new RuntimeException("Unsupported object type for serialization: " + clazz.getName());
        }
    }

    @Override
    public Stream<FieldInfo> getFields(Class<?> clazz, final Object obj) {
        final Stream.Builder<FieldInfo> stream = Stream.builder();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    final int mod = field.getModifiers();
                    if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
                        field.setAccessible(true);
                        // field.get(obj) will automatically wrap primitives into Objects
                        stream.accept(new FieldInfo(field.getName(), field.get(obj), Modifier.isFinal(mod)));
                        field.setAccessible(false);
                    }
                } catch (Exception ignored) {}
            }
            if (purityAnalysis != null) {
                for (String methodName : purityAnalysis.getPureMethods(clazz.getName())) {
                try {
                    final Method method = clazz.getDeclaredMethod(methodName);
                    final int mod = method.getModifiers();
                    if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)
                            && method.getParameters().length == 0 
                            && this.isTerminal(method.getReturnType())) {
                        method.setAccessible(true);
                        stream.accept(new FieldInfo(method.getName() + "()", method.invoke(obj), true));
                        method.setAccessible(false);
                    }
                } catch (Exception ignored) {}
            }
            }
            clazz = clazz.getSuperclass();
        } while(clazz != null);
        return stream.build();
    }

}
