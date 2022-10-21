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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.sundr.model.ClassRef;
import io.sundr.model.Property;
import io.sundr.model.TypeRef;

public class SchemaGenerationContext {
  private final LinkedList<Level> hierarchy = new LinkedList<>();

  public SchemaGenerationContext(TypeRef rootType) {
    hierarchy.addLast(Level.root(rootType));
  }

  public void registerSwap(ClassRef definitionType, SchemaSwapModel swap) {
    Key key = new Key(swap.originalType, swap.fieldName);
    Value value = new Value(definitionType, swap.originalType, swap.fieldName, swap.targetType);
    Value conflict = hierarchy.getLast().addSwap(key, value);
    if (conflict != null) {
      throw new IllegalArgumentException("Conflict");
    }
  }

  public Optional<ClassRef> lookupAndMark(ClassRef originalType, String name) {
    Key key = new Key(originalType, name);

    Iterator<Level> iterator = hierarchy.descendingIterator();
    while (iterator.hasNext()) {
      Level level = iterator.next();
      Value value = level.getSwaps().get(key);
      if (value != null) {
        value.markUsed();
        return Optional.of(value.getTargetType());
      }
    }

    return Optional.empty();
  }

  public void close() {
    popLevel();
    if (!hierarchy.isEmpty()) {
      throw new IllegalStateException("Hierarchy should be empty by now");
    }
  }

  public void pushLevel(Property property) {
    Level level = new Level(property);
    long count = hierarchy.stream().filter(p -> p.equals(level)).count();
    if (count > 0) {
      throw new IllegalArgumentException("Found a cyclic reference: " + renderCurrentPath() + " !! " + level);
    }
    hierarchy.addLast(level);
  }

  public void popLevel() {
    Level level = hierarchy.removeLast();
    level.throwIfUnmatchedSwaps();
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

  public static class SchemaSwapModel {
    private final ClassRef originalType;
    private final String fieldName;
    private final ClassRef targetType;

    public SchemaSwapModel(ClassRef originalType, String fieldName, ClassRef targetType) {
      this.originalType = originalType;
      this.fieldName = fieldName;
      this.targetType = targetType;
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

    @Override
    public String toString() {
      return "@SchemaSwap(originalType=" + originalType + ", fieldName=\"" + fieldName + "\", targetType=" + targetType + ")";
    }
  }

  private static class Level {
    private final Property property;
    private Map<Key, Value> swaps;

    public static Level root(TypeRef rootType) {
      Property root = new Property(Collections.emptyList(), rootType, "", Collections.emptyList(), null, Collections.emptyMap());
      return new Level(root);
    }

    public Level(Property property) {
      this.property = property;
    }

    public Map<Key, Value> getSwaps() {
      return swaps == null ? Collections.emptyMap() : swaps;
    }

    public Value addSwap(Key key, Value value) {
      if (swaps == null) {
        swaps = new HashMap<>();
      }
      return swaps.put(key, value);
    }

    public Property getProperty() {
      return property;
    }

    public void throwIfUnmatchedSwaps() {
      if (swaps != null) {
        String unmatchedSchemaSwaps = swaps.values().stream().filter(value -> !value.used)
          .map(Object::toString)
          .collect(Collectors.joining(", "));
        if (!unmatchedSchemaSwaps.isEmpty()) {
          throw new IllegalArgumentException("Unmatched SchemaSwaps: " + unmatchedSchemaSwaps);
        }
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Level level = (Level) o;
      return Objects.equals(property, level.property);
    }

    @Override
    public int hashCode() {
      return Objects.hash(property);
    }

    @Override
    public String toString() {
      String name = property.getName();
      return (name.isEmpty() ? "" : name + ": ") + property.getTypeRef();
    }
  }
}
