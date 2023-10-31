package ch.usi.gassert.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.usi.gassert.data.types.ErrorValue;
import ch.usi.gassert.data.types.Sequence;

public class ClassUtils {

    public static final Set<Class<?>> primitiveTypesClass = new HashSet<>(Arrays.asList(java.lang.Integer.class, java.lang.Float.class, java.lang.Long.class, java.lang.Double.class, java.lang.Character.class, java.lang.Boolean.class, java.lang.Short.class, java.lang.Byte.class));
    public static final Set<Class<?>> immutableTypesClass = Stream.concat(
        primitiveTypesClass.stream(),
        Arrays.stream(new Class<?>[] { java.lang.String.class })
    ).collect(Collectors.toSet());
    public static final Set<String> primitiveTypesName = primitiveTypesClass.stream().map(Class::getName).collect(Collectors.toSet());
    public static final Set<String> primitiveTypesSimpleName = primitiveTypesClass.stream().map(Class::getSimpleName).collect(Collectors.toSet());

    public static final Set<Class<?>> integerTypes = new HashSet<>(Arrays.asList(Byte.class, Short.class, Integer.class, Long.class));

    public static boolean isPrimitiveType(final String s) {
        return primitiveTypesName.contains(s) || primitiveTypesSimpleName.contains(s);
    }

    public static boolean isPrimitiveType(final Class<?> c) {
        return primitiveTypesClass.contains(c);
    }

    public static String getTypeWithoutGenerics(final String s) {
        final int genericIndex = s.indexOf('<');
        return genericIndex > 0 ? s.substring(0, genericIndex) : s;
    }

    public static String getTypeWrapper(final String s) {
        switch (s) {
            case "int":
                return "class java.lang.Integer";
            case "float":
                return "class java.lang.Float";
            case "long":
                return "class java.lang.Long";
            case "double":
                return "class java.lang.Double";
            case "char":
                return "class java.lang.Character";
            case "boolean":
                return "class java.lang.Boolean";
            case "short":
                return "class java.lang.Short";
            case "byte":
                return "class java.lang.Byte";
            default:
                return s;
        }
    }

    public static Class<?> getTypeWrapper(final Class<?> c) {
        if(c.equals(int.class)) {
            return Integer.class;
        }
        if(c.equals(double.class)) {
            return Double.class;
        }
        if(c.equals(boolean.class)) {
            return Boolean.class;
        }
        if(c.equals(float.class)) {
            return Float.class;
        }
        if(c.equals(char.class)) {
            return Character.class;
        }
        if(c.equals(short.class)) {
            return Short.class;
        }
        if(c.equals(long.class)) {
            return Long.class;
        }
        if(c.equals(byte.class)) {
            return Byte.class;
        }
        return c;
    }

    public static boolean isBooleanOrNumericType(final Class<?> clazz) {
        final Class<?> typeWrapper = getTypeWrapper(clazz);
        return typeWrapper == Boolean.class || Number.class.isAssignableFrom(typeWrapper);
    }

    public static boolean isBooleanType(final Class<?> clazz) {
        return getTypeWrapper(clazz) == Boolean.class;
    }

    public static boolean isNumericIntegerType(final Class<?> clazz) {
        return integerTypes.contains(getTypeWrapper(clazz));
    }

    public static boolean isNumericType(final Class<?> clazz) {
        return Number.class.isAssignableFrom(getTypeWrapper(clazz));
    }

    public static boolean isSequenceType(final Class<?> clazz) {
        return Sequence.class.isAssignableFrom(clazz) || clazz.isArray() || String.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz);
    }

    public static boolean isJavaExceptionType(final Class<?> clazz) {
        return Exception.class.isAssignableFrom(clazz);
    }

    public static boolean isErrorType(final Class<?> clazz) {
        return ErrorValue.class.isAssignableFrom(clazz);
    }

    public static boolean booleanAsBoolean(final Object obj) {
        return (boolean) obj;
    }

    public static Number numericWithClass(Class<?> c, final Number num) {
        c = getTypeWrapper(c);
        if(c.equals(Integer.class)) {
            return num.intValue();
        }
        if(c.equals(Double.class)) {
            return num.doubleValue();
        }
        if(c.equals(Float.class)) {
            return num.floatValue();
        }
        if(c.equals(Short.class)) {
            return num.shortValue();
        }
        if(c.equals(Long.class)) {
            return num.longValue();
        }
        if(c.equals(Byte.class)) {
            return num.byteValue();
        }
        throw new RuntimeException("Unsupported numeric class: " + c.getName());
    }

    public static Number numericAsNumber(final Object obj) {
        return (Number) obj;
    }

    public static Number numericAsPrimitive(final Object obj) {
        return isPrimitiveType(obj.getClass()) ? numericAsNumber(obj) : numericAsNumber(obj).doubleValue();
    }

    public static long numericAsLong(final Object obj) {
        return numericAsNumber(obj).longValue();
    }

    public static double numericAsDouble(final Object obj) {
        return numericAsNumber(obj).doubleValue();
    }

    public static ErrorValue errorAsError(final Object obj) {
        return (ErrorValue) obj;
    }

    public static ErrorValue javaExceptionAsError(final Object obj) {
        return new ErrorValue(obj.getClass().getSimpleName());
    }

    public static Sequence sequenceAsSequence(final Object obj) {
        return Sequence.fromValue(obj);
    }

    public static Optional<Boolean> booleanFromString(final String s, final Class<?> clazz) {
        if (!Boolean.class.equals(clazz)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Boolean.parseBoolean(s));
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    public static Optional<Number> numericRealFromString(final String s, final Class<?> clazz) {
        try {
            if (Double.class.equals(clazz)) {
                return Optional.of(Double.parseDouble(s));
            } else if (Float.class.equals(clazz)) {
                return Optional.of(Float.parseFloat(s));
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    public static Optional<Number> numericIntegerFromString(final String s, final Class<?> clazz) {
        try {
            if (Integer.class.equals(clazz)) {
                return Optional.of(Integer.parseInt(s));
            } else if (Long.class.equals(clazz)) {
                return Optional.of(Long.parseLong(s));
            } else if (Short.class.equals(clazz)) {
                return Optional.of(Short.parseShort(s));
            } else if (Byte.class.equals(clazz)) {
                return Optional.of(Byte.parseByte(s));
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    public static Optional<Number> numericFromString(final String s, final Class<?> clazz) {
        Optional<Number> num = numericRealFromString(s, clazz);
        if (num.isPresent()) {
            return num;
        } else {
            return numericIntegerFromString(s, clazz);
        }
    }

    public static boolean isImmutable(Class<?> clazz) {
        return immutableTypesClass.contains(getTypeWrapper(clazz));
    }

    @SuppressWarnings("unchecked")
    public static <T> T cloneValue(T value) {
        final Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            return (T) Sequence.cloneArray(value);
        } else if (List.class.isAssignableFrom(clazz)) {
            return (T) Sequence.cloneList((List<?>)value);
        }
        return value;
    }

}
