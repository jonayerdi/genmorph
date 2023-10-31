package ch.usi.gassert.util;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;

public class PITUtils {
	
	public static void main(String[] args) {
		String out = getOldInstLines(args[0]);
		System.out.println(out);
		//getOldInstLines("/Users/usi/GAssert/GAssert/subjects/simple-examples_getMin/orig-old-values/SimpleMethods.java");
	}
	
	public static String getOldInstLines(String fileLocation) {
		
		FileInputStream in;
		try {
			in = new FileInputStream(fileLocation);
			CompilationUnit cu = JavaParser.parse(in);
			
			List<Integer> instrumentationLines = new ArrayList<Integer>();
			List<Comment> commentList= cu.getComments();
			for (Comment comment:commentList) {
				if (comment.isLineComment() && comment.getContent().trim().equals("instrumentation")) {
					instrumentationLines.add(comment.getEnd().get().line + 1);
				}
			}

			String instrLines = instrumentationLines.toString();
			instrLines = instrLines.replace("[", ",");
			instrLines = instrLines.replace("]", ",");
			instrLines = instrLines.replace(" ", "");
			
			return instrLines;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		
		
	}

}
