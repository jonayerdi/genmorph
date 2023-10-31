package ch.usi.methodtest;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

public class MethodTest {

    public static final String EXTENSION = ".methodinputs";

    private static final XStream xstream = new XStream();

    static {
        xstream.allowTypesByWildcard(new String[] {
                "**"
        });
    }

    public String methodName;
    public MethodParameter[] methodParameters;

    public MethodTest(String methodName, MethodParameter[] methodParameters) {
        this.methodName = methodName;
        this.methodParameters = methodParameters;
    }

    public MethodTest(MethodTest other) {
        this.methodName = other.methodName;
        this.methodParameters = other.methodParameters.clone();
    }

    public static MethodTest fromXML(final String xml) {
        return (MethodTest) xstream.fromXML(xml);
    }

    public static MethodTest fromXML(final Reader reader) {
        return (MethodTest) xstream.fromXML(reader);
    }

    public static MethodTest fromXML(final InputStream istream) {
        return (MethodTest) xstream.fromXML(istream);
    }

    public static MethodTest fromXML(final File file) {
        return (MethodTest) xstream.fromXML(file);
    }

    public void toXML(final Writer writer) {
        xstream.toXML(this, writer);
    }

    @Override
    public String toString() {
        return methodName + Arrays.toString(methodParameters);
    }

}
