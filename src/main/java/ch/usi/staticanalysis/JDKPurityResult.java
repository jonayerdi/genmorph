package ch.usi.staticanalysis;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * this class reads the purity results of the JDK
 */
public class JDKPurityResult implements Serializable {

    private static final long serialVersionUID = 261840226281221534L;

    private final Map<String, Boolean> methodToPureMap;

    public JDKPurityResult(final Map<String, Boolean> methodToPureMap) {
        this.methodToPureMap = methodToPureMap;
    }

    public Map<String, Boolean> getMethodToPureMap() {
        return methodToPureMap;
    }

    public final void dumpToFile(final String fileName) {

        try {
            final FileOutputStream fileOut = new FileOutputStream(fileName);
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public final static JDKPurityResult loadFromFile(final String fileName) {
        JDKPurityResult test = null;
        try {
            final FileInputStream fis = new FileInputStream(fileName);
            final ObjectInputStream ois = new ObjectInputStream(fis);
            test = (JDKPurityResult) ois.readObject();
            ois.close();
            fis.close();
            return test;
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
        return test;
    }

    public static void main(final String[] args) {
        final JDKPurityResult o = loadFromFile("jdk-purity.ser");
        int count = 0;
        int countm = 0;
        for (final Map.Entry e : o.methodToPureMap.entrySet()) {
            if (e.getKey().toString().startsWith("java.lang")) {
                if (count % 500 == 0) {
                    System.out.println("}");
                    System.out.println("private static void m" + countm + "() {");
                    countm++;
                }
                count++;

                System.out.println("map.put(\"" + e.getKey() + "\"," + e.getValue() + ");");
            }
        }
        System.out.println("private static void m" + countm + "() {");

    }
}


