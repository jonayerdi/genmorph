package ch.usi.gassert.evaluator;

import ch.usi.gassert.data.tree.Tree;
import com.udojava.evalex.Expression;

import java.util.Map;

public interface IEvaluator {
    boolean eval(final Tree tree, final Map<String, Object> name2value);
}
