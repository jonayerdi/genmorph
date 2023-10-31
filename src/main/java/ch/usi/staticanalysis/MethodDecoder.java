package ch.usi.staticanalysis;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceSignatureVisitor;

/**
 * This dcoder is needed for daikon, to give in input the purity file
 */
public class MethodDecoder {

    public static String decode(final MethodNode m, final String className) {
        final String[] array = new String[m.exceptions.size()];
        int i = 0;
        for (final Object o : m.exceptions) {
            array[i] = o.toString();
            i++;
        }
        return decode(m.access, className + "." + m.name, m.desc, m.signature, array);
    }


    /**
     * get a MethodNode and returns the decode String
     *
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @return
     */
    public static String decode(final int access, final String name, final String desc, String signature, final String[] exceptions) {
        if (signature == null) signature = desc;
        final StringBuilder sb = new StringBuilder();
        appendModifiers(sb, access);
        final TraceSignatureVisitor v = new TraceSignatureVisitor(0);
        new SignatureReader(signature).accept(v);

        final String declaration = v.getDeclaration();
        final String rType = v.getReturnType();
        if (declaration.charAt(0) == '<')
            sb.append(declaration, 0, declaration.indexOf("(")).append(' ');
        else if (rType.isEmpty() || rType.charAt(0) == '[')
            sb.append("java.lang.Object");
        sb.append(rType).append(' ').append(name)
                .append(declaration, declaration.indexOf('('), declaration.length());
        if ((access & Opcodes.ACC_VARARGS) != 0 && declaration.endsWith("[])"))
            sb.replace(sb.length() - 3, sb.length(), "...)");
        final String genericExceptions = v.getExceptions();
        if (genericExceptions != null && !v.getDeclaration().isEmpty())
            sb.append(" throws ").append(genericExceptions);
        else if (exceptions != null && exceptions.length > 0) {
            sb.append(" throws ");
            int pos = sb.length();
            for (final String e : exceptions) sb.append(e).append(", ");
            final int e = sb.length() - 2;
            sb.setLength(e);
            for (; pos < e; pos++) if (sb.charAt(pos) == '/') sb.setCharAt(pos, '.');
        }
        return sb.toString();
    }

    private static void appendModifiers(final StringBuilder buf, int access) {
        for (int bit; access != 0; access -= bit) {
            bit = access & -access;
            switch (bit) {
                case Opcodes.ACC_PUBLIC:
                    buf.append("public ");
                    break;
                case Opcodes.ACC_PRIVATE:
                    buf.append("private ");
                    break;
                case Opcodes.ACC_PROTECTED:
                    buf.append("protected ");
                    break;
                case Opcodes.ACC_STATIC:
                    buf.append("static ");
                    break;
                case Opcodes.ACC_FINAL:
                    buf.append("final ");
                    break;
                case Opcodes.ACC_ABSTRACT:
                    buf.append("abstract ");
                    break;
                case Opcodes.ACC_NATIVE:
                    buf.append("native ");
                    break;
                case Opcodes.ACC_STRICT:
                    buf.append("strictfp ");
                    break;
                case Opcodes.ACC_SYNCHRONIZED:
                    buf.append("synchronized ");
                    break;
            }
        }
    }
}
