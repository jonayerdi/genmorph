package org.mu.testcase.input;

import org.mu.util.ISerializable;

public abstract class TestInput implements ISerializable {

    public final String id;

    public TestInput(String id) {
        this.id = id;
    }

    public abstract Double estimatedCost();

    public abstract TestInputFeatures getFeatures();

}
