package io.fabric8.crd.example.unroll;

import java.util.List;
import java.util.Map;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.crd.generator.annotation.SchemaUnroll;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("sample.fabric8.io")
@Version("v1alpha1")
@SchemaSwap(originalType = UnrollOnSchemaSwap.Ref1.class, fieldName = "ref1", unroll = @SchemaUnroll(depth = 2))
@SchemaSwap(originalType = UnrollOnSchemaSwap.Ref2.class, fieldName = "ref2", unroll = @SchemaUnroll(depth = 2))
@SchemaSwap(originalType = UnrollOnSchemaSwap.Ref3.class, fieldName = "ref3", unroll = @SchemaUnroll(depth = 2))
public class UnrollOnSchemaSwap extends CustomResource<UnrollOnSchemaSwap.Spec, Void> implements Namespaced {

  public static class Spec {
    private int number;
    private Ref1 ref1;
    private List<Ref2> ref2;
    private Map<String, Ref3> ref3;
  }

  public static class Ref1 {
    private int number;
    private Ref1 ref1;
  }

  public static class Ref2 {
    private int number;
    private List<Ref2> ref2;
  }

  public static class Ref3 {
    private int number;
    private Map<String, Ref3> ref3;
  }
}