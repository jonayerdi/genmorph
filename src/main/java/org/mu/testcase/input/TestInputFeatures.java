package org.mu.testcase.input;

import java.util.List;

public abstract class TestInputFeatures {
    public abstract double getFeature(String name);
    public abstract void setFeature(String name, double value);
    public abstract List<Double> asVector();
}
