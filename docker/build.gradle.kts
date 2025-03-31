
val kafka_version: String by project

dependencies {
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
}

val extractDependencies by tasks.registering(Copy::class) {
    from(sourceSets.main.get().runtimeClasspath)
    into("build/runtime/")
}

val dockerBuild by tasks.registering(Exec::class) {
    inputs.files("Dockerfile")
    commandLine("docker", "build", "-t", "ksd_app:latest", ".")
    //outputs.upToDateWhen { !project.hasProperty("force-docker") }
    doFirst {
        exec {
            isIgnoreExitValue = true
            commandLine("docker", "tag", "ksd_app:latest", "ksd_app:prev")
        }
    }

    doLast {
        exec {
            isIgnoreExitValue = true
            commandLine("docker", "rmi", "ksd_app:prev")
        }
    }
}

tasks.named("build") {
    finalizedBy(extractDependencies, dockerBuild)
}
