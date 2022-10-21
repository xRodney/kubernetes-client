/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.crd.generator;

import io.sundr.model.ClassRef;
import io.sundr.model.TypeDef;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SchemaGenerationContext {
  private final Map<Key, Value> swaps = new HashMap<>();
  private final LinkedList<HierarchyLevel> hierarchy = new LinkedList<>();

  public void registerSwap(ClassRef definitionType, ClassRef originalType, String fieldName, ClassRef targetType) {
    Value value = new Value(definitionType, originalType, fieldName, targetType);
    Key key = new Key(originalType, fieldName);
    Value previous = swaps.put(key, value);

    // Non-null previous value indicates that there are overlapping schema swaps.
    // This may mean that we are replacing a general swap with a more specific one.
    // In that case we store the previous value on the hierarchy list, so that we can restore it later.
    // If there is already a record in the hierarchy, the two swaps are defined on the same level, which is a conflict.
    // A special case is the top level, where hierarchy is empty. In that case, it can only be a conflict.
    if (previous != null) {
      Value conflict = hierarchy.isEmpty() ? previous : hierarchy.getLast().storeSwap(key, previous);
      if (conflict != null) {
        throw new IllegalArgumentException("Conflicting SchemaSwaps");
      }
    }
  }

  public Optional<ClassRef> lookupAndMark(ClassRef originalType, String name) {
    Value value = swaps.get(new Key(originalType, name));
    if (value != null) {
      value.markUsed();
      return Optional.of(value.getTargetType());
    } else {
      return Optional.empty();
    }
  }

  public void throwIfUnmatchedSwaps() {
    throwIfUnmatchedSwaps(swaps);
  }

  private void throwIfUnmatchedSwaps(Map<Key, Value> swaps1) {
    String unmatchedSchemaSwaps = swaps1.values().stream().filter(value -> !value.used)
      .map(Object::toString)
      .collect(Collectors.joining(", "));
    if (!unmatchedSchemaSwaps.isEmpty()) {
      throw new IllegalArgumentException("Unmatched SchemaSwaps: " + unmatchedSchemaSwaps);
    }
  }

  public void pushLevel(TypeDef type, String property) {
    HierarchyLevel level = new HierarchyLevel(type.getFullyQualifiedName(), property);
    long count = hierarchy.stream().filter(p -> p.equals(level)).count();
    if (count > 0) {
      throw new IllegalArgumentException("Found a cyclic reference: " + renderCurrentPath() + " ??? " + level);
    }
    hierarchy.addLast(level);
  }

  public void popLevel() {
    HierarchyLevel level = hierarchy.removeLast();
    Map<Key, Value> popped = level.restoreSwapsTo(swaps);
    throwIfUnmatchedSwaps(popped);
  }

  private String renderCurrentPath() {
    return hierarchy.stream()
        .map(Object::toString)
        .collect(Collectors.joining(" -> "));
  }

  private static final class Key {
    private final ClassRef originalType;
    private final String fieldName;

    public Key(ClassRef originalType, String fieldName) {
      this.originalType = originalType;
      this.fieldName = fieldName;
    }

    public ClassRef getOriginalType() {
      return originalType;
    }

    public String getFieldName() {
      return fieldName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Key key = (Key) o;
      return Objects.equals(originalType, key.originalType) && Objects.equals(fieldName, key.fieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(originalType, fieldName);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Key.class.getSimpleName() + "[", "]")
          .add("originalType=" + originalType)
          .add("fieldName='" + fieldName + "'")
          .toString();
    }
  }

  private static class Value {
    private final ClassRef originalType;
    private final String fieldName;
    private final ClassRef targetType;
    private boolean used;
    private final ClassRef definitionType;

    public Value(ClassRef definitionType, ClassRef originalType, String fieldName, ClassRef targetType) {
      this.definitionType = definitionType;
      this.originalType = originalType;
      this.fieldName = fieldName;
      this.targetType = targetType;
      this.used = false;
    }

    private void markUsed() {
      this.used = true;
    }

    public ClassRef getOriginalType() {
      return originalType;
    }

    public String getFieldName() {
      return fieldName;
    }

    public ClassRef getTargetType() {
      return targetType;
    }

    public boolean isUsed() {
      return used;
    }

    @Override
    public String toString() {
      return "@SchemaSwap(originalType=" + originalType + ", fieldName=\"" + fieldName + "\", targetType=" + targetType
          + ") on " + definitionType;
    }
  }

  private static class HierarchyLevel {
    private final String type;
    private final String property;
    private Map<Key, Value> storedSwaps;

    public HierarchyLevel(String type, String property) {
      this.type = type;
      this.property = property;
    }

    public String getType() {
      return type;
    }

    public String getProperty() {
      return property;
    }

    public Value storeSwap(Key key, Value swap) {
      if (storedSwaps == null) {
        storedSwaps = new HashMap<>();
      }
      return storedSwaps.put(key, swap);
    }

    public Map<Key, Value> restoreSwapsTo(Map<Key, Value> swaps) {
      if (storedSwaps == null) {
        return Collections.emptyMap();
      }

      Map<Key, Value> oldValues = new HashMap<>(storedSwaps.size());
      storedSwaps.forEach((key, value) -> oldValues.put(key, swaps.put(key, value)));
      return oldValues;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HierarchyLevel that = (HierarchyLevel) o;
      return Objects.equals(type, that.type) && Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, property);
    }

    @Override
    public String toString() {
      return type + "#" + property;
    }
  }
}
