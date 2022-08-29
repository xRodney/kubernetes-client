package io.fabric8.crd.example.map;

import java.util.List;
import java.util.Map;

/**
 * Overly complicated way to specify multimap
 */
public interface WeirdMap<Unused, Container extends List<Value>, Value, Key> extends Map<Key, Container> {
}
