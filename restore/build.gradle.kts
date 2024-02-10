val undertow_version: String by project

dependencies {
    implementation(project(":common"))
    implementation("io.undertow:undertow-servlet:$undertow_version")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}