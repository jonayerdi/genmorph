package ch.usi.oasis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class FEPAnalyseResults {

    private static double calculateStandardDeviation(int[] array) {

        // finding the sum of array values
        double sum = 0.0;

        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }

        // getting the mean of array.
        double mean = sum / array.length;

        // calculating the standard deviation
        double standardDeviation = 0.0;
        for (int i = 0; i < array.length; i++) {
            standardDeviation += Math.pow(array[i] - mean, 2);

        }

        return Math.sqrt(standardDeviation/array.length);
    }

    public HashSet<String> getPrefixList(String dir) {
        File file = new File(dir);
        String[] fileList = file.list();
        HashSet<String> prefixList = new HashSet<String>();
        String subStr = "_ESTest_";
        for (String f: fileList) {
            String prefix = f.substring(0, f.lastIndexOf(subStr) + subStr.length() -  1);
            System.out.println(prefix);
            if (!prefix.equals(".DS_St")) {
                prefixList.add(prefix);
            }
        }
        return prefixList;
    }

    public static void main(String[] args) throws IOException {
        String resultLocation = "/Users/usi/Documents/fep_again/math_generated_testcases/";
        FEPAnalyseResults far = new FEPAnalyseResults();
        HashSet<String> prefixList = far.getPrefixList(resultLocation);
        String[] distanceList = {"NCD_SHOCSZLIB"};

        PrintWriter csvWriter = new PrintWriter("fep_tests.csv");

        int overallSize = 0;
        int overallSizeBytes = 0;
        int testNum = 0;

        for (String className: prefixList) {
            StringBuilder sb = new StringBuilder();
            sb.append(className); sb.append(",");
            for (String distance : distanceList) {
                int[] testCaseNumArray = new int[5];
                int prefixTestNum = 0;
                for (int run = 0; run < 5; run++) {
                    int runTestNum = 0;
                    String javaFile = resultLocation + className + "_" + distance + "_" + run + ".java";
                    CompilationUnit cu = null;
                    try {
                        FileInputStream in = new FileInputStream(javaFile);
                        cu = JavaParser.parse(in);
                        for (TypeDeclaration typeDec : cu.getTypes()) {
                            List<BodyDeclaration> members = typeDec.getMembers();
                            for (BodyDeclaration member : members) {
                                if (member instanceof MethodDeclaration) {
                                    MethodDeclaration md = (MethodDeclaration) member;
                                    if (!(md.getNameAsString().contains("notGeneratedAnyTest"))) {
                                        String testBody = md.getBody().get().toString();

                                        testNum++;
                                        prefixTestNum++;
                                        runTestNum++;

                                        overallSize += testBody.length();
                                        overallSizeBytes += testBody.getBytes("UTF-8").length;
                                    }
                                }
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    testCaseNumArray[run] = runTestNum;
                }

                double averagePrefixTestNum = (double) prefixTestNum / 5;

                IntSummaryStatistics ss = new IntSummaryStatistics();
                for (int element:testCaseNumArray) {
                    ss.accept(element);
                }
                Arrays.sort(testCaseNumArray);
                sb.append(ss.getMin()); sb.append(",");
                sb.append(ss.getMax()); sb.append(",");
                sb.append(ss.getAverage()); sb.append(",");
                sb.append(calculateStandardDeviation(testCaseNumArray)); sb.append(",");
                System.out.println(className + ":" + distance + ":" + prefixTestNum + ":" + averagePrefixTestNum);
                for (int element: testCaseNumArray) {
                    System.out.println(element);
                }
            }
            sb.append("\n");
            csvWriter.write(sb.toString());
        }
        csvWriter.close();
        System.out.println(overallSize/testNum);
        System.out.println(overallSizeBytes/testNum);
    }

}
