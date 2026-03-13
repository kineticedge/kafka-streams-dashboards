

dependencies {
    implementation(project(":common"))
    runtimeOnly("org.bitbucket.b_c:jose4j:0.9.6")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}