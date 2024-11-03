//****************************** UNCLASSIFIED *********************************
/*
 * Copyright (C) 2024 by Raft.
 * All rights reserved.
 *
 * LICENSE: This file may contain source code subject to one or more
 * DoD/US Government Contracts with Government Use rights.
 * Distribution is restricted. Contact Raft for details.
 *
 * EXPORT CONTROL STATEMENT: This file may contain CUI and/or ITAR controlled
 * material.
 *
 * Created 8/22/24 9:00AM by neil.buesing
 *
 */
package io.kineticedge.ksd.common.environment;


import java.util.Map;

public class MockEnvironment extends io.kineticedge.ksd.common.environment.Environment {

  private final Map<String, String> variables;

  public MockEnvironment(Map<String, String> variables) {

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
  protected String getEnv(String name) {
    return variables.get(name);
  }
}
