package ch.usi.gassert.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Cache<K, V> {

    private final Function<K, V> onCacheMiss;
    private final Map<K, V> data;

    public Cache(final Function<K, V> onCacheMiss) {
        this(onCacheMiss, new HashMap<>());
    }

    public Cache(final Function<K, V> onCacheMiss, final int initialCapacity) {
        this(onCacheMiss, new HashMap<>(initialCapacity));
    }

    public Cache(final Function<K, V> onCacheMiss, final Map<K, V> data) {
        this.onCacheMiss = onCacheMiss;
        this.data = data;
    }

    public V get(final K key) {
        V value = this.data.get(key);
        if(value == null) {
            value = this.onCacheMiss.apply(key);
            if(value != null) {
                this.data.put(key, value);
            }
        }
        return value;
    }

    public Map<K, V> getCachedData() {
        return this.data;
    }

}
