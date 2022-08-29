package io.fabric8.crd.example.map;

import java.util.List;
import java.util.Map;

public interface MultiMap<K, V> extends Map<K, List<V>> {
}
