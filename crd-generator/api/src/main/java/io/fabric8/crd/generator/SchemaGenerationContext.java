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
import io.sundr.model.TypeRef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SchemaGenerationContext {
  private final Map<Key, Value> swaps = new HashMap<>();
  private final TypeRef root;
  private final LinkedList<PropertyOnType> path = new LinkedList<>();
  // swapsPerLevel follows the same structure as path, but has one null entry at the beginning
  private final LinkedList<Map<Key, Value>> overridenSwapsPerLevel = new LinkedList<>();

  public SchemaGenerationContext(TypeRef root) {
    this.root = root;
    overridenSwapsPerLevel.addLast(null);
  }

  public void registerSwap(ClassRef definitionType, ClassRef originalType, String fieldName, ClassRef targetType) {
    Value value = new Value(definitionType, originalType, fieldName, targetType);
    Key key = new Key(originalType, fieldName);
    Value previous = swaps.put(key, value);

    if (previous != null) {
      Map<Key, Value> currentSwaps = getCurrentLevelOverridenSwaps();
      Value conflict = currentSwaps.put(key, previous);
      if (conflict != null) {
        throw new IllegalArgumentException("Conflicting SchemaSwaps");
      }
    }
  }

  private Map<Key, Value> getCurrentLevelOverridenSwaps() {
    Map<Key, Value> currentSwaps = overridenSwapsPerLevel.getLast();
    if (currentSwaps == null) {
      currentSwaps = new HashMap<>();
      overridenSwapsPerLevel.removeLast();
      overridenSwapsPerLevel.addLast(currentSwaps);
    }
    return currentSwaps;
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
    String unmatchedSchemaSwaps = swaps.values().stream().filter(value -> !value.used)
        .map(Object::toString)
        .collect(Collectors.joining(", "));
    if (!unmatchedSchemaSwaps.isEmpty()) {
      throw new IllegalArgumentException("Unmatched SchemaSwaps: " + unmatchedSchemaSwaps);
    }
  }

  public void pushLevel(TypeDef type, String property) {
    PropertyOnType propertyOnType = new PropertyOnType(type.getFullyQualifiedName(), property);
    long count = path.stream().filter(p -> p.equals(propertyOnType)).count();
    if (count > 0) {
      throw new IllegalArgumentException("Found a cyclic reference: " + renderCurrentPath() + " ??? " + propertyOnType);
    }
    path.addLast(propertyOnType);
    overridenSwapsPerLevel.push(null);
  }

  public void popLevel() {
    path.removeLast();
    Map<Key, Value> currentSwaps = overridenSwapsPerLevel.removeLast();
    if (currentSwaps != null) {
      swaps.putAll(currentSwaps);
    }
  }

  private String renderCurrentPath() {
    return path.stream()
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

  private static class PropertyOnType {
    private final String type;
    private final String property;

    public PropertyOnType(String type, String property) {
      this.type = type;
      this.property = property;
    }

    public String getType() {
      return type;
    }

    public String getProperty() {
      return property;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PropertyOnType that = (PropertyOnType) o;
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
