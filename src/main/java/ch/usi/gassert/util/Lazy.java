package ch.usi.gassert.util;

import java.util.Optional;
import java.util.function.Function;

public class Lazy<P, V> {

    private final Function<P, V> lazyOperation;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<V> value;

    public Lazy(Function<P, V> lazyOperation) {
        this.lazyOperation = lazyOperation;
        this.value = Optional.empty();
    }

    public V get(final P param) {
        if (!value.isPresent()) {
            value = Optional.of(this.lazyOperation.apply(param));
        }
        return value.get();
    }

}
