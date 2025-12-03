
val apache_commons_csv_version: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.apache.commons:commons-csv:${apache_commons_csv_version}")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}