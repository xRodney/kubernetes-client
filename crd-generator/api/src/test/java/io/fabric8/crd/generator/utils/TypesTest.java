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
package io.fabric8.crd.generator.utils;

import static io.fabric8.crd.generator.utils.Types.REFLECTION_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.crd.example.basic.Basic;
import io.fabric8.crd.example.basic.BasicSpec;
import io.fabric8.crd.example.basic.BasicStatus;
import io.fabric8.crd.example.inherited.Child;
import io.fabric8.crd.example.joke.Joke;
import io.fabric8.crd.example.map.IntegerValueMultiMap;
import io.fabric8.crd.example.map.MultiHashMap;
import io.fabric8.crd.example.map.MultiMap;
import io.fabric8.crd.example.map.StringKeyMultiMap;
import io.fabric8.crd.example.map.StringIntegerMultiHashMap;
import io.fabric8.crd.example.map.StringIntegerMultiMap;
import io.fabric8.crd.example.map.WeirdMap;
import io.fabric8.crd.example.person.Person;
import io.fabric8.crd.example.webserver.WebServerWithStatusProperty;
import io.fabric8.crd.generator.utils.Types.SpecAndStatus;
import io.sundr.adapter.api.Adapters;
import io.sundr.model.ClassRef;
import io.sundr.model.Property;
import io.sundr.model.TypeDef;
import io.sundr.model.TypeDefBuilder;
import io.sundr.model.TypeParamRefBuilder;
import io.sundr.model.TypeRef;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class TypesTest {

  @Test
  void shouldFindStatusProperty() {
    TypeDef def = Types.typeDefFrom(WebServerWithStatusProperty.class);
    Optional<Property> p = Types.findStatusProperty(def);
    assertTrue(p.isPresent());

    def = Types.typeDefFrom(Basic.class);
    p = Types.findStatusProperty(def);
    assertTrue(p.isPresent());
  }

  @Test
  void shouldFindInheritedStatusProperty() {
    final TypeDef def = Types.typeDefFrom(Child.class);
    final Optional<Property> p = Types.findStatusProperty(def);
    assertTrue(p.isPresent());
    final Property property = p.get();
    final TypeRef typeRef = property.getTypeRef();
    assertTrue(typeRef instanceof ClassRef);
    final ClassRef classRef = (ClassRef) typeRef;
    final SpecAndStatus specAndStatus = Types.resolveSpecAndStatusTypes(def);
    assertEquals(specAndStatus.getStatusClassName(), classRef.getFullyQualifiedName());
  }

  @Test
  void shouldHaveAllTheExpectedProperties() {
    final TypeDef def = Types.typeDefFrom(Joke.class);
    final List<Property> properties = def.getProperties();
    assertEquals(7, properties.size());
  }
  
  @Test
  void findingSuperClassesShouldWork() {
    List<ClassRef> superClasses = Types.typeDefFrom(Basic.class).getExtendsList();
    assertTrue(superClasses.stream().anyMatch(c -> c.getName().contains("CustomResource")));
  }

  @Test
  void projectSuperClassesShouldProduceProperlyTypedClasses() {
    List<ClassRef> superClasses = Types.typeDefFrom(Basic.class).getExtendsList();
    assertEquals(2, superClasses.size());
    Optional<ClassRef> crOpt = superClasses.stream()
      .filter(c -> c.getName().contains("CustomResource")).findFirst();
    assertTrue(crOpt.isPresent());
    ClassRef crDef = crOpt.get();
    List<TypeRef> arguments = crDef.getArguments();
    assertEquals(2, arguments.size());
    assertTrue(arguments.get(0).toString().contains(BasicSpec.class.getSimpleName()));
    assertTrue(arguments.get(1).toString().contains(BasicStatus.class.getSimpleName()));
  }

  @Test
  void isNamespacedShouldWork() {
    TypeDef def = Types.typeDefFrom(Basic.class);
    assertTrue(Types.isNamespaced(def));
    def = Types.typeDefFrom(Person.class);
    assertFalse(Types.isNamespaced(def));
  }

  @Test
  void shouldNotFailWhenInheritingHashMap() {
    TypeDef typeDef = Types.typeDefFrom(MultiHashMap.class);

    new TypeDefBuilder(typeDef)
      .accept(TypeParamRefBuilder.class,
        c -> assertTrue(c.getAttributes().isEmpty(), "type param ref " + c + " should not have attributes, has " + c.getAttributes()));
  }

  @Test
  void shouldCorrectlyUnshallowExtendsAndImplementsLists() {
    Types.typeDefFrom(MultiHashMap.class);  // just to initialize the reflection context
    TypeDef typeDef = Types.typeDefFrom(toRef(MultiHashMap.class, toRef(Integer.class), toRef(String.class)));

    List<ClassRef> extendsList = typeDef.getExtendsList();
    assertEquals(3, extendsList.size());
    assertClassExtends(typeDef, toRef(Object.class));
    assertClassExtends(typeDef, toRef(AbstractMap.class, toRef(Integer.class), toRef(List.class, toRef(String.class))));
    assertClassExtends(typeDef, toRef(HashMap.class, toRef(Integer.class), toRef(List.class, toRef(String.class))));

    List<ClassRef> implementsList = typeDef.getImplementsList();
    assertEquals(4, implementsList.size());
    assertClassImplements(typeDef, toRef(Cloneable.class));
    assertClassImplements(typeDef, toRef(Serializable.class));
    assertClassImplements(typeDef, toRef(Map.class, toRef(Integer.class), toRef(List.class, toRef(String.class))));
    assertClassImplements(typeDef, toRef(MultiMap.class, toRef(Integer.class), toRef(String.class)));
  }

  @Test
  void shouldCorrectlyUnshallowMultiMaps() {
    ClassRef[] refs = new ClassRef[]{
      toRef(MultiHashMap.class, toRef(String.class), toRef(Integer.class)),
      toRef(MultiMap.class, toRef(String.class), toRef(Integer.class)),
      toRef(StringKeyMultiMap.class, toRef(Integer.class)),
      toRef(IntegerValueMultiMap.class, toRef(String.class)),
      toRef(StringIntegerMultiMap.class),
      toRef(StringIntegerMultiHashMap.class),
      toRef(WeirdMap.class, toRef(Object.class), toRef(List.class, toRef(Integer.class)), toRef(Integer.class), toRef(String.class))
    };

    ClassRef stringIntMultiMap = toRef(Map.class, toRef(String.class), toRef(List.class, toRef(Integer.class)));

    assertAll(Stream.of(refs).map(Types::typeDefFrom).map(def -> () -> assertClassImplements(def, stringIntMultiMap)));
  }

  private static void assertClassImplements(TypeDef clazz, ClassRef expected) {
    assertTrue(clazz.getImplementsList().contains(expected), clazz + " should implement " + expected);
  }

  private static void assertClassExtends(TypeDef clazz, ClassRef expected) {
    assertTrue(clazz.getExtendsList().contains(expected), clazz + " should extend " + expected);
  }

  private static ClassRef toRef(Class<?> rawClass, ClassRef... arguments) {
    Adapters.adaptType(rawClass, REFLECTION_CONTEXT);  // this stores the TypeDef to internal repository
    List<TypeRef> arguments1 = Stream.of(arguments).collect(Collectors.toList());
    return new ClassRef(rawClass.getName(), 0, arguments1, Collections.emptyMap());
  }
}
