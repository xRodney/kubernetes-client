package io.fabric8.crd.example.unroll;

import io.fabric8.crd.generator.annotation.SchemaUnroll;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.List;
import java.util.Map;

@Group("sample.fabric8.io")
@Version("v1alpha1")
public class Unroll extends CustomResource<Unroll.Spec, Void> implements Namespaced {

  public static class Spec {
    private int number;
    private Ref1 ref1;
    private List<Ref2> ref2;
    private Map<String, Ref3> ref3;
  }

  private static class Ref1 {
    private int number;
    @SchemaUnroll(depth = 2)
    private Ref1 ref1;
  }

  private static class Ref2 {
    private int number;
    @SchemaUnroll(depth = 2)
    private List<Ref2> ref2;
  }

  private static class Ref3 {
    private int number;
    @SchemaUnroll(depth = 2)
    private Map<String, Ref3> ref3;
  }
}
