package ch.usi.gassert.data.state;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class TestExecutionTest {

    static final String[] JSONS = {
        "{\"systemId\":\"demo.MyClass$sin$0@M7\",\"testId\":\"test2\",\"variables\":{\"inputs\":{\"i_y\":1.5707963267948966,\"i_this.PRECISION\":20.0},\"outputs\":{\"o_return\":0.9999999999965856}}}",
        "{\"systemId\":\"demo.MyClass$sin$0@M10\",\"testId\":\"test0\",\"variables\":{\"inputs\":{\"i_y\":null,\"i_this.PRECISION\":20.0},\"outputs\":{\"o_return\":\"ERROR[NullPointerException]\"}}}"
    };

    @Test
    public void testFromJson() {
        for (int i = 0 ; i < JSONS.length ; ++i) {
            try {
                String json = JSONS[i];
                JsonReader reader = new JsonReader(new StringReader(json));
                TestExecution execution = TestExecution.fromJson(reader);
                Writer writer = new StringWriter();
                execution.toJson(new JsonWriter(writer));
                TestExecution execution2 = TestExecution.fromJson(new JsonReader(new StringReader(writer.toString())));
                assertEquals(execution, execution2);
            } catch (Exception e) {
                throw new RuntimeException("Error in JSON index " + i, e);
            }
        }
    }

}
