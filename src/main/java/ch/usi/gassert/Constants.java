package ch.usi.gassert;

import java.util.Arrays;
import java.util.List;

/**
 * This class defines magic constants that can be used for the assertion generation
 */
public class Constants {

    public final static List<Number> numericConstants = Arrays.asList(
            1, 1, 1,
            2, 2,
            Math.PI, Math.PI,
            Math.PI / 2.0,
            Math.PI * 2.0,
            Math.E,
            Math.sqrt(2)
    );

}
