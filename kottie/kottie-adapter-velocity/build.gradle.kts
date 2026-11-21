dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-server-spi"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    // Velocity API is compileOnly on the host plugin — not required here.
}
