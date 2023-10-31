package ch.usi.gassert.util;

import java.util.Collection;

public class StringUtils {

    public static String replaceExtraLines(String s) {
        return removeSpaceOrTabs(s.replaceAll("(?m)^\\s*$[\n\r]{1,}", "").trim());
    }

    public static String removeSpaceOrTabs(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split(System.lineSeparator())) {
            if (line.startsWith("import ") || line.trim().startsWith("//")) {
                continue;
            }
            sb.append(line.trim());
            sb.append(System.lineSeparator());
        }
        return sb.toString().replaceAll(" \\(", "(").replaceAll("\\s\\{", "{").replace(" throws Exception{", "{").replace(" throws RuntimeException{", "{");
    }

    public static String quote(final String s) {
        return "\"" + s + "\"";
    }

    public static String parenthesize(final String s) {
        return s.startsWith("(") && s.endsWith(")") ? s : "(" + s + ")";
    }

    public static int getCommonPrefixIndex(final Collection<String> c) {
        String prefix = null;
        for (final String s : c) {
            if (prefix == null) {
                prefix = s;
            } else {
                for (int i = 0 ; i < prefix.length() ; ++i) {
                    if ((i == s.length()) || (prefix.charAt(i) != s.charAt(i))) {
                        prefix = prefix.substring(0, i);
                        break;
                    }
                }
            }
            if (prefix.isEmpty()) {
                break;
            }
        }
        return prefix == null ? 0 : prefix.length();
    }

    public static String removeAt(String str, int index) {
        return str.substring(0, index) + str.substring(index + 1);
    }

}
