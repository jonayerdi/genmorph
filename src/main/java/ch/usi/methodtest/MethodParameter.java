package ch.usi.methodtest;

import ch.usi.gassert.util.ClassUtils;

import java.util.Objects;

public class MethodParameter {

    public String name;
    public Class<?> clazz;
    public Object value;

    public MethodParameter(final String name, final Class<?> clazz, final Object value) {
        this.name = name;
        this.clazz = clazz;
        this.value = value;
    }

    /**
     * This function determines whether this MethodParameter can be changed for the follow-up test case.
     *
     * @return true if the parameter can change in the followup, false otherwise.
     */
    public boolean canChangeInFollowup() {
        return canChangeInFollowup(this.clazz);
    }

    /**
     * This function determines whether a MethodParameter with the given class
     * can be changed for the follow-up test case.
     *
     * @return true if a parameter of the given class can change in the followup, false otherwise.
     */
    public static boolean canChangeInFollowup(final Class<?> clazz) {
        return ClassUtils.isBooleanOrNumericType(clazz);
    }

    @Override
    public String toString() {
        return "(" + clazz.getSimpleName() + ") " + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodParameter that = (MethodParameter) o;
        return Objects.equals(name, that.name) && Objects.equals(clazz, that.clazz) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clazz, value);
    }
}
