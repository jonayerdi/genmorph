package ch.usi.gassert.data;

import ch.usi.gassert.Config;
import ch.usi.gassert.util.ClassUtils;

import java.util.*;

/**
 * this class represents a Program State
 */
@Deprecated
public class ProgramState {


    private final Map<String, Object> identifier2value;

    public ProgramState() {
        identifier2value = new HashMap<>();
    }

    public ProgramState(Map<String, Object> identifier2value) {
        this.identifier2value = identifier2value;
    }

    /**
     * first element is the name and the second is the value
     *
     * @param pars
     */
    public ProgramState(final Object... pars) throws Exception {
        identifier2value = new HashMap<>();
        final Map<String, Object> var2obj = new HashMap<>();
        for (int i = 0; i < pars.length; i = i + 2) {
            var2obj.put(String.valueOf(pars[i]), pars[i + 1]);
        }
        for (final Map.Entry<String, Object> entry : var2obj.entrySet()) {
            if (entry.getValue() == null) {
                throw new Exception("Null variables not supported");
                // Config.serializer.serialize(this, entry.getKey(), entry.getValue(), var2obj);
                // continue;
            }
            final String typeWrapper = ClassUtils.getTypeWrapper(entry.getValue().getClass().toString());
            if (ClassUtils.isPrimitiveType(typeWrapper)) {
                if (entry.getValue().getClass().toString().equals("class java.lang.String")) {
                    throw new Exception("String variables not supported");
                    // addVariable(entry.getKey() + Config.DELIMITER_METHODS + "isEmpty", Boolean.valueOf(((String) entry.getKey()).isEmpty()), java.lang.Boolean.class.toString());
                    // addVariable(entry.getKey() + Config.DELIMITER_METHODS + "length", Integer.valueOf(((String) entry.getKey()).length()), java.lang.Integer.class.toString());
                } else {
                    addVariable(entry.getKey(), roundIfDouble(entry.getValue()));
                }
            } else { // skip non this parameters **/
                throw new Exception("Non-primitive variables not supported (yet?)");
                //Config.serializer.serialize(this, entry.getKey(), entry.getValue(), var2obj);
            }
        }
    }

    public void addVariable(final String id, final Object value) {
        // this check seems very important
        if (value != null && !String.valueOf(value).isEmpty()) {
            identifier2value.put(id, value);
        }
    }

    public boolean contains(final String id, final Object value) {
        return identifier2value.containsKey(id) && identifier2value.get(id).equals(value);
    }

    public boolean containsAll(final ProgramState other) {
        for (final Map.Entry<String, Object> entry : other.identifier2value.entrySet()) {
            if (!contains(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProgramState)) {
            return false;
        }
        final ProgramState that = (ProgramState) o;
        return Objects.equals(identifier2value, that.identifier2value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier2value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProgramState.class.getSimpleName() + "[", "]")
                .add("identifier2value=" + identifier2value)
                .toString();
    }

    public static Object roundIfDouble(final Object par) {
        if (par.getClass().toString().equals("class java.lang.Double")) {
            if (!Config.IS_ROUNDING_DOUBLE) {
                return par;
            }
            // if 7.0 we dont want to return 7.0000
            if (par.toString().endsWith(".0")) {
                return par;
            }
            return Config.DECIMAL_FORMAT.format(par);
        }
        return par;
    }

    public Map<String, Object> getIdentifier2value() {
        return identifier2value;
    }

    public List<String> getDifferentVariables(final ProgramState other) {
        final List<String> list = new LinkedList<>();
        for (final String identifier : getIdentifier2value().keySet()) {
            // we are assuming the program states are identical in the sense that they have the same identfiers
            if (!other.identifier2value.containsKey(identifier)) {
                continue;
                //TODO check why it fails
//                throw new RuntimeException("something wrong here");
            }
            final Object valueOther = other.identifier2value.get(identifier);
            final Object valueThis = identifier2value.get(identifier);

            if (!valueOther.equals(valueThis)) {
                list.add(identifier);
            }
        }
        return list;
    }

    public int size() {
        return identifier2value.size();
    }
}
