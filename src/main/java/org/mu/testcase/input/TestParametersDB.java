package org.mu.testcase.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TestParametersDB {

    public final Function<String, TestParameters> onMissingId;
    public final Map<String, TestParameters> params;

    public static final TestParametersDB EMPTY = new TestParametersDB();

    private static Map<String, TestParameters> buildMap(List<TestParameters> params) {
        Map<String, TestParameters> map = new HashMap<>();
        for (TestParameters param : params) {
            TestParameters prev = map.put(param.id, param);
            if (prev != null) {
                throw new RuntimeException("Duplicate key: " + param.id);
            }
        }
        return map;
    }

    private TestParametersDB() {
        this(null, (Map<String, TestParameters>)null);
    }

    public TestParametersDB(final Function<String, TestParameters> onMissingId) {
        this(onMissingId, new HashMap<>());
    }

    public TestParametersDB(final Map<String, TestParameters> params) {
        this(null, params);
    }

    public TestParametersDB(final List<TestParameters> params) {
        this(null, buildMap(params));
    }

    public TestParametersDB(final Function<String, TestParameters> onMissingId, final List<TestParameters> params) {
        this(onMissingId, buildMap(params));
    }

    public TestParametersDB(final Function<String, TestParameters> onMissingId, final Map<String, TestParameters> params) {
        this.onMissingId = onMissingId;
        this.params = params;
    }

    public TestParameters get(final String id) {
        if (id == null) {
            return null;
        } else if (params != null){
            if (params.containsKey(id)) {
                return params.get(id);
            } else if (onMissingId != null) {
                final TestParameters newValue = onMissingId.apply(id);
                if (newValue != null) {
                    params.put(id, newValue);
                    return newValue;
                }
            }
            throw new RuntimeException("Could not get params with ID: " + id);
        } else {
            throw new RuntimeException("Attempted to get params from empty TestParametersDB");
        }
    }

}
