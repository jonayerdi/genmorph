package ch.usi.methodtest.transformations;

import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;
import ch.usi.methodtest.MethodTest;

import java.util.List;

public interface MethodTestTransformation {

    List<String> getParameters();

    List<ConjunctiveMRIPClause> makeClauses(final List<String> parameters);

    List<MethodParameter> transform(final List<MethodParameter> parameters);

    List<List<Integer>> findTransformations(final MethodTest source);

    public static ConjunctiveMRIPClause makeDefaultClause(final String var) {
        return ConjunctiveMRIPClause.buildSimple(
                MR.VARIABLE_TO_FOLLOWUP(var),
                "==",
                MR.VARIABLE_TO_SOURCE(var)
        );
    }

}
