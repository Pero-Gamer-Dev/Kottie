dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-coroutines"))
    api(project(":kottie-server-spi"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
