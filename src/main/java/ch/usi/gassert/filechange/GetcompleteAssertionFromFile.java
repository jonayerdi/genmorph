package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class simply remove the assertions from the test generated we need this because some tests might fail.
 * remove both junit and java assertions
 */
public class GetcompleteAssertionFromFile {

    private GetcompleteAssertionFromFile() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) throws FileNotFoundException {
        printAssertion(args[0]);
    }

    public static void printAssertion(final String javaFilePath) {

        final List<String> assertionsInput = new ArrayList<>();

        try {

            final FileReader in = new FileReader(javaFilePath);
            final BufferedReader br = new BufferedReader(in);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("assert")) {
                    line = line.replace("assert (", "assert(");
                }
                final Matcher m = Pattern.compile("assert\\((.*?)\\);").matcher(line);
                if (m.find()) {
                    assertionsInput.add(m.group(0));
                }
            }
            in.close();


        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        System.out.println(assertionsInput.get(assertionsInput.size() - 1));
    }

}
