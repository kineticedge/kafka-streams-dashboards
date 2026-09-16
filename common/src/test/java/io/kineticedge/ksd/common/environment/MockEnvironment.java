/*
 * Copyright 2026 Kinetic Edge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kineticedge.ksd.common.environment;

import java.util.Map;

/**
 * Test double for {@link Environment} that serves environment variables from a
 * caller-supplied map instead of the process environment.
 */
public class MockEnvironment extends Environment {

  private final Map<String, String> variables;

  /**
   * @param variables the variables this environment exposes; must not be null
   * @throws IllegalArgumentException if {@code variables} is null
   */
  public MockEnvironment(final Map<String, String> variables) {
    if (variables == null) {
      throw new IllegalArgumentException("variables cannot be null.");
    }
    this.variables = variables;
  }

  @Override
  protected Map<String, String> getEnv() {
    return variables;
  }

  @Override
  protected String getEnv(final String name) {
    return variables.get(name);
  }
}
