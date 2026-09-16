
package io.kineticedge.ksd.kroxylicious.filters;

import java.nio.ByteBuffer;

/**
 * A transformation of the key or value of a produce record.
 */
@FunctionalInterface
public interface ByteBufferTransformation {
    ByteBuffer transform(String topicName, ByteBuffer original);
}