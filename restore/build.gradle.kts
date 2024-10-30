
dependencies {
    implementation(project(":common"))
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}