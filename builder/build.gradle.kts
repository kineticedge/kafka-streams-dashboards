val apache_commons_csv_version: String by project

//plugins {
//    id("application")
//}
//plugins.apply("application")

dependencies {
    implementation(project(":common"))
    implementation("org.apache.commons:commons-csv:$apache_commons_csv_version")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
}