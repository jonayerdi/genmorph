package ch.usi.gassert.util;

import ch.usi.gassert.Config;
import ch.usi.gassert.Stats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyGson {

    static {
        instance = new GsonBuilder().disableHtmlEscaping().create();
    }

    static private Gson instance;

    public static Gson getInstance() {
        if (instance == null) {
            instance = new GsonBuilder().disableHtmlEscaping().create();
        }
        return instance;
    }

    public static void storeTestExecFile(final Object o) {
        System.out.println("folder is " + Config.pathDataFolder);
        final String json = getInstance().toJson(o);
        final File tmp = new File(Config.pathDataFolder);
        if (!tmp.exists()) {
            System.out.println("folder not exists");
            throw new RuntimeException("foder does not exists" + tmp.getAbsolutePath() + " I am here" + new File("").getAbsolutePath());
        }
        final long id = System.currentTimeMillis(); // before I used .listFile.lenght but had problems
        try {
            final FileWriter writer = new FileWriter(tmp.getAbsolutePath() + "/ser" + id + ".json");
            writer.write(json);
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }


}
