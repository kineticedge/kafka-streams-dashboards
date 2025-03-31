
val kafka_version: String by project

plugins {
//    application
    distribution
}

// as of 0.11.0, looks like Kroxylicious us built to Java17, to ensure that
// this plugin is at that version -- as you update Kroxylicious.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

    compileOnly("io.kroxylicious:kroxylicious-api:0.11.0")
    compileOnly("org.apache.kafka:kafka-clients:3.4.0")
    implementation("org.slf4j:slf4j-api:2.0.7")

//    implementation(project(":common"))
//    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    //implementation("io.undertow:undertow-servlet:$undertow_version")
}

//application {
//    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
//    // applicationDefaultJvmArgs = listOf("-javaagent:./jmx_prometheus/jmx_prometheus_javaagent.jar=7071:./docker/streams-config.yml")
//}


//
//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//
//plugins {
//    kotlin("jvm") version "1.8.20"
//    id("java-library")
//    id("maven-publish")
//}
//
//group = "com.example"
//version = "1.0.0"
//description = "Kroxylicious Value Masking Plugin"
//
//repositories {
//    mavenCentral()
//    maven {
//        url = uri("https://packages.confluent.io/maven/")
//    }
//}
//
//val kroxyliciousVersion = "0.4.0" // Adjust to match your target version
//
//dependencies {
//    // Kroxylicious API - provided at runtime
//    compileOnly("io.kroxylicious:kroxylicious-api:$kroxyliciousVersion")
//
//    // Kafka clients
//    compileOnly("org.apache.kafka:kafka-clients:3.4.0")
//
//    // Logging
//    implementation("org.slf4j:slf4j-api:2.0.7")
//
//    // Testing dependencies
//    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
//    testImplementation("org.mockito:mockito-core:5.2.0")
//    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
//    testImplementation("io.kroxylicious:kroxylicious-api:$kroxyliciousVersion")
//    testImplementation("org.apache.kafka:kafka-clients:3.4.0")
//}
//
//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//    withJavadocJar()
//    withSourcesJar()
//}
//
//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "17"
//        freeCompilerArgs = listOf("-Xjsr305=strict")
//    }
//}
//
//tasks.withType<Test> {
//    useJUnitPlatform()
//}
//
//// Ensure META-INF/services is correctly included
//sourceSets {
//    main {
//        resources {
//            srcDir("src/main/resources")
//        }
//    }
//}
//
//tasks.jar {
//    manifest {
//        attributes(
//            mapOf(
//                "Implementation-Title" to project.name,
//                "Implementation-Version" to project.version,
//                "Built-By" to System.getProperty("user.name"),
//                "Built-Date" to java.time.Instant.now(),
//                "Automatic-Module-Name" to "com.example.kroxylicious.masking"
//            )
//        )
//    }
//}
//
//// Distribution configuration
//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//
//            pom {
//                name.set(project.name)
//                description.set(project.description)
//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//                developers {
//                    developer {
//                        id.set("developer")
//                        name.set("Developer Name")
//                        email.set("developer@example.com")
//                    }
//                }
//            }
//        }
//    }
//}
//
//// Create the configuration directory for the plugin
//tasks.register<Copy>("prepareKroxyliciousPlugin") {
//    from(tasks.jar)
//    into(layout.buildDirectory.dir("kroxylicious-plugin"))
//
//    doLast {
//        println("Plugin jar is available at: ${layout.buildDirectory.dir("kroxylicious-plugin").get()}")
//    }
//}
//
//// Create distribution zip containing the plugin and example configuration
//tasks.register<Zip>("createDistribution") {
//    from(tasks.jar)
//    from(file("src/main/resources/kroxylicious-config.yaml"))
//    from(file("README.md"))
//
//    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
//    archiveFileName.set("${project.name}-${project.version}-distribution.zip")
//}
//
//// Hook into the build process
//tasks.build {
//    finalizedBy("prepareKroxyliciousPlugin", "createDistribution")
//}
