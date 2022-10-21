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
package io.fabric8.crd.example.extraction;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("samples.javaoperatorsdk.io")
@Version("v1alpha1")
@SchemaSwap(originalType = OverlappingSchemaSwaps.Joker.class, fieldName = "joker", targetType = String.class)
public class OverlappingSchemaSwaps extends CustomResource<OverlappingSchemaSwaps.Spec, Void> implements Namespaced {

  public static class Spec {
    private WithString1 prop1;
    private WithString2 prop2;
    private WithJsonNode prop3;
    private WithString2 prop4;
    private WithString1 prop5;
  }

  private static class WithString1 {
    private Joker myObject;
  }

  private static class WithString2 {
    private Joker myObject;
  }

  @SchemaSwap(originalType = OverlappingSchemaSwaps.Joker.class, fieldName = "joker", targetType = JsonNode.class)
  private static class WithJsonNode {
    private Joker myObject;
  }

  public static class Joker {
    private int joker;
  }
}
