package io.fabric8.crd.example.extraction;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.crd.generator.annotation.SchemaSwap;

public class SchemaSwapSpec2 {
  private WithString1 prop1;
  private WithString2 prop2;
  private WithJsonNode prop3;
  private WithString2 prop4;
  private WithString1 prop5;

  private static class WithString1 {
    private Joker myObject;
  }

  private static class WithString2 {
    private Joker myObject;
  }

  @SchemaSwap(originalType = Joker.class, fieldName = "joker", targetType = JsonNode.class)
  private static class WithJsonNode {
    private Joker myObject;
  }

  public static class Joker {
    private int joker;
  }
}
