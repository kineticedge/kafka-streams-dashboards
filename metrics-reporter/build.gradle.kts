plugins {
    id("java")
    //id("application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "io.kineticedge.ksd.metrics"
version = "1.0"

repositories {
    mavenCentral()
}

val kafka_version: String by project
val slf4j_version: String by project
val grpc_version: String by project
val opentelemetry_proto_version: String by project

dependencies {
    implementation("org.slf4j:slf4j-simple:${slf4j_version}")
    implementation("io.opentelemetry.proto:opentelemetry-proto:${opentelemetry_proto_version}")
    implementation("io.grpc:grpc-api:${grpc_version}")
    implementation("io.grpc:grpc-netty-shaded:${grpc_version}")
    implementation("io.grpc:grpc-protobuf:${grpc_version}")
    implementation("io.grpc:grpc-stub:${grpc_version}")
    implementation("org.apache.kafka:kafka-clients") {
        version {
            strictly(kafka_version)
        }
    }
    //implementation("io.micrometer:micrometer-core:1.13.5")
}


tasks.register<Jar>("uberJar") {
    // EXLUDE -- https://github.com/grpc/grpc-java/issues/10853
    // if Exclude, first wins which is the wrong one; using INCLUDE...
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveClassifier.set("all")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith("jar")
        }.map { zipTree(it) }
    })

    manifest {
    }
}

tasks.build {
    dependsOn(tasks.named("uberJar"))
}