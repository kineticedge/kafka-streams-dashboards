
val httpclient5_version: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.apache.httpcomponents.client5:httpclient5:${httpclient5_version}")
}

