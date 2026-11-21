dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-server-spi"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    // Paper API is intentionally not a hard dependency of the skeleton.
    // Wire compileOnly("io.papermc.paper:paper-api:...") when targeting a version.
}
