package ch.usi.gassert.data.types;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class Sequence {

    public static final double NUMBER_PRECISION = 0.0001;

    public abstract String getType();
    public abstract Object getValue();
    public abstract int length();
    public abstract Object item(int index);
    public abstract Stream<?> items();
    public abstract Sequence flip();
    public abstract Sequence remove(int index);
    public abstract Sequence truncate(int index);

    public static Object newArrayWithElements(final Class<?> itemsClass, final int length, Iterator<?> elements) {
        final Object newArray = Array.newInstance(itemsClass, length);
        for (int i = 0 ; i < length ; ++i) {
            Array.set(newArray, i, elements.next());
        }
        return newArray;
    }

    public static Object newArrayWithElements(final Class<?> itemsClass, final int length, Stream<?> elements) {
        return newArrayWithElements(itemsClass, length, elements.iterator());
    }

    public static Class<?> arrayItemsClass(final Class<?> arrayClass) {
        if (int[].class.equals(arrayClass)) {
            return int.class;
        }
        if (double[].class.equals(arrayClass)) {
            return double.class;
        }
        if (boolean[].class.equals(arrayClass)) {
            return boolean.class;
        }
        if (float[].class.equals(arrayClass)) {
            return float.class;
        }
        if (char[].class.equals(arrayClass)) {
            return char.class;
        }
        if (short[].class.equals(arrayClass)) {
            return short.class;
        }
        if (long[].class.equals(arrayClass)) {
            return long.class;
        }
        if (byte[].class.equals(arrayClass)) {
            return byte.class;
        }
        try {
            // [Ljava.lang.Object;
            final String arrayClassName = arrayClass.getName();
            return Class.forName(arrayClassName.substring(2, arrayClassName.length() - 1));
        } catch (Exception e) {
            throw new RuntimeException("Error inferring array element class for: " + arrayClass.getName(), e);
        }
    }

    public static Class<?> arrayItemsClass(final Object array) {
        return arrayItemsClass(array.getClass());
    }

    public static final Object cloneArray(Object value) {
        final int length = Array.getLength(value);
        return newArrayWithElements(
            arrayItemsClass(value),
            length,
            IntStream.range(0, length).mapToObj(i -> Array.get(value, i))
        );
    }

    public static List<?> cloneList(final List<?> list) {
        if (list == null ) {
            return null;
        }
        try {
            return (List<?>)list.getClass().getMethod("clone").invoke(list);
        } catch (Exception ignore) {}
        try {
            return list.getClass().getConstructor(Collection.class).newInstance(list);
        } catch (Exception ignore) {}
        throw new RuntimeException("Uncloneable list class: " + list.getClass().getName());
    }

    public double sum() {
        if (this.length() == 0) {
            return 0.0;
        }
        final Class<?> clazz = this.item(0).getClass();
        final ToDoubleFunction<Object> mapFn;
        if (Boolean.class.isAssignableFrom(clazz)) {
            mapFn = x -> ((Boolean)x) ? 1.0 : 0.0;
        } else if (Number.class.isAssignableFrom(clazz)) {
            mapFn = x -> ((Number)x).doubleValue();
        } else if (Sequence.class.isAssignableFrom(clazz)) {
            mapFn = x -> ((Sequence)x).sum();
        } else {
            return 0.0;
        }
        return this.items().mapToDouble(mapFn).sum();
    }

    public boolean equals(Sequence other, double precision) {
        final Object value = getValue();
        if (value == null || other == null) {
            return value == other;
        }
        if (this.length() != other.length()) {
            return false;
        }
        if (this.length() == 0) {
            return true;
        }
        if (Number.class.isAssignableFrom(this.item(0).getClass())) {
            for (int i = 0 ; i < this.length() ; ++i) {
                if(Math.abs(((Number)this.item(i)).doubleValue() - ((Number)other.item(i)).doubleValue()) >= precision) {
                    return false;
                }
            }
        } else {
            for (int i = 0 ; i < this.length() ; ++i) {
                if(!this.item(i).equals(other.item(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        final Object value = getValue();
        if (value == null || obj == null) {
            return value == obj;
        } else if (Sequence.class.isAssignableFrom(obj.getClass())) {
            return this.equals((Sequence)obj, NUMBER_PRECISION);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    public static Sequence fromValue(final Object value) {
        final Class<?> clazz = value.getClass();
        if (Sequence.class.isAssignableFrom(clazz)) {
            return (Sequence)value;
        } else if (clazz.isArray()) {
            return new ArraySequence(cloneArray(value));
        } else if (String.class.isAssignableFrom(clazz)) {
            return new StringSequence((String)value);
        } else if (List.class.isAssignableFrom(clazz)) {
            return new ListSequence(cloneList((List<?>)value));
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public static Sequence fromNumber(final Number number) {
        return new StringSequence(Integer.toString(number.intValue()));
    }

    public static final class ArraySequence extends Sequence {
        public final Object value;
        public ArraySequence(Object value) {
            this.value = value;
        }
        public String getType() {
            return "array";
        }
        public Object getValue() {
            return value;
        }
        public int length() {
            return Array.getLength(value);
        }
        public Object item(int index) {
            return Array.get(value, index);
        }
        public Stream<?> items() {
            return IntStream.range(0, Array.getLength(value)).mapToObj(i -> Array.get(value, i));
        }
        public Sequence flip() {
            final int length = Array.getLength(value);
            if (length < 1) {
                return this;
            }
            return new ArraySequence(newArrayWithElements(
                arrayItemsClass(value),
                length,
                IntStream.range(0, length)
                    .mapToObj(i -> Array.get(value, length - 1 - i))
            ));
        }
        public Sequence remove(int index) {
            final int length = Array.getLength(value);
            if (index < length) {
                if (index >= 0) {
                    return new ArraySequence(newArrayWithElements(
                        arrayItemsClass(value),
                        length - 1,
                        IntStream.range(0, length)
                            .filter(i -> i != index)
                            .mapToObj(i -> Array.get(value, i))
                    ));
                } else if(-index <= length) {
                    return new ArraySequence(newArrayWithElements(
                        arrayItemsClass(value),
                        length - 1,
                        IntStream.range(0, length)
                            .filter(i -> i != (length + index))
                            .mapToObj(i -> Array.get(value, i))
                    ));
                }
            }
            return this;
        }
        public Sequence truncate(int index) {
            final int length = Array.getLength(value);
            final int start = index < 0 ? Integer.min(-index, length) : 0;
            final int end = index >= 0 ? Integer.min(index, length) : length;
            if (start != 0 || end < length) {
                return new ArraySequence(newArrayWithElements(
                    arrayItemsClass(value),
                    end - start,
                    IntStream.range(start, end)
                        .mapToObj(i -> Array.get(value, i))
                ));
            }
            return this;
        }
    }

    public static final class ListSequence extends Sequence {
        public final List<?> value;
        public ListSequence(List<?> value) {
            this.value = value;
        }
        public String getType() {
            return "list";
        }
        public Object getValue() {
            return value;
        }
        public int length() {
            return value.size();
        }
        public Object item(int index) {
            return value.get(index);
        }
        public Stream<?> items() {
            return value.stream();
        }
        public Sequence flip() {
            final List<?> newValue = cloneList(value);
            Collections.reverse(newValue);
            return new ListSequence(newValue);
        }
        public Sequence remove(int index) {
            final List<?> newValue = cloneList(value);
            if (index < newValue.size()) {
                if (index >= 0) {
                    newValue.remove(index);
                } else if(-index <= newValue.size()) {
                    newValue.remove(newValue.size() + index);
                }
            }
            return new ListSequence(newValue);
        }
        public Sequence truncate(int index) {
            final int length = value.size();
            final int start = index < 0 ? Integer.min(-index, length) : 0;
            final int end = index >= 0 ? Integer.min(index, length) : length;
            if (end < length) {
                final List<?> newValue = cloneList(value);
                for (int i = length - 1 ; i >= end ; --i) {
                    newValue.remove(i);
                }
                return new ListSequence(newValue);
            }
            if (start != 0) {
                final List<?> newValue = cloneList(value);
                for (int i = start - 1 ; i >= 0 ; --i) {
                    newValue.remove(i);
                }
                return new ListSequence(newValue);
            }
            return this;
        }
    }

    public static final class StringSequence extends Sequence {
        public final String value;
        public StringSequence(String value) {
            this.value = value;
        }
        public String getType() {
            return "string";
        }
        public Object getValue() {
            return value;
        }
        public int length() {
            return value.length();
        }
        public Object item(int index) {
            return (int)value.charAt(index);
        }
        public Stream<?> items() {
            return value.chars().mapToObj(c -> c);
        }
        public Sequence flip() {
            StringBuffer sb = new StringBuffer();
            for (int i = value.length() - 1 ; i >= 0 ; --i) {
                sb.append(value.charAt(i));
            }
            return new StringSequence(sb.toString());
        }
        public Sequence remove(int index) {
            if (index < value.length()) {
                if (index >= 0) {
                    return new StringSequence(value.substring(0, index) + value.substring(index + 1));
                } else if(-index <= value.length()) {
                    index = value.length() + index;
                    return new StringSequence(value.substring(0, index) + value.substring(index + 1));
                }
            }
            return this;
        }
        public Sequence truncate(int index) {
            final int length = value.length();
            final int start = index < 0 ? Integer.min(-index, length) : 0;
            final int end = index >= 0 ? Integer.min(index, length) : length;
            if (start != 0 || end < length) {
                return new StringSequence(value.substring(start, end));
            }
            return this;
        }
    }
    
}
