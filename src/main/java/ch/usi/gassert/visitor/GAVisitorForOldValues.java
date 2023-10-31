package ch.usi.gassert.visitor;


import ch.usi.gassert.data.GetOldValues;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// is thread safe
public class GAVisitorForOldValues {

    private static GAVisitorForOldValues instance;

    private GAVisitorForOldValues() {
        super();
    }

    public static GAVisitorForOldValues getInstance() {
        if (instance == null) {
            instance = new GAVisitorForOldValues();
        }
        return instance;
    }


    public void enterMethod(final Object... pars) {
        final GetOldValues o = new GetOldValues(pars);
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("old-values-initialization.list"));
            //final File newFile = new File("ignore-old-values.txt");
            //if (!newFile.exists()) {
            writer.write(o.getLinesOfCodeToAdd());
            //}
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /*public void assertionPoint(Object... pars) {
    	assertionPoint(false, pars);
    }*/


}

