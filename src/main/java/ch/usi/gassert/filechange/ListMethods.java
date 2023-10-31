package ch.usi.gassert.filechange;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class enumerates the methods from the given Java source.
 */
public abstract class ListMethods {

    public static void main(final String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.err.println("Wrong number of parameters: 1 argument expected, got " + args.length);
            System.err.println("Java source file path");
            System.exit(1);
        }
        final String javaFilePath = args[0];
        for (final String method : listMethods(javaFilePath)) {
            System.out.println(method);
        }
    }

    public static List<String> listMethods(final String javaFilePath) throws FileNotFoundException {
        return listMethods(JavaParser.parse(new FileInputStream(javaFilePath)));
    }

    public static List<String> listMethods(final CompilationUnit cu) {
        final MethodListCollector visitor = new MethodListCollector();
        cu.accept(visitor, null);
        return visitor.methodsList;
    }

    private static class MethodListCollector extends VoidVisitorAdapter<Void> {
        public final List<String> methodsList;
        public MethodListCollector() {
            this.methodsList = new ArrayList<>(16);
        }
        @Override
        public void visit(MethodDeclaration method, Void arg) {
            this.methodsList.add(method.getNameAsString());
        }
    }

}
