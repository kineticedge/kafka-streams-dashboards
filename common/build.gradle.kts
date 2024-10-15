val kafka_version: String by project
val junit_pioneer_version: String by project
val junit_version: String by project

dependencies {
    implementation("org.apache.kafka:kafka-streams:$kafka_version")

    implementation("org.apache.datasketches:datasketches-java:2.0.0")


    testImplementation("org.junit-pioneer:junit-pioneer:$junit_pioneer_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
}

tasks.named<Test>("test") {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
}