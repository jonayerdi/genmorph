package ch.usi.gassert.util;


import ch.usi.gassert.Config;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogUtils {

    private static class Holder {

        static final Logger INSTANCE = Holder.getLogger();

        private static Logger getLogger() {
            final ConsoleAppender console = new ConsoleAppender(); // create
            //final FileAppender file = new FileAppender();
            //file.setFile("logs" + File.separator + Config.keyExperiment + ".log");
            // appender
            // configure the appender
            final String PATTERN = "%d %c.%M(%F:%L)     %n %m %n %n";
            console.setLayout(new PatternLayout(PATTERN));
            console.setThreshold(Config.loggerLevel);
            console.activateOptions();
            //file.setLayout(new PatternLayout(PATTERN));
            //file.setThreshold(Config.loggerLevel);
            //file.activateOptions();
            // add appender to any Logger (here is root)
            final Logger logger = Logger.getLogger("");
            logger.addAppender(console);
            //logger.addAppender(file);
            return logger;
        }


    }

    /**
     * @param fitness
     * @param type           can be FN, FP or FNplusFP
     * @param stringToAppend
     */
    public static void appendCSV(final String fitness, final String type, final String stringToAppend) {
        BufferedWriter writer = null;
        final File f = new File("logs" + File.separator + Config.keyExperiment + "_fitness_" + fitness + "_type_" + type + ".csv");
        LogUtils.log().info("file CSV " + f.getAbsolutePath());
        try {
            writer = new BufferedWriter(
                    new FileWriter("logs" + File.separator + Config.keyExperiment + "_fitness_" + fitness + "_type_" + type + ".csv", true));
            writer.newLine();   //Add new line
            writer.write(stringToAppend);
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger log() {
        return Holder.INSTANCE;
    }
}
