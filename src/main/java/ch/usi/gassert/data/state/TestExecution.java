package ch.usi.gassert.data.state;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;

/**
 * This is a test execution
 */
public class TestExecution implements ITestExecution {

    private final String systemId;
    private final String testId;
    private final Variables variables;

    public TestExecution(final String systemId, final String testId, final Variables variables) {
        this.systemId = systemId;
        this.testId = testId;
        this.variables = variables;
    }

    public TestExecution(final TestExecution other) {
        this(other.getSystemId(), other.getTestId(), new Variables(other.getVariables()));
    }

    public static TestExecution fromJson(final JsonReader reader) throws IOException {
        String systemId = null;
        String testId = null;
        Variables variables = null;
        reader.beginObject();
        while (!reader.peek().equals(JsonToken.END_OBJECT)) {
            switch (reader.nextName()) {
                case "systemId":
                    systemId = reader.nextString();
                    break;
                case "testId":
                    testId = reader.nextString();
                    break;
                case "variables":
                    variables = Variables.fromJson(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (systemId == null) {
            throw new RuntimeException("TestExecution JSON missing systemId");
        } else if (testId == null) {
            throw new RuntimeException("TestExecution JSON missing testId");
        } else if (variables == null) {
            throw new RuntimeException("TestExecution JSON missing variables");
        }
        return new TestExecution(systemId, testId, variables);
    }

    public String getSystemId() {
        return systemId;
    }

    public String getTestId() {
        return testId;
    }

    public Variables getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "TestExecution { versionId: \"" + systemId + "\", testId: \"" + testId + "\" }";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TestExecution)) return false;
        final TestExecution that = (TestExecution) o;
        return Objects.equals(getSystemId(), that.getSystemId()) &&
                Objects.equals(getTestId(), that.getTestId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVariables());
    }

    public void toJson(final JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("systemId").value(systemId);
        writer.name("testId").value(testId);
        writer.name("variables");
        variables.toJson(writer);
        writer.endObject();
    }

}
