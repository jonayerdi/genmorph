package ch.usi.gassert.evaluator;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeEval;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.util.Utils;
import com.udojava.evalex.Expression;

import java.util.Map;

public class BasicEvaluator implements IEvaluator {

    public final boolean onError;

    public BasicEvaluator() {
        this(false);
    }

    public BasicEvaluator(boolean onError) {
        this.onError = onError;
    }

    @Override
    public boolean eval(final Tree tree, final Map<String, Object> name2value) {
        try {
            return TreeEval.evalBool(tree, name2value, Config.EVAL_NUMBER_PRECISION);
        } catch (Throwable ignored) {}
        return this.onError;
    }

}
