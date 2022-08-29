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
package io.fabric8.crd.example.map;

import java.util.List;
import java.util.Map;

public class ContainingMapsSpec {

  private Map<String, Map<String, List<Boolean>>> test2 = null;
  public Map<String, Map<String, List<Boolean>>> getTest2() {
    return test2;
  }

  // all these denote the same schema
  private Map<String, List<Integer>> test = null;
  private MultiMap<String, Integer> test3;
  private MultiHashMap<String, Integer> test4;
  private StringKeyMultiMap<Integer> test5;
  private IntegerValueMultiMap<String> test6;
  private StringIntegerMultiMap test7;
  private StringIntegerMultiHashMap test8;
  private WeirdMap<?, List<Integer>, Integer, String> test9;
}
