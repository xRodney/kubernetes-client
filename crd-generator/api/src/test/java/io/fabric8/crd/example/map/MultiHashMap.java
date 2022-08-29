package io.fabric8.crd.example.map;

import java.util.HashMap;
import java.util.List;

public class MultiHashMap<K, V> extends HashMap<K, List<V>> implements MultiMap<K, V>{
}
