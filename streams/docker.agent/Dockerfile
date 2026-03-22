FROM ghcr.io/graalvm/native-image-community:25

RUN mkdir -p /app/metadata

COPY ./build/distributions/streams-1.0.tar ./streams-1.0.tar

RUN tar xfv ./streams-1.0.tar

# Extract native libraries
RUN mkdir -p /app/native-libs && \
    cd /app/native-libs && \
    jar xf /app/streams-1.0/lib/rocksdbjni-10.1.3.jar librocksdbjni-linux-aarch64.so && \
    jar xf /app/streams-1.0/lib/lz4-java-1.10.1.jar net/jpountz/util/linux/aarch64/liblz4-java.so && \
    mv net/jpountz/util/linux/aarch64/liblz4-java.so . && \
    rm -rf net

ENV LD_LIBRARY_PATH=/app/native-libs

WORKDIR /app

ENTRYPOINT ["sh", "-c", "CLASSPATH=$(echo streams-1.0/lib/*.jar | tr ' ' ':') && java -agentlib:native-image-agent=config-output-dir=/app/metadata -cp \"${CLASSPATH}\" io.kineticedge.ksd.streams.Main \"$@\""]
