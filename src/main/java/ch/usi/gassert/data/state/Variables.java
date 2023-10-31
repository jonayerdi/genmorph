package ch.usi.gassert.data.state;

import ch.usi.gassert.data.types.ErrorValue;
import ch.usi.gassert.data.types.Sequence;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.OpaqueObject;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Variables {

    private final Set<String> inputs;
    private final Set<String> outputs;
    private final Map<String, Object> variableValues;

    public Variables() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    public Variables(final Map<String, Object> variableValues, final Set<String> inputs, final Set<String> outputs) {
        this.variableValues = variableValues;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public Variables(final Variables other) {
        this.variableValues = new HashMap<>(other.getValues());
        this.inputs = new HashSet<>(other.getInputs());
        this.outputs = new HashSet<>(other.getOutputs());
    }

    public static Variables fromJson(final JsonReader reader) throws IOException {
        final Variables variables = new Variables();
        reader.beginObject();
        while (!reader.peek().equals(JsonToken.END_OBJECT)) {
            switch (reader.nextName()) {
                case "inputs":
                    variables.readJsonVariables(reader, true);
                    break;
                case "outputs":
                    variables.readJsonVariables(reader, false);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return variables;
    }

    public void readJsonVariables(final JsonReader reader, final boolean isInput) throws IOException {
        reader.beginObject();
        while (!reader.peek().equals(JsonToken.END_OBJECT)) {
            final String name = reader.nextName();
            final Object value = readJsonValue(reader);
            this.add(name, value, isInput);
        }
        reader.endObject();
    }

    public static Object readJsonValue(final JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case BOOLEAN:
                return reader.nextBoolean();
            case NUMBER:
                return reader.nextDouble();
            case BEGIN_OBJECT:
                // Empty object representing unserializable variables
                reader.beginObject();
                reader.endObject();
                return OpaqueObject.get();
            case BEGIN_ARRAY:
                return readJsonArrayValue(reader);
            case STRING:
                return readJsonStringValue(reader);
            case NULL:
                reader.nextNull();
                return null;
            default:
                throw new RuntimeException("Unsupported JSON token for value");
        }
    }

    public static Object readJsonArrayValue(final JsonReader reader) throws IOException {
        reader.beginArray();
        final List<Object> array = new ArrayList<>(32);
        // List type encoded as String in first element
        final String type = reader.nextString();
        boolean isArray = false;
        switch (type) {
            case "A":
                isArray = true;
            case "L":
                break;
            default:
                throw new RuntimeException("Unsupported sequence type: " + type);
        }
        // Read list elements
        while (reader.peek() != JsonToken.END_ARRAY) {
            array.add(readJsonValue(reader));
        }
        reader.endArray();
        return isArray 
            ? new Sequence.ArraySequence(array.toArray())
            : new Sequence.ListSequence(array);
    }

    public static Object readJsonStringValue(final JsonReader reader) throws IOException {
        final String value = reader.nextString();
        // Might be a literal string
        if (value.startsWith("$")) {
            return new Sequence.StringSequence(value.substring(1));
        }
        // Might be a number, such as "NaN", "Infinity" and "-Infinity"
        try {
            final double number = Double.parseDouble(value);
            return number;
        } catch (NumberFormatException ignored) {}
        // Might be an ERROR value
        final ErrorValue error = ErrorValue.parse(value);
        if (error != null) {
            return error;
        }
        // No idea what `value` is
        throw new RuntimeException("Invalid serialized value: \"" + value + "\"");
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public Set<String> getNames() {
        return variableValues.keySet();
    }

    public Map<String, Object> getValues() {
        return variableValues;
    }

    public Map<String, Object> getInputValues() {
        return inputs.stream().collect(Collectors.toMap(v -> v, variableValues::get));
    }

    public Map<String, Object> getOutputValues() {
        return outputs.stream().collect(Collectors.toMap(v -> v, variableValues::get));
    }

    public Object get(String name) {
        return variableValues.get(name);
    }

    public Object add(String name, Object value, boolean isInput) {
        if (isInput) {
            this.outputs.remove(name);
            this.inputs.add(name);
        } else {
            this.inputs.remove(name);
            this.outputs.add(name);
        }
        return this.variableValues.put(name, value);
    }

    public void addAll(Map<String, Object> variables, boolean isInput) {
        Set<String> vars = variables.keySet();
        if (isInput) {
            this.outputs.removeAll(vars);
            this.inputs.addAll(vars);
        } else {
            this.inputs.removeAll(vars);
            this.outputs.addAll(vars);
        }
        this.variableValues.putAll(variables);
    }

    public Object addInput(String name, Object value) {
        return this.add(name, value, true);
    }

    public void addInputs(Map<String, Object> variables) {
        this.addAll(variables, true);
    }

    public Object addOutput(String name, Object value) {
        return this.add(name, value, false);
    }

    public void addOutputs(Map<String, Object> variables) {
        this.addAll(variables, false);
    }

    public Object remove(String name, boolean isInput) {
        if (isInput) {
            this.inputs.remove(name);
        } else {
            this.outputs.remove(name);
        }
        return this.variableValues.remove(name);
    }

    public Object removeInput(String name) {
        return this.remove(name, true);
    }

    public Object removeOutput(String name) {
        return this.remove(name, false);
    }

    public void toJson(final JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("inputs").beginObject();
        for (final String var : inputs) {
            addValue(writer.name(var), variableValues.get(var));
        }
        writer.endObject(); // inputs
        writer.name("outputs").beginObject();
        for (final String var : outputs) {
            addValue(writer.name(var), variableValues.get(var));
        }
        writer.endObject(); // outputs
        writer.endObject();
    }

    private void addValue(final JsonWriter writer, final Object value) throws IOException {
        if (value != null) {
            final Class<?> clazz = value.getClass();
            if (ClassUtils.isBooleanType(clazz)) {
                final boolean bool = ClassUtils.booleanAsBoolean(value);
                writer.value(bool);
            } else if (ClassUtils.isNumericType(clazz)) {
                final double num = ClassUtils.numericAsDouble(value);
                if (Double.isFinite(num)) {
                    writer.value(num);
                } else {
                    // "NaN", "Infinity" and "-Infinity" must be serialized as strings
                    writer.value(Double.toString(num));
                }
            } else if (ClassUtils.isSequenceType(clazz)) {
                final Sequence sequence = ClassUtils.sequenceAsSequence(value);
                final String type = sequence.getType();
                String typePrefix = "L";
                switch (type) {
                    case "string":
                        writer.value("$" + sequence.getValue());
                        break;
                    case "array":
                        typePrefix = "A";
                    case "list":
                        writer.beginArray();
                        // Encode list type in first element
                        writer.value(typePrefix);
                        // Write list elements
                        sequence.items().forEach(elem -> {
                            try {
                                this.addValue(writer, elem);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        writer.endArray();
                        break;
                    default:
                        throw new RuntimeException("Unsupported sequence type: " + type);
                }
            } else if (ClassUtils.isErrorType(clazz)) {
                final ErrorValue error = ClassUtils.errorAsError(value);
                writer.value(error.toString());
            } else {
                // Write empty object for unserializable variables
                writer.beginObject().endObject();
                //throw new RuntimeException("Cannot serialize class: " + clazz.getSimpleName());
            }
        } else {
            writer.nullValue();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Variables)) return false;
        final Variables that = (Variables) o;
        return Objects.equals(getInputs(), that.getInputs()) &&
                Objects.equals(getOutputs(), that.getOutputs()) &&
                Objects.equals(getValues(), that.getValues());
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, outputs, variableValues);
    }

}
