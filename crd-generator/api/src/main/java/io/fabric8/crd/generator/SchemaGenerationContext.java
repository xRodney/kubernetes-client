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

import io.sundr.model.AttributeKey;
import io.sundr.model.ClassRef;
import io.sundr.model.Property;
import io.sundr.model.PropertyBuilder;
import io.sundr.model.TypeRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static io.sundr.model.utils.Types.VOID;

public class SchemaGenerationContext {
  public static final AttributeKey<SchemaUnrollModel> ATTRIBUTE_SCHEMA_UNROLL = new AttributeKey<>(SchemaUnrollModel.class);

  private final LinkedList<Level> hierarchy = new LinkedList<>();

  public SchemaGenerationContext(TypeRef rootType) {
    hierarchy.addLast(Level.root(rootType));
  }

  public void registerSwap(ClassRef definitionType, SchemaSwapModel swap) {
    Key key = new Key(swap.originalType, swap.fieldName);
    Value value = new Value(swap, definitionType);
    Value conflict = hierarchy.getLast().addSwap(key, value);
    if (conflict != null) {
      throw new IllegalArgumentException("Conflicting @SchemaSwaps on " + definitionType + ": " + value + " vs. " + conflict);
    }
  }

  public Optional<SchemaSwapModel> lookupAndMark(ClassRef originalType, String name) {
    Key key = new Key(originalType, name);

    Iterator<Level> iterator = hierarchy.descendingIterator();
    while (iterator.hasNext()) {
      Level level = iterator.next();
      Value value = level.getSwaps().get(key);
      if (value != null) {
        value.markUsed();
        return Optional.of(value.getSwap());
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

  public Property pushLevel(Property property) {
    SchemaUnrollModel unroll = property.getAttribute(ATTRIBUTE_SCHEMA_UNROLL);
    final Property result;

    long count = hierarchy.stream().filter(p -> p.hasProperty(property)).count();
    result = count > unroll.getDepth() ? terminatePropertyOrThrow(property, unroll) : property;

    hierarchy.addLast(new Level(result));
    return result;
  }

  private Property terminatePropertyOrThrow(Property property, SchemaUnrollModel unroll) {
    if (!unroll.isDefined()) {
      throw new IllegalArgumentException("Found a cyclic reference: " + renderCurrentPath() + " !! " + new Level(property));
    }
    return new PropertyBuilder(property).withTypeRef(unroll.getTerminator()).build();
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
    private boolean used;
    private final ClassRef definitionType;
    private final SchemaSwapModel swap;

    public Value(SchemaSwapModel swap, ClassRef definitionType) {
      this.swap = swap;
      this.definitionType = definitionType;
      this.used = false;
    }

    public SchemaSwapModel getSwap() {
      return swap;
    }

    private void markUsed() {
      this.used = true;
    }

    public boolean isUsed() {
      return used;
    }

    @Override
    public String toString() {
      return swap + " on " + definitionType;
    }
  }

  public static class SchemaSwapModel {
    private final ClassRef originalType;
    private final String fieldName;
    private final ClassRef targetType;
    private final boolean ignored;
    private final SchemaUnrollModel unroll;

    public SchemaSwapModel(ClassRef originalType, String fieldName, ClassRef targetType, SchemaUnrollModel unroll) {
      this.originalType = originalType;
      this.fieldName = fieldName;
      this.unroll = unroll;

      if (VOID.getName().equals(targetType.getFullyQualifiedName())) {
        this.targetType = null;
        this.ignored = !unroll.isDefined();
      } else {
        this.targetType = targetType;
        this.ignored = false;
      }
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

    public SchemaUnrollModel getUnroll() {
      return unroll;
    }

    public boolean isIgnored() {
      return ignored;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", "@SchemaSwap(", ")")
          .add("originalType=" + originalType)
          .add("fieldName=" + fieldName)
          .add("targetType=" + targetType)
          .add("unroll=" + unroll)
          .toString();
    }
  }

  public static class SchemaUnrollModel {
    private final int depth;
    private final TypeRef terminator;

    public SchemaUnrollModel(int depth, TypeRef terminator) {
      this.depth = depth;
      this.terminator = terminator;
    }

    public int getDepth() {
      return depth;
    }

    public TypeRef getTerminator() {
      return terminator;
    }

    public boolean isDefined() {
      return depth > 0;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", "@SchemaUnroll(", ")")
          .add("depth=" + depth)
          .add("terminator=" + terminator)
          .toString();
    }
  }

  private static class Level {
    private final Property property;
    private Map<Key, Value> swaps;

    public static Level root(TypeRef rootType) {
      Property root = new Property(Collections.emptyList(), rootType, "", Collections.emptyList(), null,
          Collections.emptyMap());
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

    public boolean hasProperty(Property property) {
      return Objects.equals(this.property, property);
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
