package ch.usi.gassert.serialization;

import ch.usi.methodtest.MethodParameter;
import ch.usi.methodtest.MethodTest;
import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static ch.usi.gassert.util.FileUtils.SEPARATORS;

/*
 * Serializer calls added indirectly by ch.usi.gassert.filechange.AddInstrumentationMethod
 */
@SuppressWarnings("unused")
public class InputsSerializingMethodVisitor {

    public static final InputsSerializingMethodVisitor INSTANCE = new InputsSerializingMethodVisitor();

    public static InputsSerializingMethodVisitor getInstance() {
        return INSTANCE;
    }

    public static void enter(final Object... args) {
        INSTANCE.serializeEnter(args);
    }

    public static void exit(final Object... args) {
        INSTANCE.serializeExit(args);
    }

    final XStream xstream;
    final File outdirfile;

    public InputsSerializingMethodVisitor() {
        this.xstream = new XStream();
        this.outdirfile = new File(System.getenv("gassert_outdir"));
        if (!outdirfile.isDirectory() && !outdirfile.mkdirs()) {
            throw new RuntimeException("Could not create output directory: " + this.outdirfile);
        }
    }

    public void serializeEnter(final Object... args) {
        final Iterator<?> argsIter = Arrays.stream(args).iterator();
        final String systemId = (String) argsIter.next();
        final String methodName = (String) argsIter.next();
        final Integer methodIndex = (Integer) argsIter.next();
        final int paramsStart = 3;
        final int paramsLength = args.length - paramsStart;
        final List<MethodParameter> methodParameters = new ArrayList<>();
        while (argsIter.hasNext()) {
            final String name = (String) argsIter.next();
            final Class<?> clazz = (Class<?>) argsIter.next();
            final Object value = argsIter.next();
            argsIter.next(); // isMutable
            methodParameters.add(new MethodParameter(name, clazz, value));
        }
        final MethodTest methodTest = new MethodTest(methodName, methodParameters.toArray(new MethodParameter[0]));
        writeInputs(systemId, methodTest);
    }

    public void serializeExit(final Object... args) {
        // Nothing to do here...
    }

    public void writeInputs(String systemId, final MethodTest methodTest) {
        try {
            final Writer writer = new FileWriter(File.createTempFile(systemId + SEPARATORS[0] + "test", MethodTest.EXTENSION, outdirfile));
            xstream.toXML(methodTest, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing out method inputs", e);
        }
    }

}
