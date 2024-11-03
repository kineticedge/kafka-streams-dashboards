
package io.kineticedge.ksd.common.environment;

import java.util.Map;

/**
 * A wrapper class to Environment to make tests easier.
 */
public class Environment {

    // all public methods should use these 2 methods, then MockEnvironment replaces these with alternate for testing.

    protected Map<String, String> getEnv() {
        return System.getenv();
    }

    protected String getEnv(String name) {
        return System.getenv(name);
    }

    // public methods

    public String getAll(String name) {
        return getEnv(name);
    }

    public Map<String, String> getAll() {
        return getEnv();
    }
}
