package ch.usi.gassert.data.state;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class VariablesTest {

    static final String[] JSONS = {
            "{\"inputs\":{\"i_y\":null,\"i_this.PRECISION\":20.0},\"outputs\":{\"o_return\":\"ERROR[NullPointerException]\"}}"
    };

    @Test
    public void testFromJson() throws IOException {
        for (String json : JSONS) {
            JsonReader reader = new JsonReader(new StringReader(json));
            Variables variables = Variables.fromJson(reader);
            Writer writer = new StringWriter();
            variables.toJson(new JsonWriter(writer));
            Variables variables2 = Variables.fromJson(new JsonReader(new StringReader(writer.toString())));
            assertEquals(variables, variables2);
        }
    }

}
