
val kafka_version: String by project
val quartz_version: String by project
val netty_version: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.apache.kafka:kafka-streams:$kafka_version")

    implementation("io.netty:netty-transport:${netty_version}")
    implementation("io.netty:netty-codec-http:${netty_version}")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}
