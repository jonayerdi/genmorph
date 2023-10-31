package ch.usi.staticanalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import ch.usi.gassert.util.LogUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static ch.usi.staticanalysis.MethodDecoder.decode;


public class PurityAnalysis {
    private static final boolean DEBUG = false;

    private final List<String> outputPureMethodsForDaikon;
    private final Map<String, List<String>> classToMethodSignature;
    private final Map<String, Boolean> jdk;
    // for avoiding infinite transitive loop
    private final Stack<String> purityStack;
    private boolean globalResult = true;

    /**
     *
     */
    public PurityAnalysis(final String jdkPurityFile) {
        outputPureMethodsForDaikon = new LinkedList<>();
        classToMethodSignature = new HashMap<>();
        purityStack = new Stack<>();
        jdk = JDKPurityResult2.map;
    }

    public Map<String, List<String>> getClassToMethodSignature() {
        return classToMethodSignature;
    }

    public List<String> getOutputPureMethodsForDaikon() {
        return outputPureMethodsForDaikon;
    }

    public static void main(final String[] args) {
        final PurityAnalysis purity = new PurityAnalysis(null);
        for (final String m : purity.getPureMethods(args[0])) {
            System.out.println(m);
        }
    }

    public List<String> getPureMethods(final String className) {
        final List<String> pureMethods = new ArrayList<>(16);
        try {
            safeNet = 0;
            final ClassReader reader = new ClassReader(className);
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            @SuppressWarnings("unchecked") final List<MethodNode> mNodes = classNode.methods;
            for (final MethodNode m : mNodes) {
                final String methodName = m.name + m.desc;
                final Type returnType = Type.getReturnType(m.desc);
                // SKIP IF VOID OR RETURNS A OBJECT
                if (returnType == Type.VOID_TYPE || !isPrimitive(returnType) || Type.getArgumentTypes(m.desc).length > 0) {
                    continue; // skip those that do not return anything
                }
                globalResult = true;
                if (globalResult && isMethodPure(className, methodName, m)) {
                    pureMethods.add(m.name);
                }
            }
        } catch (final IOException e) {
            LogUtils.log().error("class " + className + " not found!");
            e.printStackTrace();
        }
        return pureMethods;
    }

    public static void main2(final String[] args) {
        System.out.println(args[0] + " " + args[1] + " " + args[2] + " ");
        final PurityAnalysis purity = new PurityAnalysis(args[1]);
        purity.navigate(args[0], new File(".").getAbsolutePath() + "/" + args[0]);
        purity.printFileMethods(args[2]);
    }

    private void navigate(final String delimiter, final String path) {
        final File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (file.getName().endsWith(".java")) {
            computePurityClass(file.getAbsolutePath().split(delimiter)[1].replaceAll("/", ".").replace(".java", ""));
        } else if (file.isDirectory()) {
            for (final File children : file.listFiles()) {
                navigate(delimiter, children.getAbsolutePath());
            }
        }
    }

    private void printFileMethods(final String file) {
        try {
            final FileWriter f = new FileWriter(new File(file), false);
            for (final String m : outputPureMethodsForDaikon) {
                f.write(m + System.lineSeparator());
            }
            f.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    int safeNet = 0;

    public void computePurityClass(final String className) {
        try {
            safeNet = 0;
            final ClassReader reader = new ClassReader(className);
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            @SuppressWarnings("unchecked") final List<MethodNode> mNodes = classNode.methods;
            classToMethodSignature.put(className, new LinkedList<>());
            for (final MethodNode m : mNodes) {
                final String methodName = m.name + m.desc;
                final Type returnType = Type.getReturnType(m.desc);
                // SKIP IF VOID OR RETURNS A OBJECT
                if (returnType == Type.VOID_TYPE || !isPrimitive(returnType) || Type.getArgumentTypes(m.desc).length > 0) {
                    continue; // skip those that do not return anything
                }
                globalResult = true;
                if (globalResult && isMethodPure(className, methodName, m)) {
                    outputPureMethodsForDaikon.add(decode(m, className));
                    if (methodName.contains("()")) { // no parameters
                        classToMethodSignature.get(className).add(m.name);
                    }
                }
            }
        } catch (final IOException e) {
            LogUtils.log().error("class " + className + " not found!");
            e.printStackTrace();
        }
    }


    private static boolean isPrimitive(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return true;
            default:
                return false;
        }
    }

    protected boolean isMethodPure(final String className, final String methodName, final MethodNode methodNode) {
        if (safeNet > 100) {
            globalResult = false;
            return false;
        }
        safeNet++;

        //System.out.println(purityStack);
        if (!globalResult) {
            return false;
        }
        if (className.startsWith("ch.usi.gassert.visitor.")) {
            return true;
        }

        // I'd say that these should always be pure, even though they technically don't have to
        if (className.equals("java.util.List") && (methodName.equals("size()I") || methodName.equals("isEmpty()Z"))) {
            return true;
        }

        final String key = className + "." + methodName;
        if (jdk.containsKey(key)) {
            if (key.contains("valueOf")) {
                return true;
            }
            if (DEBUG) {
                System.out.println("JDK " + key + " " + jdk.get(key));
            }
            return jdk.get(key);
        }
        if (methodNode.instructions.size() == 0) {
            if (DEBUG) {
                System.out.println("no instructions " + key + " " + jdk.get(key));
            }
            globalResult = false;
            return false;
        }
        @SuppressWarnings("unchecked") final Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
        purityStack.push(key);
        boolean result = true;
        while (it.hasNext()) {
            final AbstractInsnNode instNode = it.next();
            if (instNode.getOpcode() == Opcodes.PUTFIELD || instNode.getOpcode() == Opcodes.PUTSTATIC) {
                result = false;
                break;
            } else if (instNode instanceof MethodInsnNode) {
                final MethodInsnNode invokeNode = (MethodInsnNode) instNode;
                if (purityStack.contains(invokeNode.owner.replaceAll("/", ".") + "." + invokeNode.name + invokeNode.desc)) {
                    continue;
                }
                final String owner = invokeNode.owner.replaceAll("/", ".");
                if (owner.startsWith("ch.usi.gassert.visitor.")) {
                    continue;
                }
                final MethodNode invokeMethodNode = getMethodNode(owner, invokeNode.name + invokeNode.desc);
                if (invokeMethodNode == null) {
                    // we want to be conservative we dont want to say tha is pure when is not
                    result = false;
                    break;
                }
                purityStack.push(owner + "." + invokeNode.name + invokeNode.desc);
                result = isMethodPure(owner, invokeNode.name + invokeNode.desc, invokeMethodNode);
                purityStack.pop();
                globalResult = result && globalResult;

            }
        }
        if (DEBUG) {
            System.out.println(key + " " + result);
        }
        purityStack.pop();
        globalResult = result && globalResult;
        return globalResult;
    }


    private MethodNode getMethodNode(final String className, final String method) {
        final ClassReader reader;
        try {
            reader = new ClassReader(className);
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            final List<MethodNode> methods = classNode.methods;
            for (final MethodNode m : methods) {
                if (method.equals(m.name + m.desc)) {
                    return m;
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
