rootProject.name = "kafka-streams-dashboards"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
    }
}

include("tools")
include("metrics-reporter")
include("common")
include("builder")
include("publisher")
include("restore")
include("streams")
include("analytics")
include("docker")
include("ui:kroxylicious-filters")
