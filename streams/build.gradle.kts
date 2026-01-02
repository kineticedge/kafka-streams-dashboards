
val kafka_version: String by project
val netty_version: String by project

plugins {
    application
    distribution
}

dependencies {
    implementation(project(":common"))
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    //implementation("io.undertow:undertow-servlet:$undertow_version")

    implementation("io.netty:netty-transport:${netty_version}")
    implementation("io.netty:netty-codec-http:${netty_version}")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
    // applicationDefaultJvmArgs = listOf("-javaagent:./jmx_prometheus/jmx_prometheus_javaagent.jar=7071:./docker/streams-config.yml")
}

