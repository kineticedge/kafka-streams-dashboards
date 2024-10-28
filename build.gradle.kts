import org.gradle.jvm.tasks.Jar

val logback_version: String by project
val jcommander_version: String by project
val jackson_version: String by project
val apache_commons_version: String by project
val kafka_version: String by project
val slf4j_version: String by project
val lombok_version: String by project

val junit_pioneer_version: String by project
val junit_version: String by project


plugins {
    id("java")
//    id("application")
    id("eclipse")
}

// this allows for subprojects to use java plugin constructs
// without then also causing the parent to have an empty jar file
// generated.
tasks.withType<Jar> {
    onlyIf { !sourceSets["main"].allSource.isEmpty }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://packages.confluent.io/maven/")
    }

}

subprojects.filter { it.name != "metrics-reporter" }.forEach {

    //println("apply common configuration to module ${it.name}")

    it.version = "1.0"

    it.plugins.apply("java")
    it.plugins.apply("application")

    it.java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    it.dependencies {
        implementation("ch.qos.logback:logback-classic:$logback_version")
        implementation("com.beust:jcommander:$jcommander_version")
        implementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
        implementation("org.apache.commons:commons-lang3:$apache_commons_version")
        implementation("org.apache.kafka:kafka-clients:$kafka_version") {
            version {
                strictly(kafka_version)
            }
        }

        implementation("org.slf4j:slf4j-api:$slf4j_version")

        // lombok dependencies
        compileOnly("org.projectlombok:lombok:$lombok_version")
        annotationProcessor("org.projectlombok:lombok:$lombok_version")

        testCompileOnly("org.projectlombok:lombok:$lombok_version")
        testAnnotationProcessor("org.projectlombok:lombok:$lombok_version")
    }

    it.tasks.test {
        useJUnitPlatform()
    }

    // no reason to build both .tar and .zip application distributions, disable zip
    it.tasks.getByName<Zip>("distZip").isEnabled = false

    // make this part of the docker image.
    it.tasks.getByName<Tar>("distTar") {
        exclude("rocksdbjni-*.jar")
        exclude("zstd-jni-*.jar")
        exclude("lz4-java-*.jar")
        exclude("snappy-java-*.jar")
        exclude("jackson-annotations-*.jar")
        exclude("jackson-core-*.jar")
        exclude("jackson-databind-*.jar")
        exclude("kafka-clients-*.jar")
        exclude("kafka-streams-*.jar")
    }

}


subprojects {

    if (file("${project.projectDir}/run.sh").exists()) {

        val createIntegrationClasspath: (String) -> Unit = { scriptName ->
            val cp = extensions.getByName<JavaPluginExtension>("java").sourceSets["main"].runtimeClasspath.files.joinToString("\n") {
                """export CP="${'$'}{CP}:$it""""
            }

            val file = file(scriptName)
            file.writeText("export CP=\"\"\n$cp\n")

            file.setExecutable(true)
        }

        val postBuildScript by tasks.registering {
            doLast {
                createIntegrationClasspath("./.classpath.sh")
            }
        }

        tasks.named("build").configure {
            finalizedBy(postBuildScript)
        }
    }
}
