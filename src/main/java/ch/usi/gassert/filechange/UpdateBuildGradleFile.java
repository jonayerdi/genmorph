package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 * this class updates the build gradle file
 */
public class UpdateBuildGradleFile {


    private UpdateBuildGradleFile() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        updateBuildGradle(args[0]);
    }

    public static void updateBuildGradle(final String buildGradleFile) {
        FileUtils.overwriteTextOnFile(buildGradleFile, getNewFile(FileUtils.readContentFile(buildGradleFile)));
    }

    private static String getNewFile(final String readContentFile) {
        return readContentFile.split("allprojects \\{")[0] + newContent;

    }

    static String newContent = "allprojects {\n" +
            "    // add a collection to track failedTests\n" +
            "    ext.failedTests = []\n" +
            "    ext.passingTests = []\n" +
            "\n" +
            "    // add a testlistener to all tasks of type Test\n" +
            "    tasks.withType(Test) {\n" +
            "        afterTest { TestDescriptor descriptor, TestResult result ->\n" +
            "            if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE) {\n" +
            "                failedTests << \"${descriptor.className}.${descriptor.name}\"\n" +
            "            }\n" +
            "            if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS) {\n" +
            "                passingTests << \"${descriptor.className}.${descriptor.name}\"\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    // print out tracked failed tests when the build has finished\n" +
            "    gradle.buildFinished {\n" +
            "\n" +
            "        if (!failedTests.empty) {\n" +
            "            failedTests << \"\"\n" +
            "            new File(\"${project.buildDir}/failed-tests.txt\").text = failedTests.join(\"\\n\")\n" +
            "        }\n" +
            "        if (!passingTests.empty) {\n" +
            "            passingTests << \"\"\n" +
            "            new File(\"${project.buildDir}/passing-tests.txt\").text = passingTests.join(\"\\n\")\n" +
            "        }\n" +
            "    }\n" +
            "}";
}



