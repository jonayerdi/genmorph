package ch.usi.gassert.data;


import ch.usi.gassert.util.ClassUtils;
import ch.usi.staticanalysis.PurityAnalysis;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * this class represents a Program State
 */
public class GetOldValues {
    static private PurityAnalysis pureAnalysis = new PurityAnalysis("jdk-purity.ser");

    public List<String> getOldVariableDeclaration() {
        return oldVariableDeclaration;
    }

    public String getLinesOfCodeToAdd() {
        final StringBuilder ab = new StringBuilder();
        for (final String l : oldVariableDeclaration) {
            ab.append(l);
            ab.append("\n");

        }
        return ab.toString();
    }

    private final List<String> oldVariableDeclaration;

    /**
     * first element is the name and the second is the value
     *
     * @param pars
     */
    public GetOldValues(final Object... pars) {
        oldVariableDeclaration = new LinkedList<>();
        final Map<String, Object> var2obj = new HashMap<>();
        // i == 0 skip because is name of the method
        for (int i = 1; i < pars.length; i = i + 2) {
            var2obj.put(String.valueOf(pars[i]), pars[i + 1]);
        }
        for (final Map.Entry<String, Object> entry : var2obj.entrySet()) {
            final String typeWrapper = ClassUtils.getTypeWrapper(entry.getValue().getClass().toString());
            if (!ClassUtils.isPrimitiveType(typeWrapper)) {
                //    oldVariableDeclaration.add(typeWrapper.replace("class ", "") + " old_" + entry.getKey() + " = " + entry.getKey() + ";");
                // } else { // skip non this parameters **/
                addNonPrimitiveType(entry.getKey(), entry.getValue());
            }
            //}
        }
    }

    private void addNonPrimitiveType(final String variableName, final Object value) {
        addFields(variableName, value);
        addObserver(variableName, value);
    }

    private void addObserver(final String variableName, final Object value) {
        if (variableName.endsWith("_object") || value.getClass().isArray()) {
            return;
        }

        try {
            // use pure methods
            if (!pureAnalysis.getClassToMethodSignature().containsKey(value.getClass().getName())) {
                pureAnalysis.computePurityClass(value.getClass().getName());
            }
            for (final String methodName : pureAnalysis.getClassToMethodSignature().get(value.getClass().getName())) {
                final Method method = value.getClass().getMethod(methodName);
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    oldVariableDeclaration.add(method.getReturnType().toString().replace("class ", "") + " old_" + variableName + "_" + methodName + " = " + variableName + "." + methodName + "();");
                }
            }
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private void addFields(final String variableName, final Object value) {

        if (!variableName.equals("this") || true) {
            return;
        }

        for (final Field f : value.getClass().getDeclaredFields()) {
            try {
                if (/*!java.lang.reflect.Modifier.isPublic(f.getModifiers()) || */java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                if (ClassUtils.isPrimitiveType(f.getType().toString()) || f.getType().isPrimitive()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        oldVariableDeclaration.add(f.getType().toString().replace("class ", "") + " old_" + variableName + "." + f.getName() + " = " + f.getName() + ";");
                    } else {
                        oldVariableDeclaration.add(f.getType().toString().replace("class ", "") + " old_" + variableName + "." + f.getName() + " = " + variableName + "." + f.getName() + ";");
                    }
                }
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }


}
