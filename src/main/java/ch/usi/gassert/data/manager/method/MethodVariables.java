package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.manager.DataManagerFactory;
import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.Utils;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that describes the type of a GAssert Tree node.
 * From a set of test executions, it infers the type (BOOLEAN, NUMBER) and whether the variable can be NULL or ERROR.
 *
 * TODO: GAssert does not currently use this, but maybe it should
 */
public class MethodVariables {

    public static class Type {

        public Tree.Type type;
        public boolean isNullable;
        public boolean isFallible;

        public Type() {
            this.type = null;
            this.isNullable = false;
            this.isFallible = false;
        }

        public Type(Tree.Type type, boolean isNullable, boolean isFallible) {
            this.type = type;
            this.isNullable = isNullable;
            this.isFallible = isFallible;
        }

        public void setType(Tree.Type type) {
            if (this.type == null) {
                this.type = type;
            } else if (this.type != type) {
                throw new RuntimeException("Conflicting types. Old: " + this.type + ", New: " + type);
            }
        }

        @Override
        public String toString() {
            String typeName = type != null ? type.toString() : "NONE";
            if (isNullable) {
                typeName += "?";
            }
            if (isFallible) {
                typeName += "!";
            }
            return typeName;
        }

        public static Type fromString(String str) {
            Type type = new Type();
            if (str.charAt(str.length() - 1) == '!') {
                type.isFallible = true;
                str = str.substring(0, str.length() - 1);
            }
            if (str.charAt(str.length() - 1) == '?') {
                type.isNullable = true;
                str = str.substring(0, str.length() - 1);
            }
            if (!str.equals("NONE")) {
                type.type = Tree.Type.fromString(str);
            }
            return type;
        }

    }

    public static Map<String, Type> getTypesFromValues(final Map<String, Object> variableValues) {
        final Map<String, Type> types = new HashMap<>();
        for (String name : variableValues.keySet()) {
            try {
                final Object value = variableValues.get(name);
                final Type type = new Type();
                if (value == null) {
                    type.isNullable = true;
                } else {
                    final Class<?> clazz = value.getClass();
                    if (ClassUtils.isErrorType(clazz)) {
                        type.isFallible = true;
                    } else if (ClassUtils.isBooleanType(clazz)) {
                        type.type = Tree.Type.BOOLEAN;
                    } else if (ClassUtils.isNumericType(clazz)) {
                        type.type = Tree.Type.NUMBER;
                    } else {
                        throw new RuntimeException("Unsupported Object for GAssert: " + value);
                    }
                }
                types.put(name, type);
            } catch (Exception e) {
                throw new RuntimeException("Error handling variable: " + name, e);
            }
        }
        return types;
    }

    public static Map<String, Type> getTypesFromValues(final Iterator<Map<String, Object>> testExecutionVariables) {
        if (testExecutionVariables.hasNext()) {
            final Map<String, Type> types = getTypesFromValues(testExecutionVariables.next());
            while (testExecutionVariables.hasNext()) {
                final Map<String, Object> variableValues = testExecutionVariables.next();
                for (String name : Utils.union(types.keySet(), variableValues.keySet())) {
                    try {
                        types.putIfAbsent(name, new Type(null, true, false));
                        final Type type = types.get(name);
                        final Object value = variableValues.get(name);
                        if (value == null) {
                            type.isNullable = true;
                        } else {
                            final Class<?> clazz = value.getClass();
                            if (ClassUtils.isErrorType(clazz)) {
                                type.isFallible = true;
                            } else if (ClassUtils.isBooleanType(clazz)) {
                                type.setType(Tree.Type.BOOLEAN);
                            } else if (ClassUtils.isNumericType(clazz)) {
                                type.setType(Tree.Type.NUMBER);
                            } else {
                                throw new RuntimeException("Unsupported Object for GAssert: " + value);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error handling variable: " + name, e);
                    }
                }
            }
            return types;
        } else {
            return new HashMap<>(0); // No test executions = no variables
        }
    }

    public static Map<String, Type> variableTypesFromJson(final JsonReader reader) throws IOException {
        final Map<String, Type> types = new HashMap<>();
        reader.beginObject();
        while (!reader.peek().equals(JsonToken.END_OBJECT)) {
            final String name = reader.nextName();
            final String typeStr = reader.nextString();
            types.put(name, Type.fromString(typeStr));
        }
        reader.endObject();
        return types;
    }

    public static void variableTypesToJson(final JsonWriter writer, final Map<String, Type> types) throws IOException {
        writer.beginObject();
        for (String name : types.keySet()) {
            writer.name(name).value(types.get(name).toString());
        }
        writer.endObject();
    }

    public static void main(String[] args) {
        // Parse args
        if (args.length != 2) {
            System.err.println("Wrong number of parameters: 2 arguments expected, got " + args.length);
            System.err.println("Manager class");
            System.err.println("Manager args");
            System.exit(-1);
        }
        final String managerClass = args[0];
        final DataManagerArgs managerArgs = new DataManagerArgs(args[1].split(";"));

        // Load DataManager
        final IDataManager dataManager = DataManagerFactory.load(managerClass, managerArgs);

        // Get variable types
        final Map<String, Type> types = getTypesFromValues(
                dataManager.getCorrectTestExecutions()
                        .stream()
                        .map(t -> t.getVariables().getValues())
                        .iterator()
        );

        // Print variable types
        for (String name : types.keySet()) {
            System.out.println(name + ": " + types.get(name));
        }
    }

}
