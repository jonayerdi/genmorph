package ch.usi.oasis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import java.io.FileWriter;

public class ResultsAnalyser {

    public static ArrayList<String> getTimestampList(File resultsFile) {
        ArrayList<String> timestampList = new ArrayList<String>();
        File files[] = resultsFile.listFiles();
        for (File file: files) {
            if (file.getName().contains("exec_log")) {
                String timestamp = file.getName().replace("exec_log", "").replace(".txt", "");
                timestampList.add(timestamp);


                getExecNumbers(resultsFile, timestamp);
                checkErrFile(resultsFile, timestamp);
                checkExecHistory(resultsFile);
            }
        }
        Collections.sort(timestampList);
        return timestampList;
    }

    public static void checkErrFile(File resultsFile, String timestamp) {
        File file = new File(resultsFile + "/evo_err" + timestamp + ".txt");
        try {
            String content = new String(Files.readAllBytes(Paths.get(resultsFile + "/evo_err" + timestamp + ".txt")), StandardCharsets.UTF_8);
            if (content.length() > 0) {
                System.out.println("non-empty err file:" + resultsFile + "/evo_err" + timestamp + ".txt");
            }
        } catch (IOException e) {
            System.out.println("err exception");
            e.printStackTrace();
        }
    }

    public static void checkExecHistory(File resultsFile) {
        File file = new File(resultsFile + "/exec_history.csv");
        long lineCount = 0;
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
            lineCount = stream.count();
        } catch (IOException e) {
            System.out.println("hist exception");
            e.printStackTrace();
        }
        if (lineCount > 10) {
            System.out.println("problematic exec_hist:" + file);
        }
    }

    public static int[] getExecNumbers(File resultsFile, String timestamp){
       //System.out.println(resultsFile + "/exec_log" + timestamp + ".txt");
       File file = new File(resultsFile + "/exec_log" + timestamp + ".txt");
       List<String> enterStatesList = new ArrayList<String>();
       List<String> inputCondStatesList = new ArrayList<String>();

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("entered")) {
                    enterStatesList.add(line.replace("entered", ""));
                }
                if (line.contains("input_condition_done")) {
                    inputCondStatesList.add(line.replace("input_condition_done", ""));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int[] returnArray = new int[]{enterStatesList.size(), new HashSet<String>(enterStatesList).size(), inputCondStatesList.size(), new HashSet<String>(inputCondStatesList).size()};
        //System.out.println(returnArray[0] + " " + returnArray[1] + " " + returnArray[2] + " " + returnArray[3]);
        return returnArray;
    }


    public static int countTestCasesNum(File testFile) {
        int testNum = 0;
        FileInputStream in = null;
        try {
            in = new FileInputStream(testFile);
            CompilationUnit cu = JavaParser.parse(in);
            for (TypeDeclaration typeDec : cu.getTypes()) {
                List<BodyDeclaration> members = typeDec.getMembers();
                for (BodyDeclaration member : members) {
                    if (member instanceof MethodDeclaration) {
                        MethodDeclaration md = (MethodDeclaration) member;
                        if (!(md.getNameAsString().contains("notGeneratedAnyTest"))) {
                            testNum++;

                        }
                    }
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testNum;
    }

    public static void main(String[] args) {
        File folder = new File("/Users/usi/Downloads/mrsNoFP_FM/");
        File[] listOfFiles = folder.listFiles();

        int noTests = 0;
        int overallNum = 0;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isDirectory()) {
                File[] listOfMethods = listOfFiles[i].listFiles();
                for (File methodFile: listOfMethods) {
                    if (!methodFile.getName().contains("DS_Store")) {
                        File[] resultFiles = methodFile.listFiles();
                        for (File resultFile : resultFiles) {
                            if (resultFile.isDirectory() && resultFile.getName().contains("new_test_120")) {
                                overallNum++;

                                ArrayList<String> tsList = getTimestampList(resultFile);
                                String fileName = methodFile.getName();
                                //System.out.println(fileName);
                                String[] subStr = fileName.split("%");
                                String className = subStr[0];
                                //System.out.println(className);
                                String testLoc = resultFile.getAbsolutePath()+ "/" + className.replace(".", "/");
                                File testDir = new File(testLoc);

                                int testNum = 0;
                                int overallEnterNum = 0;
                                int overallInputCondNum = 0;

                                for (String ts: tsList) {
                                    File testFile = new File(testDir + ts + "_ESTest.java");
                                    int[] execNumbers = getExecNumbers(resultFile, ts);
                                    if (testFile.exists()) {
                                        testNum = countTestCasesNum(testFile);
                                        overallEnterNum += execNumbers[1];
                                        overallInputCondNum += execNumbers[3];
                                        if (testNum > 0) {
                                            break;
                                        }
                                    }
                                }
                                String overallStr = resultFile.getAbsolutePath();
                                String approach = overallStr.substring(overallStr.indexOf("assertions_") + "assertions_".length(), overallStr.indexOf("_seed"));
                                String seed = overallStr.substring(overallStr.indexOf("_seed") + 5, overallStr.indexOf("org") - 1);
                                String mrNum = overallStr.substring(overallStr.indexOf("MR") + 2, overallStr.length());
                                //System.out.println(approach + " " + seed + " " + className + " " + subStr[1] + " " + mrNum);

                                double ratio = (double) overallInputCondNum/overallEnterNum;
                                try (PrintWriter writer = new PrintWriter(new FileWriter("output.csv",true))) {

                                    StringBuilder sb = new StringBuilder();
                                    sb.append(approach);
                                    sb.append(',');
                                    sb.append(seed);
                                    sb.append(',');
                                    sb.append(className);
                                    sb.append(',');
                                    sb.append(subStr[1]);
                                    sb.append(',');
                                    sb.append(mrNum);
                                    sb.append(',');
                                    sb.append(testNum);
                                    sb.append(',');
                                    sb.append(ratio);
                                    sb.append('\n');
                                    writer.write(sb.toString());
                                } catch (FileNotFoundException e) {
                                    System.out.println(e.getMessage());
                                } catch (IOException e) {
                                    System.out.println(e.getMessage());
                                }
                                //System.out.println(resultFile.getAbsolutePath() + ", " + testNum + ", " + ratio );
                                if (testNum == 0) {
                                    noTests++;
                                }


                            }

                        }
                    }
                }
            }
        }


        System.out.println(noTests + " " + overallNum);

    }
}
