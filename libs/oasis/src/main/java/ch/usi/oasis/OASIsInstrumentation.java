package ch.usi.oasis;

import java.util.ArrayList;
import java.util.List;

public class OASIsInstrumentation {

    public static void main(String[] args) {
        //String dir = "/Users/usi/Downloads/mrs_single/assertions_baseline1_seed32/org.apache.commons.math3.util.FastMath_sin_0/new_test_120MR23/";
        String fileLocation = "/Users/usi/Documents/GAssert/MRs/commons-math3-3.6.1-src-1/src/main/java/org/apache/commons/math3/util/FastMath.java";
        String methodName = "sin";

        List<String> inputRelList = new ArrayList<>();
        List<String> outputRelList = new ArrayList<>();

        inputRelList.add("(((double) i_this_f.PI) < ((Math.abs((((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_x_s) / ((double) i_x_f))) + (((double) i_x_f) - ((double) i_x_s)))) < 1.0E-4) ? 1.0 : ((((double) i_this_s.PI) - (((double) i_this_f.E) - ((double) i_x_s))) / (((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_x_s) / ((double) i_x_f))) + (((double) i_x_f) - ((double) i_x_s))))))");
        outputRelList.add("(((Math.abs(((((double) o_return_s) * ((double) o_return_f)) - ((Math.abs(((double) i_this_f.PI)) < 1.0E-4) ? 1.0 : ((((double) o_return_f) + ((double) o_return_f)) / ((double) i_this_f.PI))))) < 1.0E-4) ? 1.0 : (((Math.abs(((double) i_x_s)) < 1.0E-4) ? 1.0 : (((double) i_x_f) / ((double) i_x_s))) / ((((double) o_return_s) * ((double) o_return_f)) - ((Math.abs(((double) i_this_f.PI)) < 1.0E-4) ? 1.0 : ((((double) o_return_f) + ((double) o_return_f)) / ((double) i_this_f.PI)))))) <= ((Math.abs(((((double) i_x_f) * ((double) i_this_s.E)) * ((1.466 * ((double) o_return_f)) + ((double) i_this_f.E)))) < 1.0E-4) ? 1.0 : ((((((double) i_this_s.E) * ((double) i_x_f)) - 3.1416) * 1.9264) / ((((double) i_x_f) * ((double) i_this_s.E)) * ((1.466 * ((double) o_return_f)) + ((double) i_this_f.E))))))");

        inputRelList.add("((Math.abs(((double) i_this_s.E) - ((double) i_x_s)) < 1.0E-4) && ((((double) i_this_s.PI) * ((double) i_x_f)) >= (((double) i_this_f.PI) + ((double) i_this_s.PI))))");
        outputRelList.add("((((Math.abs(((double) o_return_s)) < 1.0E-4) ? 1.0 : (((double) i_this_f.E) / ((double) o_return_s))) - ((Math.abs(((Math.abs(((double) o_return_f)) < 1.0E-4) ? 1.0 : (((double) i_this_f.E) / ((double) o_return_f)))) < 1.0E-4) ? 1.0 : (((Math.abs(((Math.abs((((double) o_return_f) - ((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_this_s.E) / ((double) i_x_f))))) < 1.0E-4) ? 1.0 : (((double) i_this_f.E) / (((double) o_return_f) - ((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_this_s.E) / ((double) i_x_f))))))) < 1.0E-4) ? 1.0 : (((double) i_this_s.E) / ((Math.abs((((double) o_return_f) - ((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_this_s.E) / ((double) i_x_f))))) < 1.0E-4) ? 1.0 : (((double) i_this_f.E) / (((double) o_return_f) - ((Math.abs(((double) i_x_f)) < 1.0E-4) ? 1.0 : (((double) i_this_s.E) / ((double) i_x_f)))))))) / ((Math.abs(((double) o_return_f)) < 1.0E-4) ? 1.0 : (((double) i_this_f.E) / ((double) o_return_f)))))) >= (((double) i_this_s.E) + ((double) i_this_f.PI)))");

        inputRelList.add("(i_x_f <= (i_x_s + (0 - 1)))");
        outputRelList.add("(Math.abs(((o_return_s * i_this_s.PI) * i_x_s) - ((Math.abs((((Math.abs(o_return_s) < 1.0E-4) ? 1.0 : (o_return_f / o_return_s)) + ((Math.abs((i_x_f - i_this_s.PI)) < 1.0E-4) ? 1.0 : (i_x_s / (i_x_f - i_this_s.PI))))) < 1.0E-4) ? 1.0 : (i_this_f.E / (((Math.abs(o_return_s) < 1.0E-4) ? 1.0 : (o_return_f / o_return_s)) + ((Math.abs((i_x_f - i_this_s.PI)) < 1.0E-4) ? 1.0 : (i_x_s / (i_x_f - i_this_s.PI))))))) >= 1.0E-4)");

        FalsePositiveTransformation fpt = new FalsePositiveTransformation(fileLocation, methodName,
                                                                                inputRelList, outputRelList);

        String lineList = OASIs.transformForFP(fileLocation, methodName, inputRelList, outputRelList);
        System.out.println(lineList);
    }
}
