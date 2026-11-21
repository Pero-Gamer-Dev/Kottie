dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-server-spi"))
    api(project(":kottie-packets"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
