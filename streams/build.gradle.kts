
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

    implementation(project(":common"))

    runtimeOnly("org.bitbucket.b_c:jose4j:0.9.6")

}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
    // applicationDefaultJvmArgs = listOf("-javaagent:./jmx_prometheus/jmx_prometheus_javaagent.jar=7071:./docker/streams-config.yml")
}




//



// Only define native tasks if explicitly requested
//if (gradle.startParameter.taskNames.any { it.contains("native") }) {

val graalvmHome = providers.environmentVariable("GRAALVM_HOME")
    .orElse(providers.environmentVariable("GRAALVM25_HOME"))

fun requireGraalTool(name: String): String {
    val home = graalvmHome.orNull ?: error("Set GRAALVM_HOME (or GRAALVM25_HOME) to a GraalVM install")
    val f = file("$home/bin/$name")
    require(f.exists()) { "Missing $name at: ${f.path}. Install a GraalVM distribution that includes native-image tools." }
    return f.absolutePath
}

val metadataDir = layout.buildDirectory.dir("native-image-agent/generated/META-INF/native-image/io.kineticedge.ksd/stream")
val agentOutDir = layout.buildDirectory.dir("native-image-agent/run")
val mergedMetadataDir = layout.buildDirectory.dir("native-image-agent/merged")

//val runStreamsWithAgent = tasks.register<JavaExec>("runStreamsWithAgent") {
//    group = "native"
//    description = "Runs the streams app under native-image-agent to collect config"
//
//    dependsOn(tasks.named("classes"))
//
//    setExecutable(requireGraalTool("java"))
//    classpath = sourceSets.main.get().runtimeClasspath
//    mainClass.set("io.kineticedge.ksd.streams.Main")
//
//    jvmArgs("-agentlib:native-image-agent=config-output-dir=${agentOutDir.get().asFile.absolutePath}")
//
//    outputs.dir(agentOutDir)
//}
//
//val generateStreamsNativeConfig = tasks.register<Exec>("generateStreamsNativeConfig") {
//    group = "native"
//    description = "Generates streams native-image config"
//
//    dependsOn(runStreamsWithAgent)
//
//    commandLine(
//        requireGraalTool("native-image-configure"),
//        "generate",
//        "--input-dir=${agentOutDir.get().asFile.absolutePath}",
//        "--output-dir=${metadataDir.get().asFile.absolutePath}",
//    )
0//}

val mergeWithBaseline = tasks.register<Exec>("mergeWithBaseline") {
    group = "native"
    description = "Merges streams config with native-baseline config"

//    dependsOn(generateStreamsNativeConfig)
//    dependsOn(":native-baseline:mergeScenarioNativeConfig")

    val baselineDir = project(":native-baseline").layout.buildDirectory.dir("native-image-agent/merged")

    println("****")
    println("****")
    println("****")
    println(layout.buildDirectory.dir("native-image-agent/run").get().asFile.absolutePath)
    println(baselineDir.get().asFile.absolutePath)
    println(mergedMetadataDir.get().asFile.absolutePath)
    println("****")
    println("****")
    println("****")

    commandLine(
        requireGraalTool("native-image-configure"),
        "generate",
        "--input-dir=${layout.buildDirectory.dir("native-image-agent/run").get().asFile.absolutePath}",
        "--input-dir=${baselineDir.get().asFile.absolutePath}",
        "--output-dir=${mergedMetadataDir.get().asFile.absolutePath}",
    )

    outputs.dir(mergedMetadataDir)
}

val copyMergedMetadata = tasks.register<Copy>("copyMergedMetadata") {
    group = "native"
    description = "Copies merged native-image metadata to source directory"

    dependsOn(mergeWithBaseline)

    from(mergedMetadataDir.get().asFile)
    into(file("src/main/resources/META-INF/native-image/"))

    onlyIf { mergedMetadataDir.get().asFile.exists() }
}
//}