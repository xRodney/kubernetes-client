package io.fabric8.crd.example.unroll;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.crd.generator.annotation.SchemaUnroll;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.List;
import java.util.Map;

@Group("sample.fabric8.io")
@Version("v1alpha1")
@SchemaSwap(originalType = UnrollTerminator.Ref1.class, fieldName = "ref1", unroll = @SchemaUnroll(depth = 2, terminator = JsonNode.class))
@SchemaSwap(originalType = UnrollTerminator.Ref2.class, fieldName = "ref2", unroll = @SchemaUnroll(depth = 2, terminator = JsonNode.class))
@SchemaSwap(originalType = UnrollTerminator.Ref3.class, fieldName = "ref3", unroll = @SchemaUnroll(depth = 2, terminator = JsonNode.class))
public class UnrollTerminator extends CustomResource<UnrollTerminator.Spec, Void> implements Namespaced {

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
