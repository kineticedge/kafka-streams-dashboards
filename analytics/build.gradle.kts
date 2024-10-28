
val kafka_version: String by project
val quartz_version: String by project
val undertow_version: String by project
val jetty_version: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    implementation("org.quartz-scheduler:quartz:$quartz_version")
    implementation("io.undertow:undertow-servlet:$undertow_version")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}
